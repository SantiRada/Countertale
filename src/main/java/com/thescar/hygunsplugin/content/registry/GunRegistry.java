package com.thescar.hygunsplugin.content.registry;

import com.thescar.hygunsplugin.content.settings.GunSettings;

import com.hypixel.hytale.logger.HytaleLogger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Reads consolidated HyGuns per-item settings from item jsons.
 */
public final class GunRegistry {
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	/**
	 * Mutable backing store for incremental updates.
	 */
	private static final ConcurrentHashMap<String, GunSettings> LIVE_SETTINGS = new ConcurrentHashMap<>();
	/**
	 * Tracks file signatures so we only reparse changed resources.
	 */
	private static final ConcurrentHashMap<Path, FileStamp> FILE_STAMPS = new ConcurrentHashMap<>();
	/**
	 * Maps resource file -> itemId for in-place updates/removals on change.
	 */
	private static final ConcurrentHashMap<Path, String> FILE_ITEM_IDS = new ConcurrentHashMap<>();
	/**
	 * Snapshot used by readers.
	 */
	private static volatile Map<String, GunSettings> ITEM_SETTINGS = Map.of();
	@Nullable
	private static volatile Path LAST_PLUGIN_PATH;

	private GunRegistry() {
	}

	public static void loadFromJar(@Nullable Path pluginJarPath) {
		LAST_PLUGIN_PATH = pluginJarPath;
		LIVE_SETTINGS.clear();
		FILE_STAMPS.clear();
		FILE_ITEM_IDS.clear();
		try {
			if (pluginJarPath != null && Files.isDirectory(pluginJarPath)) {
				ItemAssetScanner.scanDirectory(pluginJarPath, (itemId, json) -> putExtractedSettings(LIVE_SETTINGS, itemId, json));
			} else if (pluginJarPath != null && Files.isRegularFile(pluginJarPath)) {
				ItemAssetScanner.scanZip(pluginJarPath, (itemId, json) -> putExtractedSettings(LIVE_SETTINGS, itemId, json));
			} else {
				LOGGER
					.atWarning()
					.log("GunRegistry load failed: plugin path is not a file or directory: %s", pluginJarPath);
			}

		} catch (Exception t) {
			LOGGER.atWarning().log("GunRegistry load failed: %s", t);
		}

		try {
			ItemAssetScanner.scanRuntimeMods(pluginJarPath, (itemId, json) -> putExtractedSettings(LIVE_SETTINGS, itemId, json));
		} catch (Exception t) {
			LOGGER.atWarning().log("GunRegistry runtime mods scan failed: %s", t);
		}

		ITEM_SETTINGS = Map.copyOf(LIVE_SETTINGS);
		LOGGER.atInfo().log("GunRegistry loaded %d item settings", ITEM_SETTINGS.size());
	}

	/**
	 * Incremental update path: scans only changed item json files and
	 * updates/creates settings entries.
	 */
	public static void refreshChangedResources() {
		Path pluginPath = LAST_PLUGIN_PATH;
		if (pluginPath == null) {
			return;
		}
		boolean changed = false;
		try {
			ArrayList<Path> roots = new ArrayList<>();
			ItemAssetScanner.collectDirectoryRoots(pluginPath, roots::add);
			for (Path root : roots) {
				changed |= scanDirectoryIncremental(root, LIVE_SETTINGS);
			}

		} catch (Exception t) {
			LOGGER.atWarning().log("GunRegistry incremental refresh failed: %s", t);
			return;
		}

		if (changed) {
			ITEM_SETTINGS = Map.copyOf(LIVE_SETTINGS);
			LOGGER.atInfo().log("GunRegistry refreshed item settings: %d", ITEM_SETTINGS.size());
		}
	}

	private static boolean scanDirectoryIncremental(Path pluginRoot, ConcurrentHashMap<String, GunSettings> out) throws Exception {
		boolean changed = false;
		Path dirA = pluginRoot.resolve("Server").resolve("Item").resolve("Items");
		Path dirB = pluginRoot.resolve("resources").resolve("Server").resolve("Item").resolve("Items");
		if (Files.isDirectory(dirA)) {
			changed |= scanItemsDirectory(dirA, out, true);
		}

		if (Files.isDirectory(dirB)) {
			changed |= scanItemsDirectory(dirB, out, true);
		}

		return changed;
	}

