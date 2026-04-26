package com.thescar.hygunsplugin.content.registry;

import com.thescar.hygunsplugin.HygunsPluginMain;
import com.thescar.hygunsplugin.content.ammo.AmmoDefinition;
import com.thescar.hygunsplugin.content.settings.AmmoBoxSettings;
import com.thescar.hygunsplugin.content.settings.AmmoItemSettings;

import com.hypixel.hytale.assetstore.AssetLoadResult;
import com.hypixel.hytale.assetstore.RawAsset;
import com.hypixel.hytale.assetstore.codec.ContainedAssetCodec;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class AmmoBoxAssetGenerator {
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	private static final String SOURCE_ID = "HyGuns:AmmoBoxes";
	private static final String DEFAULT_HITBOX_TYPE = "AmmoBox";
	private static final List<String> DEFAULT_ITEM_CATEGORIES = List.of("HyGuns.Furniture");
	private static final String DEFAULT_ICON = "Icons/ItemsGenerated/Ammo_Box_Tier_I.png";
	private static final float DEFAULT_ITEM_SCALE = 1.0F;
	private static final float DEFAULT_ICON_SCALE = 0.8F;
	private static final float DEFAULT_BLOCK_MODEL_SCALE = 1.0F;
	private static final Set<String> GENERATED_ITEM_IDS = ConcurrentHashMap.newKeySet();
	private static final Set<String> GENERATED_INTERACTION_IDS = ConcurrentHashMap.newKeySet();
	private static final Set<String> GENERATED_ROOT_INTERACTION_IDS = ConcurrentHashMap.newKeySet();

	private AmmoBoxAssetGenerator() {
	}

	public static void refresh() {
		Map<String, AmmoDefinition> ammoDefinitions = AmmoRegistry.snapshot();
		if (ammoDefinitions.isEmpty()) {
			removeAllGeneratedAssets();
			return;
		}

		LinkedHashMap<String, RawAsset<String>> itemAssets = new LinkedHashMap<>();
		LinkedHashMap<String, RawAsset<String>> interactionAssets = new LinkedHashMap<>();
		LinkedHashMap<String, RawAsset<String>> rootInteractionAssets = new LinkedHashMap<>();
		for (AmmoDefinition definition : ammoDefinitions.values()) {
			collectAssets(definition, itemAssets, interactionAssets, rootInteractionAssets);
		}

		Set<String> newItemIds = itemAssets.keySet();
		Set<String> newInteractionIds = interactionAssets.keySet();
		Set<String> newRootInteractionIds = rootInteractionAssets.keySet();
		removeStaleAssets(GENERATED_ITEM_IDS, newItemIds, Item.getAssetStore());
		removeStaleAssets(GENERATED_INTERACTION_IDS, newInteractionIds, Interaction.getAssetStore());
		removeStaleAssets(GENERATED_ROOT_INTERACTION_IDS, newRootInteractionIds, RootInteraction.getAssetStore());

		loadRootInteractionAssets(rootInteractionAssets);
		loadInteractionAssets(interactionAssets);
		loadItemAssets(itemAssets);

		GENERATED_ITEM_IDS.clear();
		GENERATED_ITEM_IDS.addAll(newItemIds);
		GENERATED_INTERACTION_IDS.clear();
		GENERATED_INTERACTION_IDS.addAll(newInteractionIds);
		GENERATED_ROOT_INTERACTION_IDS.clear();
		GENERATED_ROOT_INTERACTION_IDS.addAll(newRootInteractionIds);
	}

	private static void collectAssets(AmmoDefinition definition, Map<String, RawAsset<String>> itemAssets,
	                                  Map<String, RawAsset<String>> interactionAssets,
	                                  Map<String, RawAsset<String>> rootInteractionAssets) {
		AmmoItemSettings settings = definition.settings();
		AmmoBoxSettings ammoBox = settings != null
		                          ? settings.ammoBox()
		                          : null;
		if (ammoBox == null || !ammoBox.isEnabled()) {
			return;
		}

		String ammoTypeId = AmmoAssetGenerationSupport.ammoTypeId(definition.itemId());
		String itemId = "AmmoBox_" + ammoTypeId;
		String interactionId = "Refill_AmmoBox_" + ammoTypeId;
		String rootInteractionId = "Root_Use_AmmoBox_" + ammoTypeId;
		String ammoItemId = AmmoAssetGenerationSupport.resolveAmmoItemIdForAssets(definition.itemId());

		itemAssets.put(itemId, rawItemAsset(itemId, definition.itemId(), ammoBox, rootInteractionId));
		interactionAssets.put(interactionId, rawInteractionAsset(interactionId, ammoItemId, ammoBox));
		rootInteractionAssets.put(rootInteractionId, rawRootInteractionAsset(rootInteractionId, interactionId));
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
			LOGGER.atWarning().log("Ammo box item generation had failures: %s", result.getFailedToLoadKeys());
		}
	}

	private static void loadInteractionAssets(Map<String, RawAsset<String>> assets) {
		if (assets.isEmpty()) {
			return;
		}

		AssetLoadResult<String, Interaction> result = Interaction.getAssetStore().loadBuffersWithKeys(
			SOURCE_ID,
			new ArrayList<>(assets.values()),
			null,
			false
		);
		if (result.hasFailed()) {
			LOGGER.atWarning().log("Ammo box interaction generation had failures: %s", result.getFailedToLoadKeys());
		}
	}

	private static void loadRootInteractionAssets(Map<String, RawAsset<String>> assets) {
		if (assets.isEmpty()) {
			return;
		}

		AssetLoadResult<String, RootInteraction> result = RootInteraction.getAssetStore().loadBuffersWithKeys(
			SOURCE_ID,
			new ArrayList<>(assets.values()),
			null,
			false
		);
		if (result.hasFailed()) {
			LOGGER
				.atWarning()
				.log("Ammo box root interaction generation had failures: %s", result.getFailedToLoadKeys());
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

		if (!GENERATED_INTERACTION_IDS.isEmpty()) {
			Interaction.getAssetStore().removeAssets(new ArrayList<>(GENERATED_INTERACTION_IDS));
			GENERATED_INTERACTION_IDS.clear();
		}

		if (!GENERATED_ROOT_INTERACTION_IDS.isEmpty()) {
			RootInteraction.getAssetStore().removeAssets(new ArrayList<>(GENERATED_ROOT_INTERACTION_IDS));
			GENERATED_ROOT_INTERACTION_IDS.clear();
		}
	}

	private static RawAsset<String> rawItemAsset(String itemId, String ammoItemId, AmmoBoxSettings ammoBox, String rootInteractionId) {
		JsonObject root = new JsonObject();
		JsonObject translation = new JsonObject();
		translation.addProperty("Name", AmmoAssetGenerationSupport.resolveAmmoDisplayName(ammoItemId));
		root.add("TranslationProperties", translation);
		root.add("Categories", buildCategories(ammoBox));

		JsonObject interactions = new JsonObject();
		interactions.addProperty("Primary", "Block_Primary");
		interactions.addProperty("Secondary", "Block_Secondary");
		root.add("Interactions", interactions);
		root.add("BlockType", buildBlockType(ammoBox, rootInteractionId));
		root.addProperty("PlayerAnimationsId", "Item");
		root.add("Tags", buildTags());
		root.addProperty("ItemSoundSetId", "ISS_Items_Foliage");
		root.addProperty("DropOnDeath", true);
		root.addProperty("MaxStack", 1);
		root.addProperty("Icon", AmmoAssetGenerationSupport.normalizeIconPath(ammoBox.icon(), DEFAULT_ICON));
		root.add("IconProperties", buildIconProperties(ammoBox.iconScale()));
		root.addProperty("Scale", DEFAULT_ITEM_SCALE);
		return rawAsset(itemId, root);
	}

	private static JsonArray buildCategories(AmmoBoxSettings ammoBox) {
		JsonArray categories = new JsonArray();
		List<String> values = !ammoBox.categories().isEmpty()
		                      ? ammoBox.categories()
		                      : DEFAULT_ITEM_CATEGORIES;
		for (String category : values) {
			categories.add(category);
		}

		return categories;
	}

	private static JsonObject buildBlockType(AmmoBoxSettings ammoBox, String rootInteractionId) {
		JsonObject blockType = new JsonObject();
		blockType.addProperty("InteractionHint", "server.interactionHints.ammoBoxRefill");
		blockType.addProperty("DrawType", "Model");
		blockType.addProperty("Material", "Solid");
		blockType.addProperty("Opacity", "Transparent");
		blockType.addProperty("CustomModel", ammoBox.model());

		JsonArray textures = new JsonArray();
		JsonObject texture = new JsonObject();
		texture.addProperty("Texture", ammoBox.modelTexture());
		texture.addProperty("Weight", 1);
		textures.add(texture);
		blockType.add("CustomModelTexture", textures);

		blockType.addProperty("Group", "Dev");
		blockType.addProperty("HitboxType", resolveHitboxType(ammoBox.hitboxType()));
		blockType.addProperty("VariantRotation", "NESW");
		JsonObject flags = new JsonObject();
		flags.addProperty("IsStackable", true);
		blockType.add("Flags", flags);
		JsonObject support = new JsonObject();
		JsonArray down = new JsonArray();
		JsonObject face = new JsonObject();
		face.addProperty("FaceType", "Full");
		down.add(face);
		support.add("Down", down);
		blockType.add("Support", support);
		blockType.addProperty("BlockParticleSetId", "Metal");
		blockType.addProperty("ParticleColor", "#17501d");
		blockType.addProperty("BlockSoundSetId", "Metal");
		blockType.addProperty("CustomModelScale", DEFAULT_BLOCK_MODEL_SCALE);
		blockType.addProperty("FlipType", "Symmetric");
		blockType.addProperty("RequiresAlphaBlending", false);
		JsonObject interactions = new JsonObject();
		interactions.addProperty("Use", rootInteractionId);
		blockType.add("Interactions", interactions);
		return blockType;
	}

	private static JsonObject buildTags() {
		JsonObject tags = new JsonObject();
		JsonArray type = new JsonArray();
		type.add("AmmoBox");
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
		rotation.add(350.0F);
		rotation.add(235.0F);
		rotation.add(345.0F);
		iconProperties.add("Rotation", rotation);
		JsonArray translation = new JsonArray();
		translation.add(0.0F);
		translation.add(-13.0F);
		iconProperties.add("Translation", translation);
		return iconProperties;
	}

	private static RawAsset<String> rawInteractionAsset(String interactionId, String ammoItemId, AmmoBoxSettings ammoBox) {
		JsonObject root = new JsonObject();
		root.addProperty("Type", "Charging");
		root.addProperty("DisplayProgress", true);
		root.addProperty("AllowIndefiniteHold", false);

		JsonObject next = new JsonObject();
		JsonObject setLoadedAmmo = new JsonObject();
		setLoadedAmmo.addProperty("Type", HygunsPluginMain.key("SetAmmoType"));
		setLoadedAmmo.addProperty("AmmoItemId", ammoItemId);
		JsonObject setAmmo = new JsonObject();
		setAmmo.addProperty("Type", HygunsPluginMain.key("SetAmmo"));
		setAmmo.addProperty("ToMax", true);
		setLoadedAmmo.add("Next", setAmmo);
		next.add(formatFloat(ammoBox.refillTime()), setLoadedAmmo);
		root.add("Next", next);
		return rawAsset(interactionId, root);
	}

	private static RawAsset<String> rawRootInteractionAsset(String rootInteractionId, String interactionId) {
		JsonObject root = new JsonObject();
		root.addProperty("RequireNewClick", true);
		JsonArray interactions = new JsonArray();
		interactions.add(interactionId);
		root.add("Interactions", interactions);
		return rawAsset(rootInteractionId, root);
	}

	private static String formatFloat(float value) {
		if (Math.rint(value) == value) {
			return String.format(Locale.ROOT, "%.1f", value);
		}

		return Float.toString(value);
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

	private static String resolveHitboxType(String hitboxType) {
		return hitboxType != null && !hitboxType.isBlank()
		       ? hitboxType
		       : DEFAULT_HITBOX_TYPE;
	}

	private static float resolveIconScale(Float iconScale) {
		return iconScale != null && iconScale > 0.0F
		       ? iconScale
		       : DEFAULT_ICON_SCALE;
	}
}
