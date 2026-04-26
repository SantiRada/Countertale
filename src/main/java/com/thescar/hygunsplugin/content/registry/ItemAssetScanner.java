package com.thescar.hygunsplugin.content.registry;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.server.core.Options;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.plugin.PluginManager;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ItemAssetScanner {
	private static final String ITEMS_PREFIX_A = "Server/Item/Items/";
	private static final String ITEMS_PREFIX_B = "resources/Server/Item/Items/";

	private ItemAssetScanner() {
	}

	public static void scanZip(Path pluginJarPath, ItemJsonConsumer consumer) throws Exception {
		try (ZipFile zf = new ZipFile(pluginJarPath.toFile())) {
			Enumeration<? extends ZipEntry> entries = zf.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				String name = entry.getName();
				String prefix = resolveZipPrefix(name);
				if (prefix == null || !name.endsWith(".json")) {
					continue;
				}

				String itemId = toItemId(name.substring(prefix.length(), name.length() - ".json".length()));
				String json;
				try (InputStream in = zf.getInputStream(entry)) {
					json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
				}

				consumer.accept(itemId, json);
			}
		}
	}

	public static void scanDirectory(Path pluginRoot, ItemJsonConsumer consumer) throws Exception {
		Path dirA = pluginRoot.resolve("Server").resolve("Item").resolve("Items");
		Path dirB = pluginRoot.resolve("resources").resolve("Server").resolve("Item").resolve("Items");
		if (Files.isDirectory(dirA)) {
			scanItemsDirectory(dirA, consumer);
		}

		if (Files.isDirectory(dirB)) {
			scanItemsDirectory(dirB, consumer);
		}
	}

	public static void scanItemsDirectory(Path itemsDir, ItemJsonConsumer consumer) throws Exception {
		try (Stream<Path> stream = Files.walk(itemsDir)) {
			for (Path path : (Iterable<Path>) stream
				.filter(file -> Files.isRegularFile(file) && file.toString().endsWith(".json"))::iterator) {
				String itemId = toItemId(itemsDir, path);
				String json = Files.readString(path, StandardCharsets.UTF_8);
				consumer.accept(itemId, json);
			}
		}
	}

	public static void forEachRuntimeModRoot(@Nullable Path pluginJarPath, Consumer<Path> consumer) throws Exception {
		LinkedHashSet<Path> seenEntries = new LinkedHashSet<>();
		for (Path modsDir : resolveModsDirectories()) {
			if (!Files.isDirectory(modsDir)) {
				continue;
			}

			try (Stream<Path> entries = Files.list(modsDir)) {
				entries.filter(Objects::nonNull).forEach(entry -> {
					try {
						Path normalizedEntry = entry.normalize();
						if (!seenEntries.add(normalizedEntry)) {
							return;
						}

						if (pluginJarPath != null && Files.exists(pluginJarPath) && Files.isSameFile(normalizedEntry, pluginJarPath)) {
							return;
						}

						consumer.accept(normalizedEntry);
					} catch (Exception ignored) {
						// Ignore one bad mod and continue.
					}

				});
			}
		}

		for (Path packLocation : resolveAssetPackLocations()) {
			try {
				Path normalizedLocation = packLocation.normalize();
				if (!seenEntries.add(normalizedLocation)) {
					continue;
				}

				if (pluginJarPath != null && Files.exists(pluginJarPath) && Files.isSameFile(normalizedLocation, pluginJarPath)) {
					continue;
				}

				consumer.accept(normalizedLocation);
			} catch (Exception ignored) {
				// Ignore one bad mod and continue.
			}
		}
	}

	public static void scanRuntimeMods(@Nullable Path pluginJarPath, ItemJsonConsumer consumer) throws Exception {
		forEachRuntimeModRoot(
			pluginJarPath, entry -> {
				try {
					if (Files.isDirectory(entry)) {
						scanDirectory(entry, consumer);
						return;
					}

					String name = entry.getFileName() != null
					              ? entry.getFileName().toString().toLowerCase(Locale.ROOT)
					              : "";
					if (Files.isRegularFile(entry) && (name.endsWith(".jar") || name.endsWith(".zip"))) {
						scanZip(entry, consumer);
					}

				} catch (Exception ignored) {
					// Ignore one bad mod and continue.
				}

			}
		);
	}

	public static void collectDirectoryRoots(@Nullable Path pluginJarPath, Consumer<Path> consumer) throws Exception {
		LinkedHashSet<Path> seenDirectories = new LinkedHashSet<>();
		if (pluginJarPath != null && Files.isDirectory(pluginJarPath)) {
			Path normalizedPluginPath = pluginJarPath.normalize();
			seenDirectories.add(normalizedPluginPath);
			consumer.accept(normalizedPluginPath);
		}

		forEachRuntimeModRoot(
			pluginJarPath, entry -> {
				if (!Files.isDirectory(entry)) {
					return;
				}

				Path normalizedEntry = entry.normalize();
				if (seenDirectories.add(normalizedEntry)) {
					consumer.accept(normalizedEntry);
				}

			}
		);
	}

	static LinkedHashSet<Path> resolveModsDirectories() {
		LinkedHashSet<Path> modsDirectories = new LinkedHashSet<>();
		if (PluginManager.MODS_PATH != null) {
			modsDirectories.add(PluginManager.MODS_PATH.normalize());
		}

		var optionSet = Options.getOptionSet();
		if (optionSet != null) {
			for (Path path : optionSet.valuesOf(Options.MODS_DIRECTORIES)) {
				if (path != null) {
					modsDirectories.add(path.normalize());
				}
			}
		}

		return modsDirectories;
	}

	static LinkedHashSet<Path> resolveAssetPackLocations() {
		LinkedHashSet<Path> packLocations = new LinkedHashSet<>();
		AssetModule assetModule = AssetModule.get();
		if (assetModule == null) {
			return packLocations;
		}

		for (AssetPack assetPack : assetModule.getAssetPacks()) {
			if (assetPack != null && assetPack.getPackLocation() != null) {
				packLocations.add(assetPack.getPackLocation().normalize());
			}
		}

		return packLocations;
	}

	@Nullable
	public static <T> T lookupWithVariants(Map<String, T> map, @Nullable String itemId) {
		if (itemId == null || itemId.isBlank()) {
			return null;
		}

		String key = itemId.trim().replace('\\', '/');
		T value = map.get(key);
		if (value != null) {
			return value;
		}

		int colon = key.indexOf(':');
		if (colon >= 0 && colon + 1 < key.length()) {
			String stripped = key.substring(colon + 1);
			value = map.get(stripped);
			if (value != null) {
				return value;
			}

			key = stripped;
		}

		String keyLower = key.toLowerCase(Locale.ROOT);
		for (Map.Entry<String, T> entry : map.entrySet()) {
			String candidate = entry.getKey();
			if (candidate == null) {
				continue;
			}

			String candidateNorm = candidate.replace('\\', '/');
			String candidateLower = candidateNorm.toLowerCase(Locale.ROOT);
			if (candidateLower.equals(keyLower) || candidateLower.endsWith("/" + keyLower) || keyLower.endsWith("/" + candidateLower)) {
				return entry.getValue();
			}
		}

		return null;
	}

	public static String toItemId(Path itemsDir, Path file) {
		String rel = itemsDir.relativize(file).toString().replace('\\', '/');
		return toItemId(rel.substring(0, rel.length() - ".json".length()));
	}

	public static String toItemId(String assetPath) {
		String normalized = assetPath.replace('\\', '/').trim();
		int colonIndex = normalized.indexOf(':');
		if (colonIndex >= 0 && colonIndex + 1 < normalized.length()) {
			normalized = normalized.substring(colonIndex + 1);
		}

		int slashIndex = normalized.lastIndexOf('/');
		if (slashIndex >= 0 && slashIndex + 1 < normalized.length()) {
			normalized = normalized.substring(slashIndex + 1);
		}

		return normalized;
	}

	@Nullable
	private static String resolveZipPrefix(String path) {
		if (path.startsWith(ITEMS_PREFIX_A)) {
			return ITEMS_PREFIX_A;
		}

		if (path.startsWith(ITEMS_PREFIX_B)) {
			return ITEMS_PREFIX_B;
		}

		return null;
	}

	@FunctionalInterface
	public interface ItemJsonConsumer {
		void accept(String itemId, String json) throws Exception;
	}
}