	private static boolean scanItemsDirectory(Path itemsDir, Map<String, GunSettings> out, boolean incremental) throws Exception {
		boolean changed = false;
		Set<Path> seenPaths = incremental
		                      ? new HashSet<>()
		                      : Set.of();
		try (Stream<Path> stream = Files.walk(itemsDir)) {
			for (Path p : (Iterable<Path>) stream.filter(f -> Files.isRegularFile(f) && f
				.toString()
				.endsWith(".json"))::iterator) {
				if (incremental) {
					seenPaths.add(p);
				}
				String itemId = ItemAssetScanner.toItemId(itemsDir, p);
				FileStamp stamp = new FileStamp(Files.getLastModifiedTime(p).toMillis(), Files.size(p));
				if (incremental) {
					FileStamp prev = FILE_STAMPS.get(p);
					if (stamp.equals(prev)) {
						continue;
					}
				}

				String json = Files.readString(p, StandardCharsets.UTF_8);
				GunSettings settings = extractSettings(json);
				FILE_STAMPS.put(p, stamp);
				FILE_ITEM_IDS.put(p, itemId);
				GunSettings previous = out.get(itemId);
				if (settings != null && settings.hasAnyValue()) {
					if (!settings.equals(previous)) {
						out.put(itemId, settings);
						changed = true;
					}

				} else {
					if (previous != null) {
						out.remove(itemId);
						changed = true;
					}
				}
			}
		}
		if (incremental) {
			changed |= removeDeletedFiles(itemsDir, seenPaths, out);
		}

		return changed;
	}

	private static boolean removeDeletedFiles(Path itemsDir, Set<Path> seenPaths, Map<String, GunSettings> out) {
		boolean changed = false;
		for (Path cachedPath : new ArrayList<>(FILE_STAMPS.keySet())) {
			if (!cachedPath.startsWith(itemsDir) || seenPaths.contains(cachedPath)) {
				continue;
			}

			FILE_STAMPS.remove(cachedPath);
			String itemId = FILE_ITEM_IDS.remove(cachedPath);
			if (itemId != null && out.remove(itemId) != null) {
				changed = true;
			}
		}
		return changed;
	}

	@Nullable
	private static GunSettings extractSettings(String json) {
		if (json == null || json.isEmpty()) {
			return null;
		}

		final JsonElement root;
		try {
			root = JsonParser.parseString(json);
		} catch (JsonParseException ignored) {
			return null;
		}

		if (!root.isJsonObject()) {
			return null;
		}
		JsonObject rootObj = root.getAsJsonObject();
		JsonElement hygunsEl = rootObj.get("HyGuns");
		if (hygunsEl == null || !hygunsEl.isJsonObject()) {
			return null;
		}
		JsonElement settingsEl = hygunsEl.getAsJsonObject().get("Settings");
		if (settingsEl == null || !settingsEl.isJsonObject()) {
			return null;
		}
		GunSettings settings = GunSettings.fromJson(settingsEl.getAsJsonObject());
		return settings.hasAnyValue()
		       ? settings
		       : null;
	}

	private static void putExtractedSettings(Map<String, GunSettings> out, String itemId, String json) {
		GunSettings settings = extractSettings(json);
		if (settings != null && settings.hasAnyValue()) {
			out.put(itemId, settings);
		}
	}

	@Nullable
	public static Integer getDefaultMaxAmmo(@Nullable String itemId) {
		GunSettings settings = getSettings(itemId);
		if (settings != null && settings.ammo() != null && settings.ammo().capacity() != null && settings
			.ammo()
			.capacity() > 0) {
			return settings.ammo().capacity();
		}

		return null;
	}

	@Nullable
	public static String getDefaultAmmoIcon(@Nullable String itemId) {
		return null;
	}

	@Nullable
	public static GunSettings getSettings(@Nullable String itemId) {
		return ItemAssetScanner.lookupWithVariants(ITEM_SETTINGS, itemId);
	}

	private record FileStamp(long lastModifiedMs, long size) {
	}
}
