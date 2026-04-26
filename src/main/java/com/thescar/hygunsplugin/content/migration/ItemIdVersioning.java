package com.thescar.hygunsplugin.content.migration;

import com.thescar.hygunsplugin.support.hytale.PlayerInventoryAccess;
import com.thescar.hygunsplugin.support.hytale.ItemStackUtils;
import com.thescar.hygunsplugin.support.json.JsonValueUtils;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

import com.google.gson.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Chain-based item id versioning.
 * <p>
 * Example: AK47 -> Weapon_AK47 -> Weapon_Rifle_AK47
 * <p>
 * Any old id in the chain resolves to the latest known id.
 */
public final class ItemIdVersioning {
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	private static final Map<String, String> NEXT_ID_BY_ID = new HashMap<>();
	private static final Pattern FILENAME_VERSION_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+){1,3}(?:[-_A-Za-z0-9]+)?)");
	private static final String CONFIG_FILE_A = "Server/Config/ItemIdMigrations.json";
	private static final String CONFIG_FILE_B = "resources/Server/Config/ItemIdMigrations.json";
	private static final String CONFIG_DIR_A = "Server/Config/ItemIdMigrations/";
	private static final String CONFIG_DIR_B = "resources/Server/Config/ItemIdMigrations/";
	private static volatile String CURRENT_PACK_VERSION = "0";
	@Nullable
	private static volatile String REQUIRED_PACK_VERSION_WARNING;

	private ItemIdVersioning() {
	}

	public static void loadFromJar(@Nullable Path pluginJarPath, @Nullable String packVersion) {
		Map<String, String> loaded = new HashMap<>();
		int filesLoaded = 0;
		CURRENT_PACK_VERSION = sanitizeVersion(packVersion);
		REQUIRED_PACK_VERSION_WARNING = null;
		try {
			if (pluginJarPath != null && Files.isDirectory(pluginJarPath)) {
				filesLoaded += loadFromDirectory(pluginJarPath, loaded);
			} else if (pluginJarPath != null && Files.isRegularFile(pluginJarPath)) {
				filesLoaded += loadFromZip(pluginJarPath, loaded);
			} else {
				LOGGER
					.atWarning()
					.log("ItemIdVersioning load failed: plugin path is not a file or directory: %s", pluginJarPath);
			}

		} catch (Exception e) {
			LOGGER.atWarning().log("ItemIdVersioning load failed: %s", e);
		}

		synchronized (NEXT_ID_BY_ID) {
			NEXT_ID_BY_ID.clear();
			NEXT_ID_BY_ID.putAll(loaded);
		}

		LOGGER.atInfo().log(
			"ItemIdVersioning loaded %d migration link(s) from %d file(s) for pack version %s", NEXT_ID_BY_ID.size(),
			filesLoaded, CURRENT_PACK_VERSION
		);
		if (REQUIRED_PACK_VERSION_WARNING != null) {
			LOGGER.atWarning().log(
				"HyGuns pack version %s is outdated for some migrations. Required version: %s or higher.",
				CURRENT_PACK_VERSION, REQUIRED_PACK_VERSION_WARNING
			);
		}
	}

	private static int loadFromDirectory(@Nonnull Path root, @Nonnull Map<String, String> out) throws Exception {
		List<Path> candidates = new ArrayList<>();
		addIfExists(candidates, root.resolve(CONFIG_FILE_A.replace('/', java.io.File.separatorChar)));
		addIfExists(candidates, root.resolve(CONFIG_FILE_B.replace('/', java.io.File.separatorChar)));
		collectJsonFiles(candidates, root.resolve(CONFIG_DIR_A.replace('/', java.io.File.separatorChar)));
		collectJsonFiles(candidates, root.resolve(CONFIG_DIR_B.replace('/', java.io.File.separatorChar)));
		candidates = candidates.stream().filter(Objects::nonNull).distinct().sorted().toList();
		int loadedFiles = 0;
		for (Path path : candidates) {
			String requiredVersion = extractVersionFromFilename(path.getFileName().toString());
			if (isMigrationFileBlocked(requiredVersion)) {
				continue;
			}

			String json = Files.readString(path, StandardCharsets.UTF_8);
			if (applyMigrationJson(path.toString(), json, out) > 0) {
				loadedFiles++;
			}
		}

		return loadedFiles;
	}

	private static int loadFromZip(@Nonnull Path zipPath, @Nonnull Map<String, String> out) throws Exception {
		List<String> names = new ArrayList<>();
		try (ZipFile zip = new ZipFile(zipPath.toFile())) {
			var entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry.isDirectory()) {
					continue;
				}
				String name = entry.getName();
				if (matchesMigrationPath(name)) {
					names.add(name);
				}
			}

			Collections.sort(names);
			int loadedFiles = 0;
			for (String name : names) {
				String fileName = Path.of(name).getFileName().toString();
				String requiredVersion = extractVersionFromFilename(fileName);
				if (isMigrationFileBlocked(requiredVersion)) {
					continue;
				}

				ZipEntry entry = zip.getEntry(name);
				if (entry == null) {
					continue;
				}
				try (InputStream in = zip.getInputStream(entry)) {
					String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
					if (applyMigrationJson(name, json, out) > 0) {
						loadedFiles++;
					}
				}
			}

			return loadedFiles;
		}
	}

	private static boolean isMigrationFileBlocked(@Nullable String requiredVersion) {
		if (requiredVersion == null || requiredVersion.isBlank()) {
			return false;
		}
		if (compareVersions(CURRENT_PACK_VERSION, requiredVersion) >= 0) {
			return false;
		}
		String prev = REQUIRED_PACK_VERSION_WARNING;
		if (prev == null || compareVersions(requiredVersion, prev) > 0) {
			REQUIRED_PACK_VERSION_WARNING = requiredVersion;
		}

		return true;
	}

	private static boolean matchesMigrationPath(@Nullable String name) {
		if (name == null || !name.endsWith(".json")) {
			return false;
		}
		return CONFIG_FILE_A.equals(name) || CONFIG_FILE_B.equals(name) || name.startsWith(CONFIG_DIR_A) || name.startsWith(CONFIG_DIR_B);
	}

	private static void addIfExists(@Nonnull List<Path> list, @Nonnull Path file) {
		if (Files.isRegularFile(file)) {
			list.add(file);
		}
	}

	private static void collectJsonFiles(@Nonnull List<Path> list, @Nonnull Path dir) throws Exception {
		if (!Files.isDirectory(dir)) {
			return;
		}
		try (var stream = Files.list(dir)) {
			stream
				.filter(Files::isRegularFile)
				.filter(p -> p.getFileName().toString().endsWith(".json"))
				.forEach(list::add);
		}
	}

	private static int applyMigrationJson(@Nonnull String sourceName, @Nonnull String json, @Nonnull Map<String, String> out) {
		final JsonElement root;
		try {
			root = JsonParser.parseString(json);
		} catch (JsonParseException e) {
			LOGGER.atWarning().log("ItemIdVersioning: invalid json in %s: %s", sourceName, e.getMessage());
			return 0;
		}

		if (!root.isJsonObject()) {
			LOGGER.atWarning().log("ItemIdVersioning: root must be object in %s", sourceName);
			return 0;
		}

		JsonObject obj = root.getAsJsonObject();
		int links = 0;
		// Most compact format:
		// "Chains": [ ["AK47","Weapon_AK47","Weapon_Rifle_AK47"], ... ]
		JsonElement chainsEl = obj.get("Chains");
		if (chainsEl != null && chainsEl.isJsonArray()) {
			JsonArray chains = chainsEl.getAsJsonArray();
			for (JsonElement chainEl : chains) {
				if (!chainEl.isJsonArray()) {
					continue;
				}
				JsonArray chain = chainEl.getAsJsonArray();
				String prev = null;
				for (JsonElement idEl : chain) {
					String id = JsonValueUtils.Read.nonBlankString(idEl);
					if (id == null) {
						continue;
					}
					if (prev != null) {
						registerLink(prev, id, out, sourceName);
						links++;
					}

					prev = id;
				}
			}
		}

		// Quick map format:
		// "Mappings": { "AK47":"Weapon_AK47", ... }
		JsonElement mappingsEl = obj.get("Mappings");
		if (mappingsEl != null && mappingsEl.isJsonObject()) {
			for (var e : mappingsEl.getAsJsonObject().entrySet()) {
				String from = (e.getKey() != null && !e.getKey().isBlank())
				              ? e.getKey()
				              : null;
				String to = JsonValueUtils.Read.nonBlankString(e.getValue());
				if (from != null && to != null) {
					registerLink(from, to, out, sourceName);
					links++;
				}
			}
		}

		// Explicit format:
		// "Links": [ { "From":"AK47", "To":"Weapon_AK47" }, ... ]
		JsonElement linksEl = obj.get("Links");
		if (linksEl != null && linksEl.isJsonArray()) {
			for (JsonElement linkEl : linksEl.getAsJsonArray()) {
				if (!linkEl.isJsonObject()) {
					continue;
				}
				JsonObject link = linkEl.getAsJsonObject();
				String from = JsonValueUtils.Read.nonBlankString(link.get("From"));
				String to = JsonValueUtils.Read.nonBlankString(link.get("To"));
				if (from != null && to != null) {
					registerLink(from, to, out, sourceName);
					links++;
				}
			}
		}

		if (links > 0) {
			LOGGER.atInfo().log("ItemIdVersioning loaded %d link(s) from %s", links, sourceName);
		}

		return links;
	}

	private static void registerLink(@Nonnull String from, @Nonnull String to, @Nonnull Map<String, String> out,
	                                 @Nonnull String sourceName) {
		if (from.isBlank() || to.isBlank()) {
			return;
		}
		if (from.equals(to)) {
			return;
		}
		String previous = out.put(from, to);
		if (previous != null && !previous.equals(to)) {
			LOGGER
				.atWarning()
				.log("ItemIdVersioning override in %s: %s -> %s (was %s)", sourceName, from, to, previous);
		}
	}

	@Nullable
	private static String extractVersionFromFilename(@Nullable String fileName) {
		if (fileName == null || fileName.isBlank()) {
			return null;
		}
		Matcher matcher = FILENAME_VERSION_PATTERN.matcher(fileName);
		return matcher.find()
		       ? matcher.group(1)
		       : null;
	}

	private static String sanitizeVersion(@Nullable String version) {
		if (version == null || version.isBlank()) {
			return "0";
		}
		return version.trim();
	}

	private static int compareVersions(@Nullable String a, @Nullable String b) {
		List<Integer> va = extractVersionParts(a);
		List<Integer> vb = extractVersionParts(b);
		int max = Math.max(va.size(), vb.size());
		for (int i = 0; i < max; i++) {
			int ai = (i < va.size())
			         ? va.get(i)
			         : 0;
			int bi = (i < vb.size())
			         ? vb.get(i)
			         : 0;
			if (ai != bi) {
				return Integer.compare(ai, bi);
			}
		}

		return 0;
	}

	@Nonnull
	private static List<Integer> extractVersionParts(@Nullable String version) {
		if (version == null || version.isBlank()) {
			return List.of(0);
		}

		Matcher matcher = Pattern.compile("\\d+").matcher(version);
		List<Integer> parts = new ArrayList<>();
		while (matcher.find()) {
			try {
				parts.add(Integer.parseInt(matcher.group()));
			} catch (NumberFormatException ignored) {
				// ignore broken numeric segment
			}
		}

		if (parts.isEmpty()) {
			parts.add(0);
		}

		return parts;
	}

	public static boolean hasOutdatedPackWarning() {
		return REQUIRED_PACK_VERSION_WARNING != null;
	}

	@Nullable
	public static String getRequiredPackVersionWarning() {
		return REQUIRED_PACK_VERSION_WARNING;
	}

	@Nonnull
	public static String resolveLatest(@Nullable String itemId) {
		if (itemId == null || itemId.isBlank()) {
			return "";
		}
		String current = itemId;
		Set<String> visited = new HashSet<>();
		while (true) {
			String next;
			synchronized (NEXT_ID_BY_ID) {
				next = NEXT_ID_BY_ID.get(current);
			}

			if (next == null || next.isBlank() || next.equals(current)) {
				return current;
			}

			if (!visited.add(current)) {
				LOGGER.atWarning().log("Detected cycle in item id migration chain at: %s", current);
				return current;
			}

			current = next;
		}
	}

	public static int migratePlayerInventory(@Nullable Player player) {
		if (player == null) {
			return 0;
		}
		AtomicInteger changed = new AtomicInteger(0);
		migrateSection(PlayerInventoryAccess.getHotbar(player), changed);
		migrateSection(PlayerInventoryAccess.getUtility(player), changed);
		migrateSection(PlayerInventoryAccess.getStorage(player), changed);
		migrateSection(PlayerInventoryAccess.getBackpack(player), changed);
		migrateSection(PlayerInventoryAccess.getArmor(player), changed);
		migrateSection(PlayerInventoryAccess.getTools(player), changed);
		return changed.get();
	}

	private static void migrateSection(@Nullable ItemContainer container, @Nonnull AtomicInteger changed) {
		if (container == null) {
			return;
		}

		container.forEach((slot, stack) -> {
			if (stack == null || stack.isEmpty()) {
				return;
			}

			String oldId = stack.getItemId();
			if (oldId == null || oldId.isBlank()) {
				return;
			}

			String latestId = resolveLatest(oldId);
			if (oldId.equals(latestId)) {
				return;
			}

			ItemStack migrated = new ItemStack(
				latestId, stack.getQuantity(), stack.getDurability(), stack.getMaxDurability(),
				ItemStackUtils.getMetadataDocument(stack)
			);
			migrated.setOverrideDroppedItemAnimation(stack.getOverrideDroppedItemAnimation());
			container.replaceItemStackInSlot(slot, stack, migrated);
			changed.incrementAndGet();
		});
	}
}
