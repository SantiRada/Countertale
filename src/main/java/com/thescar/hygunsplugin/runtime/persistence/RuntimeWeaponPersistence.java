package com.thescar.hygunsplugin.runtime.persistence;

import com.thescar.hygunsplugin.HygunsPluginMain;
import com.thescar.hygunsplugin.debug.DebugLogger;
import com.thescar.hygunsplugin.runtime.components.AmmoDataComponent;
import com.thescar.hygunsplugin.support.hytale.ItemStackUtils;

import com.hypixel.hytale.server.core.inventory.ItemStack;

import org.bson.BsonBoolean;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class RuntimeWeaponPersistence {
	public static final String PERSISTENCE_KEY = HygunsPluginMain.key("Persistence");
	public static final String AMMO_DATA_KEY = "AmmoData";
	public static final String AMMO_FIELD = "Ammo";
	public static final String MAX_AMMO_FIELD = "MaxAmmo";
	public static final String INITIALIZED_FIELD = "Initialized";
	public static final String SELECTED_AMMO_ITEM_ID_FIELD = "SelectedAmmoItemId";
	public static final String LOADED_AMMO_ITEM_ID_FIELD = "LoadedAmmoItemId";
	public static final String LOADED_AMMO_ICON_FIELD = "LoadedAmmoIcon";

	private RuntimeWeaponPersistence() {
	}

	@Nonnull
	public static PersistedAmmoState snapshot(@Nonnull AmmoDataComponent ammo) {
		return new PersistedAmmoState(
			ammo.ammo(), ammo.maxAmmo(), ammo.initialized(), ammo.selectedAmmoItemId(), ammo.loadedAmmoItemId(),
			ammo.loadedAmmoIcon()
		);
	}

	@Nullable
	public static PersistedAmmoState readAmmo(@Nullable ItemStack stack) {
		BsonDocument document = readAmmoDocument(stack);
		if (document == null) {
			DebugLogger.debug("AmmoPersistence", () -> "Read ammo metadata: itemId="
				+ (stack != null ? stack.getItemId() : "null")
				+ ", state=null");
			return null;
		}

		PersistedAmmoState state = new PersistedAmmoState(
			readInt(document, AMMO_FIELD, 0), readInt(document, MAX_AMMO_FIELD, 1),
			readBoolean(document, INITIALIZED_FIELD, false), readString(document, SELECTED_AMMO_ITEM_ID_FIELD),
			readString(document, LOADED_AMMO_ITEM_ID_FIELD), readString(document, LOADED_AMMO_ICON_FIELD)
		);
		DebugLogger.debug("AmmoPersistence", () -> "Read ammo metadata: itemId="
			+ (stack != null ? stack.getItemId() : "null")
			+ ", ammo=" + state.ammo()
			+ ", max=" + state.maxAmmo()
			+ ", selected=" + state.selectedAmmoItemId()
			+ ", loaded=" + state.loadedAmmoItemId());
		return state;
	}

	@Nonnull
	public static ItemStack writeAmmo(@Nonnull ItemStack stack, @Nonnull AmmoDataComponent ammo) {
		return writeAmmo(stack, snapshot(ammo));
	}

	@Nonnull
	public static ItemStack writeAmmo(@Nonnull ItemStack stack, @Nonnull PersistedAmmoState state) {
		BsonDocument existingPersistence = ItemStackUtils.getCustomDocument(stack, PERSISTENCE_KEY);
		BsonDocument updatedPersistence = existingPersistence != null
		                                  ? existingPersistence.clone()
		                                  : new BsonDocument();
		BsonDocument existingAmmo = getNestedDocument(existingPersistence, AMMO_DATA_KEY);
		BsonDocument updatedAmmo = new BsonDocument();
		updatedAmmo.put(AMMO_FIELD, new BsonInt32(Math.max(0, state.ammo())));
		updatedAmmo.put(MAX_AMMO_FIELD, new BsonInt32(Math.max(1, state.maxAmmo())));
		updatedAmmo.put(INITIALIZED_FIELD, BsonBoolean.valueOf(state.initialized()));
		putNullableString(updatedAmmo, SELECTED_AMMO_ITEM_ID_FIELD, state.selectedAmmoItemId());
		putNullableString(updatedAmmo, LOADED_AMMO_ITEM_ID_FIELD, state.loadedAmmoItemId());
		putNullableString(updatedAmmo, LOADED_AMMO_ICON_FIELD, state.loadedAmmoIcon());
		if (updatedAmmo.equals(existingAmmo)) {
			DebugLogger.debug("AmmoPersistence", () -> "Skipped ammo metadata write (unchanged): itemId="
				+ stack.getItemId()
				+ ", ammo=" + state.ammo()
				+ ", max=" + state.maxAmmo()
				+ ", selected=" + state.selectedAmmoItemId()
				+ ", loaded=" + state.loadedAmmoItemId());
			return stack;
		}

		updatedPersistence.put(AMMO_DATA_KEY, updatedAmmo);
		DebugLogger.debug("AmmoPersistence", () -> "Wrote ammo metadata: itemId="
			+ stack.getItemId()
			+ ", ammo=" + state.ammo()
			+ ", max=" + state.maxAmmo()
			+ ", selected=" + state.selectedAmmoItemId()
			+ ", loaded=" + state.loadedAmmoItemId());
		return ItemStackUtils.setCustomDocument(stack, PERSISTENCE_KEY, updatedPersistence);
	}

	public static void apply(@Nonnull AmmoDataComponent target, @Nonnull PersistedAmmoState state) {
		target.setAmmo(state.ammo());
		target.setMaxAmmo(state.maxAmmo());
		target.setInitialized(state.initialized());
		target.setSelectedAmmoItemId(state.selectedAmmoItemId());
		target.setLoadedAmmoItemId(state.loadedAmmoItemId());
		target.setLoadedAmmoIcon(state.loadedAmmoIcon());
		target.clearDirty();
	}

	private static int readInt(@Nonnull BsonDocument object, @Nonnull String key, int fallback) {
		if (!object.containsKey(key) || !object.get(key).isInt32()) {
			return fallback;
		}

		return object.getInt32(key).getValue();
	}

	private static boolean readBoolean(@Nonnull BsonDocument object, @Nonnull String key, boolean fallback) {
		if (!object.containsKey(key)) {
			return fallback;
		}

		if (object.get(key).isBoolean()) {
			return object.getBoolean(key).getValue();
		}

		if (object.get(key).isInt32()) {
			return object.getInt32(key).getValue() != 0;
		}

		return fallback;
	}

	private static @Nullable String readString(@Nonnull BsonDocument object, @Nonnull String key) {
		if (!object.containsKey(key) || !object.get(key).isString()) {
			return null;
		}

		String raw = object.getString(key).getValue();
		return raw == null || raw.isBlank()
		       ? null
		       : raw.trim();
	}

	private static void putNullableString(@Nonnull BsonDocument document, @Nonnull String key, @Nullable String value) {
		if (value != null && !value.isBlank()) {
			document.put(key, new BsonString(value));
		}
	}

	@Nullable
	private static BsonDocument readAmmoDocument(@Nullable ItemStack stack) {
		BsonDocument persistence = ItemStackUtils.getCustomDocument(stack, PERSISTENCE_KEY);
		return getNestedDocument(persistence, AMMO_DATA_KEY);
	}

	@Nullable
	private static BsonDocument getNestedDocument(@Nullable BsonDocument document, @Nonnull String key) {
		if (document == null || !document.containsKey(key) || !document.get(key).isDocument()) {
			return null;
		}

		return document.getDocument(key);
	}

	public record PersistedAmmoState(
		int ammo, int maxAmmo, boolean initialized, @Nullable String selectedAmmoItemId,
		@Nullable String loadedAmmoItemId, @Nullable String loadedAmmoIcon
	) {
	}
}
