package com.thescar.hygunsplugin.content.settings;

import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class WeaponAmmoSettings {
	public static final WeaponAmmoSettings EMPTY = new WeaponAmmoSettings();
	public static final SettingsGroup<WeaponAmmoSettings> GROUP = createGroup();
	private final Set<String> weaponClasses = new LinkedHashSet<>();
	private @Nullable String family;
	private @Nullable String itemId;
	private @Nullable Integer capacity;
	private @Nullable WeaponReloadSettings reload;
	private @Nullable AmmoSaveSettings saveSettings;

	public WeaponAmmoSettings() {
	}

	public WeaponAmmoSettings(@Nullable String family, Set<String> weaponClasses, @Nullable String itemId, @Nullable Integer capacity,
	                          @Nullable WeaponReloadSettings reload, @Nullable AmmoSaveSettings saveSettings) {
		this.family = family;
		this.weaponClasses.addAll(weaponClasses);
		this.itemId = itemId;
		this.capacity = capacity;
		this.reload = reload;
		this.saveSettings = saveSettings;
	}

	public static WeaponAmmoSettings fromJson(@Nullable JsonObject settings) {
		WeaponAmmoSettings parsed = GROUP.read(settings);
		JsonObject ammoObject = readAmmoObject(settings);
		parsed.setWeaponClasses(readStringSet(ammoObject, "WeaponClass"));
		WeaponReloadSettings reload = WeaponReloadSettings.fromJson(ammoObject);
		parsed.reload = reload.hasAnyValue()
		                ? reload
		                : null;
		AmmoSaveSettings save = AmmoSaveSettings.fromJson(ammoObject);
		parsed.saveSettings = save.hasAnyValue()
		                      ? save
		                      : null;
		return parsed;
	}

	public static WeaponAmmoSettings resolve(@Nullable WeaponAmmoSettings primary, @Nullable WeaponAmmoSettings fallback) {
		WeaponAmmoSettings merged = GROUP.merge(primary, fallback);
		if (primary != null && !primary.weaponClasses().isEmpty()) {
			merged.setWeaponClasses(primary.weaponClasses());
		} else if (fallback != null && !fallback.weaponClasses().isEmpty()) {
			merged.setWeaponClasses(fallback.weaponClasses());
		}

		WeaponReloadSettings primaryReload = primary != null
		                                     ? primary.reload()
		                                     : null;
		WeaponReloadSettings fallbackReload = fallback != null
		                                      ? fallback.reload()
		                                      : null;
		merged.reload = WeaponReloadSettings.resolve(primaryReload, fallbackReload);
		AmmoSaveSettings primarySave = primary != null
		                               ? primary.saveSettings()
		                               : null;
		AmmoSaveSettings fallbackSave = fallback != null
		                                ? fallback.saveSettings()
		                                : null;
		merged.saveSettings = AmmoSaveSettings.resolve(primarySave, fallbackSave, AmmoSaveSettings.DEFAULTS);
		return merged;
	}

	private static SettingsGroup<WeaponAmmoSettings> createGroup() {
		SettingsGroup<WeaponAmmoSettings> group = SettingsGroup.nested("Ammo", WeaponAmmoSettings::new);
		group.field(
			"Family", CodecKind.STRING.codec(), JsonReadKind.STRING_NON_BLANK, WeaponAmmoSettings::family,
			WeaponAmmoSettings::setFamily
		);
		group.field(
			"ItemId", CodecKind.STRING.codec(), JsonReadKind.STRING_NON_BLANK, WeaponAmmoSettings::itemId,
			WeaponAmmoSettings::setItemId
		);
		group.field(
			"Capacity", CodecKind.INTEGER.codec(), JsonReadKind.INTEGER_POSITIVE, WeaponAmmoSettings::capacity,
			WeaponAmmoSettings::setCapacity, "Max"
		);
		return group;
	}

	@Nullable
	private static JsonObject readAmmoObject(@Nullable JsonObject settings) {
		if (settings == null) {
			return null;
		}

		var ammo = settings.get("Ammo");
		return ammo != null && ammo.isJsonObject()
		       ? ammo.getAsJsonObject()
		       : null;
	}

	private static Set<String> readStringSet(@Nullable JsonObject root, String key) {
		LinkedHashSet<String> values = new LinkedHashSet<>();
		if (root == null) {
			return values;
		}

		var raw = root.get(key);
		if (raw == null) {
			return values;
		}

		if (raw.isJsonArray()) {
			for (var element : raw.getAsJsonArray()) {
				addNormalized(
					values, element != null && element.isJsonPrimitive()
					        ? element.getAsString()
					        : null
				);
			}

			return values;
		}

		if (raw.isJsonPrimitive()) {
			addNormalized(values, raw.getAsString());
		}

		return values;
	}

	private static void addNormalized(Set<String> values, @Nullable String value) {
		if (value == null) {
			return;
		}

		String trimmed = value.trim();
		if (!trimmed.isEmpty()) {
			values.add(trimmed);
		}
	}

	public @Nullable String family() {
		return this.family;
	}

	public void setFamily(@Nullable String family) {
		this.family = family;
	}

	public Set<String> weaponClasses() {
		return Set.copyOf(this.weaponClasses);
	}

	public void setWeaponClasses(Set<String> weaponClasses) {
		this.weaponClasses.clear();
		this.weaponClasses.addAll(weaponClasses);
	}

	public @Nullable String itemId() {
		return this.itemId;
	}

	public void setItemId(@Nullable String itemId) {
		this.itemId = itemId;
	}

	public @Nullable Integer capacity() {
		return this.capacity;
	}

	public void setCapacity(@Nullable Integer capacity) {
		this.capacity = capacity;
	}

	public @Nullable WeaponReloadSettings reload() {
		return this.reload;
	}

	public void setReload(@Nullable WeaponReloadSettings reload) {
		this.reload = reload;
	}

	public @Nullable AmmoSaveSettings saveSettings() {
		return this.saveSettings;
	}

	public void setSaveSettings(@Nullable AmmoSaveSettings saveSettings) {
		this.saveSettings = saveSettings;
	}

	public boolean hasAnyValue() {
		return GROUP.hasAnyValue(this) || !this.weaponClasses.isEmpty() || this.reload != null || this.saveSettings != null;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof WeaponAmmoSettings other)) {
			return false;
		}

		return Objects.equals(this.family, other.family) && Objects.equals(this.weaponClasses, other.weaponClasses)
			&& Objects.equals(this.itemId, other.itemId) && Objects.equals(this.capacity, other.capacity)
			&& Objects.equals(this.reload, other.reload) && Objects.equals(this.saveSettings, other.saveSettings);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.family, this.weaponClasses, this.itemId, this.capacity, this.reload, this.saveSettings);
	}
}
