package com.thescar.hygunsplugin.content.registry;

import com.thescar.hygunsplugin.content.ammo.AmmoDefinition;
import com.thescar.hygunsplugin.content.settings.AmmoItemSettings;
import com.thescar.hygunsplugin.content.settings.AmmoPileSettings;
import com.thescar.hygunsplugin.content.settings.AmmoPileVariant;

import com.hypixel.hytale.assetstore.AssetLoadResult;
import com.hypixel.hytale.assetstore.RawAsset;
import com.hypixel.hytale.assetstore.codec.ContainedAssetCodec;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemDropList;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class AmmoPileAssetGenerator {
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	private static final String SOURCE_ID = "HyGuns:AmmoPiles";
	private static final String DEFAULT_HITBOX_TYPE = "Pile_Ammo_Base_Small";
	private static final String DEFAULT_MATERIAL = "Solid";
	private static final String DEFAULT_PILE_ICON = "Icons/ItemsGenerated/Pile_Ammo_Base_Small.png";
	private static final String LEGACY_PILE_ICON_PREFIX = "Icons/ItemGenerated/";
	private static final float DEFAULT_ITEM_SCALE = 1.0F;
	private static final float DEFAULT_ICON_SCALE = 3.0F;
	private static final float DEFAULT_BLOCK_MODEL_SCALE = 0.1F;
	private static final List<String> DEFAULT_ITEM_CATEGORIES = List.of("Blocks.Furnitures");
	private static final String CRAFT_TYPE = "StructuralCrafting";
	private static final String BENCH_ID = "Builders";
	private static final String BENCH_CATEGORY = "Utility";
	private static final Set<String> GENERATED_ITEM_IDS = ConcurrentHashMap.newKeySet();
	private static final Set<String> GENERATED_DROP_IDS = ConcurrentHashMap.newKeySet();

	private AmmoPileAssetGenerator() {
	}

	public static void refresh() {
		Map<String, AmmoDefinition> ammoDefinitions = AmmoRegistry.snapshot();
		if (ammoDefinitions.isEmpty()) {
			removeAllGeneratedAssets();
			return;
		}

		LinkedHashMap<String, RawAsset<String>> itemAssets = new LinkedHashMap<>();
		LinkedHashMap<String, RawAsset<String>> dropAssets = new LinkedHashMap<>();
		for (AmmoDefinition definition : ammoDefinitions.values()) {
			collectAssets(definition, itemAssets, dropAssets);
		}

		Set<String> newItemIds = itemAssets.keySet();
		Set<String> newDropIds = dropAssets.keySet();
		removeStaleAssets(GENERATED_ITEM_IDS, newItemIds, Item.getAssetStore());
		removeStaleAssets(GENERATED_DROP_IDS, newDropIds, ItemDropList.getAssetStore());

		loadDropAssets(dropAssets);
		loadItemAssets(itemAssets);

		GENERATED_ITEM_IDS.clear();
		GENERATED_ITEM_IDS.addAll(newItemIds);
		GENERATED_DROP_IDS.clear();
		GENERATED_DROP_IDS.addAll(newDropIds);
	}

	private static void collectAssets(AmmoDefinition definition, Map<String, RawAsset<String>> itemAssets,
	                                  Map<String, RawAsset<String>> dropAssets) {
		AmmoItemSettings settings = definition.settings();
		AmmoPileSettings piles = settings != null
		                         ? settings.piles()
		                         : null;
		if (piles == null || !piles.isEnabled()) {
			return;
		}

		for (AmmoPileVariant variant : piles.variants()) {
			String itemId = pileItemId(definition.itemId(), variant.id());
			String dropId = pileDropId(definition.itemId(), variant.id());
			String ammoItemId = resolveAmmoItemIdForAssets(definition.itemId());
			itemAssets.put(itemId, rawItemAsset(itemId, definition, variant, dropId));
			dropAssets.put(dropId, rawDropAsset(dropId, ammoItemId, variant.amount()));
		}
	}

	private static void loadDropAssets(Map<String, RawAsset<String>> assets) {
		if (assets.isEmpty()) {
			return;
		}

		AssetLoadResult<String, ItemDropList> result = ItemDropList.getAssetStore().loadBuffersWithKeys(
			SOURCE_ID,
			new ArrayList<>(assets.values()),
			null,
			false
		);
		if (result.hasFailed()) {
			LOGGER.atWarning().log("Ammo pile drop generation had failures: %s", result.getFailedToLoadKeys());
		}
	}

	private static void loadItemAssets(Map<String, RawAsset<String>> assets) {
		if (assets.isEmpty()) {
			return;
		}

		AssetLoadResult<String, Item> result = Item.getAssetStore().loadBuffersWithKeys(
			SOURCE_ID,
			new ArrayList<>(assets.values()),
			null,
			false
		);
		if (result.hasFailed()) {
			LOGGER.atWarning().log("Ammo pile item generation had failures: %s", result.getFailedToLoadKeys());
		}
	}

	private static <T extends com.hypixel.hytale.assetstore.map.JsonAssetWithMap<String, ?>> void removeStaleAssets(Set<String> existing,
	                                                                                                                Set<String> refreshed,
	                                                                                                                com.hypixel.hytale.assetstore.AssetStore<String, T, ?> assetStore) {
		if (existing.isEmpty()) {
			return;
		}

		ArrayList<String> stale = new ArrayList<>();
		for (String id : existing) {
			if (!refreshed.contains(id)) {
				stale.add(id);
			}
		}
		if (!stale.isEmpty()) {
			assetStore.removeAssets(stale);
		}
	}

	private static void removeAllGeneratedAssets() {
		if (!GENERATED_ITEM_IDS.isEmpty()) {
			Item.getAssetStore().removeAssets(new ArrayList<>(GENERATED_ITEM_IDS));
			GENERATED_ITEM_IDS.clear();
		}
		if (!GENERATED_DROP_IDS.isEmpty()) {
			ItemDropList.getAssetStore().removeAssets(new ArrayList<>(GENERATED_DROP_IDS));
			GENERATED_DROP_IDS.clear();
		}
	}

	private static RawAsset<String> rawItemAsset(String itemId, AmmoDefinition definition, AmmoPileVariant variant, String dropId) {
		JsonObject root = new JsonObject();
		JsonObject translation = new JsonObject();
		translation.addProperty("Name", pileDisplayName(definition.itemId(), variant.id()));
		root.add("TranslationProperties", translation);

		root.add("Categories", buildCategories(variant));

		JsonObject interactions = new JsonObject();
		interactions.addProperty("Primary", "Block_Primary");
		interactions.addProperty("Secondary", "Block_Secondary");
		root.add("Interactions", interactions);
		root.add("BlockType", buildBlockType(variant, dropId));
		root.addProperty("PlayerAnimationsId", "Item");
		root.add("Tags", buildTags());
		root.addProperty("ItemSoundSetId", "ISS_Items_Foliage");
		root.addProperty("DropOnDeath", true);
		root.addProperty("Icon", normalizeIconPath(variant.icon()));
		root.add("IconProperties", buildIconProperties(variant.iconScale()));
		root.add("Recipe", buildRecipe(resolveAmmoItemIdForAssets(definition.itemId()), variant.amount()));
//		if (variant.scale() != null) {
//			root.addProperty("Scale", variant.scale());
//		} else {
//			root.addProperty("Scale", DEFAULT_ITEM_SCALE);
//		}

		return rawAsset(itemId, root);
	}

	private static JsonObject buildBlockType(AmmoPileVariant variant, String dropId) {
		JsonObject blockType = new JsonObject();
		blockType.addProperty("InteractionHint", "server.interactionHints.pick");
		blockType.addProperty("DrawType", "Model");
		blockType.addProperty("Material", resolveMaterial(variant.material()));
		blockType.addProperty("Opacity", "Transparent");
		blockType.addProperty("CustomModel", variant.model());
		JsonArray textures = new JsonArray();
		JsonObject texture = new JsonObject();
		texture.addProperty("Texture", variant.modelTexture());
		texture.addProperty("Weight", 1);
		textures.add(texture);
		blockType.add("CustomModelTexture", textures);
		blockType.addProperty("Group", "Dev");
		blockType.addProperty("HitboxType", resolveHitboxType(variant.hitboxType()));
		blockType.addProperty("VariantRotation", "NESW");
		JsonObject flags = new JsonObject();
		flags.addProperty("IsStackable", true);
		blockType.add("Flags", flags);
		JsonObject gathering = new JsonObject();
		JsonObject harvest = new JsonObject();
		harvest.addProperty("DropList", dropId);
		JsonObject soft = new JsonObject();
		soft.addProperty("DropList", dropId);
		gathering.add("Harvest", harvest);
		gathering.add("Soft", soft);
		blockType.add("Gathering", gathering);
		JsonObject support = new JsonObject();
		JsonArray down = new JsonArray();
		JsonObject face = new JsonObject();
		face.addProperty("FaceType", "Full");
		down.add(face);
		support.add("Down", down);
		blockType.add("Support", support);
		blockType.addProperty("BlockParticleSetId", "Metal");
		blockType.addProperty("ParticleColor", "#ffb020");
		blockType.addProperty("BlockSoundSetId", "Metal");
		if (variant.scale() != null) {
			blockType.addProperty("CustomModelScale", variant.scale());
		} else {
			blockType.addProperty("CustomModelScale", DEFAULT_BLOCK_MODEL_SCALE);
		}
		return blockType;
	}

	private static JsonArray buildCategories(AmmoPileVariant variant) {
		JsonArray categories = new JsonArray();
		List<String> values = !variant.categories().isEmpty()
		                      ? variant.categories()
		                      : DEFAULT_ITEM_CATEGORIES;
		for (String category : values) {
			categories.add(category);
		}

		return categories;
	}

	private static JsonObject buildTags() {
		JsonObject tags = new JsonObject();
		JsonArray type = new JsonArray();
		type.add("Pile");
		JsonArray family = new JsonArray();
		family.add("Ammo");
		tags.add("Type", type);
		tags.add("Family", family);
		return tags;
	}

	private static JsonObject buildIconProperties(Float iconScale) {
		JsonObject iconProperties = new JsonObject();
		iconProperties.addProperty("Scale", resolveIconScale(iconScale));
		JsonArray rotation = new JsonArray();
		rotation.add(22.5F);
		rotation.add(45.0F);
		rotation.add(22.5F);
		iconProperties.add("Rotation", rotation);
		JsonArray translation = new JsonArray();
		translation.add(0.0F);
		translation.add(-13.5F);
		iconProperties.add("Translation", translation);
		return iconProperties;
	}

	private static JsonObject buildRecipe(String ammoItemId, int amount) {
		JsonObject recipe = new JsonObject();
		JsonArray benchRequirement = new JsonArray();
		JsonObject bench = new JsonObject();
		bench.addProperty("Type", CRAFT_TYPE);
		bench.addProperty("Id", BENCH_ID);
		JsonArray categories = new JsonArray();
		categories.add(BENCH_CATEGORY);
		bench.add("Categories", categories);
		benchRequirement.add(bench);
		recipe.add("BenchRequirement", benchRequirement);
		JsonArray input = new JsonArray();
		JsonObject stack = new JsonObject();
		stack.addProperty("ItemId", ammoItemId);
		stack.addProperty("Quantity", amount);
		input.add(stack);
		recipe.add("Input", input);
		return recipe;
	}

	private static RawAsset<String> rawDropAsset(String dropId, String ammoItemId, int amount) {
		JsonObject root = new JsonObject();
		JsonObject container = new JsonObject();
		container.addProperty("Type", "Choice");
		JsonArray containers = new JsonArray();
		JsonObject multiple = new JsonObject();
		multiple.addProperty("Type", "Multiple");
		multiple.addProperty("Weight", 100);
		JsonArray multipleContainers = new JsonArray();
		JsonObject single = new JsonObject();
		single.addProperty("Type", "Single");
		JsonObject item = new JsonObject();
		item.addProperty("ItemId", ammoItemId);
		item.addProperty("QuantityMin", amount);
		item.addProperty("QuantityMax", amount);
		single.add("Item", item);
		multipleContainers.add(single);
		multiple.add("Containers", multipleContainers);
		containers.add(multiple);
		container.add("Containers", containers);
		root.add("Container", container);
		return rawAsset(dropId, root);
	}

	private static RawAsset<String> rawAsset(String key, JsonObject json) {
		return new RawAsset<>(
			Path.of(key + ".json"),
			key,
			null,
			0,
			json.toString().toCharArray(),
			null,
			ContainedAssetCodec.Mode.NONE
		);
	}

	private static String pileItemId(String ammoItemId, String variantId) {
		return "Pile_" + pileAmmoTypeId(ammoItemId) + "_" + sanitizeIdToken(variantId);
	}

	private static String pileDropId(String ammoItemId, String variantId) {
		return pileItemId(ammoItemId, variantId);
	}

	private static String pileDisplayName(String ammoItemId, String variantId) {
		return resolveAmmoDisplayName(ammoItemId) + " " + humanizeVariantId(variantId).toLowerCase(Locale.ROOT) + " pile";
	}

	private static String normalizeIconPath(String icon) {
		if (icon == null || icon.isBlank()) {
			return DEFAULT_PILE_ICON;
		}

		String normalized = icon.trim();
		if (normalized.startsWith(LEGACY_PILE_ICON_PREFIX)) {
			return "Icons/ItemsGenerated/" + normalized.substring(LEGACY_PILE_ICON_PREFIX.length());
		}

		return normalized;
	}

	private static String resolveHitboxType(String hitboxType) {
		return hitboxType != null && !hitboxType.isBlank()
		       ? hitboxType
		       : DEFAULT_HITBOX_TYPE;
	}

	private static String resolveMaterial(String material) {
		return material != null && !material.isBlank()
		       ? material
		       : DEFAULT_MATERIAL;
	}

	private static float resolveIconScale(Float iconScale) {
		return iconScale != null && iconScale > 0.0F
		       ? iconScale
		       : DEFAULT_ICON_SCALE;
	}

	private static String pileAmmoTypeId(String ammoItemId) {
		String normalized = ammoItemId.replace('\\', '/').trim();
		int colonIndex = normalized.indexOf(':');
		if (colonIndex >= 0 && colonIndex + 1 < normalized.length()) {
			normalized = normalized.substring(colonIndex + 1);
		}

		int slashIndex = normalized.lastIndexOf('/');
		if (slashIndex >= 0 && slashIndex + 1 < normalized.length()) {
			normalized = normalized.substring(slashIndex + 1);
		}

		return sanitizeIdToken(normalized);
	}

	private static String humanizeAmmoType(String ammoItemId) {
		String normalized = ammoItemId.replace('\\', '/').trim();
		int colonIndex = normalized.indexOf(':');
		if (colonIndex >= 0 && colonIndex + 1 < normalized.length()) {
			normalized = normalized.substring(colonIndex + 1);
		}

		int slashIndex = normalized.lastIndexOf('/');
		if (slashIndex >= 0 && slashIndex + 1 < normalized.length()) {
			normalized = normalized.substring(slashIndex + 1);
		}

		if (normalized.regionMatches(true, 0, "Ammo_", 0, "Ammo_".length())) {
			normalized = normalized.substring("Ammo_".length());
		}

		return humanizeToken(normalized);
	}

	private static String resolveAmmoDisplayName(String ammoItemId) {
		Item item = resolveAmmoItemAsset(ammoItemId);
		if (item == null) {
			return humanizeAmmoType(ammoItemId);
		}

		String translationKey = item.getTranslationKey();
		if (translationKey == null || translationKey.isBlank()) {
			return humanizeAmmoType(ammoItemId);
		}

		I18nModule i18n = I18nModule.get();
		if (i18n == null) {
			return humanizeAmmoType(ammoItemId);
		}

		String translated = i18n.getMessage(I18nModule.DEFAULT_LANGUAGE, translationKey);
		return translated == null || translated.isBlank() || translated.equals(translationKey)
		       ? humanizeAmmoType(ammoItemId)
		       : translated;
	}

	private static Item resolveAmmoItemAsset(String ammoItemId) {
		String normalized = ammoItemId.replace('\\', '/').trim();
		Item exact = Item.getAssetMap().getAsset(normalized);
		if (exact != null && exact != Item.UNKNOWN) {
			return exact;
		}

		int colonIndex = normalized.indexOf(':');
		if (colonIndex >= 0 && colonIndex + 1 < normalized.length()) {
			String stripped = normalized.substring(colonIndex + 1);
			Item strippedItem = Item.getAssetMap().getAsset(stripped);
			if (strippedItem != null && strippedItem != Item.UNKNOWN) {
				return strippedItem;
			}
		}

		return null;
	}

	private static String resolveAmmoItemIdForAssets(String ammoItemId) {
		Item item = resolveAmmoItemAsset(ammoItemId);
		if (item != null) {
			String id = item.getId();
			if (id != null && !id.isBlank() && item != Item.UNKNOWN) {
				return id;
			}
		}

		String normalized = ammoItemId.replace('\\', '/').trim();
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

	private static String humanizeVariantId(String variantId) {
		return humanizeToken(variantId);
	}

	private static String humanizeToken(String value) {
		String humanized = value.replace('\\', ' ')
			.replace('/', ' ')
			.replace('_', ' ')
			.replace('-', ' ')
			.trim()
			.replaceAll("\\s+", " ");
		return humanized.isBlank()
		       ? "Unknown"
		       : humanized;
	}

	private static String sanitizeIdToken(String value) {
		StringBuilder out = new StringBuilder(value.length());
		boolean lastWasUnderscore = false;
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			boolean safe = c >= 'A' && c <= 'Z'
				|| c >= 'a' && c <= 'z'
				|| c >= '0' && c <= '9'
				|| c == '_';
			if (safe) {
				out.append(c);
				lastWasUnderscore = false;
				continue;
			}

			if (!lastWasUnderscore) {
				out.append('_');
				lastWasUnderscore = true;
			}
		}

		String sanitized = out.toString().replaceAll("^_+|_+$", "");
		if (sanitized.isBlank()) {
			return "Unknown";
		}

		return sanitized;
	}
}
