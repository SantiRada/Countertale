package com.thescar.hygunsplugin.content.registry;

import com.thescar.hygunsplugin.content.ammo.AmmoDefinition;
import com.thescar.hygunsplugin.content.settings.AmmoItemSettings;
import com.thescar.hygunsplugin.content.settings.WeaponAmmoSettings;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class AmmoRegistry {
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	private static final ConcurrentHashMap<String, AmmoDefinition> LIVE_SETTINGS = new ConcurrentHashMap<>();
	private static volatile Map<String, AmmoDefinition> ITEM_SETTINGS = Map.of();
	@Nullable
	private static volatile Path LAST_PLUGIN_PATH;

	private AmmoRegistry() {
	}

	public static void loadFromJar(@Nullable Path pluginJarPath) {
		LAST_PLUGIN_PATH = pluginJarPath;
		LIVE_SETTINGS.clear();
		try {
			if (pluginJarPath != null && Files.isDirectory(pluginJarPath)) {
				ItemAssetScanner.scanDirectory(pluginJarPath, (itemId, json) -> putExtractedDefinition(LIVE_SETTINGS, itemId, json));
			} else if (pluginJarPath != null && Files.isRegularFile(pluginJarPath)) {
				ItemAssetScanner.scanZip(pluginJarPath, (itemId, json) -> putExtractedDefinition(LIVE_SETTINGS, itemId, json));
			}

		} catch (Exception t) {
			LOGGER.atWarning().log("AmmoRegistry load failed: %s", t);
		}

		try {
			ItemAssetScanner.scanRuntimeMods(pluginJarPath, (itemId, json) -> putExtractedDefinition(LIVE_SETTINGS, itemId, json));
		} catch (Exception t) {
			LOGGER.atWarning().log("AmmoRegistry runtime mods scan failed: %s", t);
		}

		ITEM_SETTINGS = Map.copyOf(LIVE_SETTINGS);
	}

	public static void refreshChangedResources() {
		loadFromJar(LAST_PLUGIN_PATH);
	}

	public static Map<String, AmmoDefinition> snapshot() {
		return ITEM_SETTINGS;
	}

	@Nullable
	public static AmmoDefinition getAmmo(@Nullable String itemId) {
		return ItemAssetScanner.lookupWithVariants(ITEM_SETTINGS, itemId);
	}

	public static boolean isAmmo(@Nullable String itemId) {
		AmmoDefinition definition = getAmmo(itemId);
		return definition != null && definition.hasAnyValue();
	}

	public static boolean isCompatible(@Nullable WeaponAmmoSettings weaponAmmo, @Nullable AmmoDefinition ammo) {
		if (weaponAmmo == null || ammo == null || ammo.settings() == null) {
			return false;
		}

		AmmoItemSettings ammoSettings = ammo.settings();
		return normalizedEquals(weaponAmmo.family(), ammoSettings.family())
			&& containsNormalized(weaponAmmo.weaponClasses(), ammoSettings.weaponClass());
	}

	@Nullable
	private static AmmoDefinition extractSettings(String itemId, String json) {
		if (json == null || json.isEmpty()) {
			return null;
		}

		JsonElement root;
		try {
			root = JsonParser.parseString(json);
		} catch (JsonParseException ignored) {
			return null;
		}

		if (!root.isJsonObject()) {
			return null;
		}

		JsonObject rootObj = root.getAsJsonObject();
		AmmoItemSettings settings = AmmoItemSettings.fromJson(rootObj);
		if (!settings.hasAnyValue()) {
			return null;
		}

		return new AmmoDefinition(itemId, settings);
	}

	private static void putExtractedDefinition(Map<String, AmmoDefinition> out, String itemId, String json) {
		AmmoDefinition definition = extractSettings(itemId, json);
		if (definition != null && definition.hasAnyValue()) {
			out.put(itemId, definition);
		}
	}

	private static boolean containsNormalized(Iterable<String> values, @Nullable String target) {
		if (target == null) {
			return false;
		}

		String normalizedTarget = target.trim();
		for (String value : values) {
			if (value != null && value.trim().equalsIgnoreCase(normalizedTarget)) {
				return true;
			}
		}

		return false;
	}

	private static boolean normalizedEquals(@Nullable String left, @Nullable String right) {
		if (left == null || right == null) {
			return false;
		}

		return left.trim().equalsIgnoreCase(right.trim());
	}

	public static List<AmmoDefinition> compatibleAmmo(@Nullable WeaponAmmoSettings weaponAmmo, Iterable<ItemStackView> stacks) {
		if (weaponAmmo == null) {
			return List.of();
		}

		List<AmmoDefinition> out = new ArrayList<>();
		for (ItemStackView stack : stacks) {
			AmmoDefinition definition = getAmmo(stack.itemId());
			if (definition == null || !isCompatible(weaponAmmo, definition)) {
				continue;
			}

			out.add(definition);
		}

		return out;
	}

	public static Comparator<Map.Entry<String, AmmoDefinition>> ammoOrdering() {
		return Comparator.comparingInt((Map.Entry<String, AmmoDefinition> entry) -> qualityRank(entry.getValue()))
			.thenComparingInt(entry -> isPlainAmmo(entry.getValue())
			                           ? 0
			                           : 1)
			.thenComparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER);
	}

	@Nullable
	public static String resolveDefaultAmmoItemId(Map<String, AmmoDefinition> compatibleAmmo) {
		if (compatibleAmmo == null || compatibleAmmo.isEmpty()) {
			return null;
		}

		if (compatibleAmmo.size() == 1) {
			return compatibleAmmo.keySet().iterator().next();
		}

		return compatibleAmmo.entrySet().stream().min(ammoOrdering()).map(Map.Entry::getKey).orElse(null);
	}

	private static boolean isPlainAmmo(@Nullable AmmoDefinition definition) {
		AmmoItemSettings settings = definition != null
		                            ? definition.settings()
		                            : null;
		if (settings == null) {
			return false;
		}

		boolean hasOverrides = settings.settingsOverrides() != null && settings.settingsOverrides().hasAnyValue();
		boolean hasInteractions = settings.interactions() != null && settings.interactions().hasAnyValue();
		return !hasOverrides && !hasInteractions;
	}

	private static int qualityRank(@Nullable AmmoDefinition definition) {
		AmmoItemSettings settings = definition != null
		                            ? definition.settings()
		                            : null;
		String quality = settings != null
		                 ? settings.quality()
		                 : null;
		if (quality == null) {
			return 100;
		}

		String qualityId = quality.trim();
		ItemQuality itemQuality = ItemQuality.getAssetMap().getAsset(qualityId);
		if (itemQuality != null) {
			return itemQuality.getQualityValue();
		}

		itemQuality = ItemQuality.getAssetMap().getAsset(qualityId.toLowerCase(Locale.ROOT));
		if (itemQuality != null) {
			return itemQuality.getQualityValue();
		}

		itemQuality = ItemQuality.getAssetMap().getAsset(qualityId.toUpperCase(Locale.ROOT));
		if (itemQuality != null) {
			return itemQuality.getQualityValue();
		}

		return 100;
	}

	public record ItemStackView(String itemId, int quantity) {
	}
}
