package com.thescar.hygunsplugin.content.settings;

import com.thescar.hygunsplugin.support.json.JsonValueUtils;

import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.util.Objects;

public final class AmmoItemSettings {
	private @Nullable String family;
	private @Nullable String weaponClass;
	private @Nullable String quality;
	private @Nullable String icon;
	private @Nullable GunSettings settingsOverrides;
	private @Nullable AmmoItemInteractions interactions;
	private @Nullable AmmoPileSettings piles;
	private @Nullable AmmoBoxSettings ammoBox;

	public AmmoItemSettings() {
	}

	public AmmoItemSettings(@Nullable String family, @Nullable String weaponClass, @Nullable String quality, @Nullable String icon,
	                        @Nullable GunSettings settingsOverrides, @Nullable AmmoItemInteractions interactions,
	                        @Nullable AmmoPileSettings piles, @Nullable AmmoBoxSettings ammoBox) {
		this.family = family;
		this.weaponClass = weaponClass;
		this.quality = quality;
		this.icon = icon;
		this.settingsOverrides = settingsOverrides;
		this.interactions = interactions;
		this.piles = piles;
		this.ammoBox = ammoBox;
	}

	public static AmmoItemSettings fromJson(@Nullable JsonObject root) {
		JsonObject ammoObject = readAmmoObject(root);
		AmmoItemSettings parsed = new AmmoItemSettings();
		parsed.family = JsonValueUtils.Read.stringOrFirst(ammoObject, "Family");
		parsed.weaponClass = JsonValueUtils.Read.stringOrFirst(ammoObject, "WeaponClass");
		parsed.quality = JsonValueUtils.Read.stringOrFirst(root, "Quality");
		parsed.icon = JsonValueUtils.Read.stringOrFirst(ammoObject, "Icon");
		GunSettings overrides = GunSettings.fromJson(readSettingsOverrides(ammoObject));
		parsed.settingsOverrides = overrides.hasAnyValue()
		                           ? overrides
		                           : null;
		AmmoItemInteractions interactions = AmmoItemInteractions.fromJson(ammoObject);
		parsed.interactions = interactions.hasAnyValue()
		                      ? interactions
		                      : null;
		AmmoPileSettings piles = AmmoPileSettings.fromJson(readPiles(ammoObject));
		parsed.piles = piles.hasAnyValue()
		               ? piles
		               : null;
		AmmoBoxSettings ammoBox = AmmoBoxSettings.fromJson(readAmmoBox(ammoObject));
		parsed.ammoBox = ammoBox != null && ammoBox.hasAnyValue()
		                 ? ammoBox
		                 : null;
		return parsed;
	}

	@Nullable
	private static JsonObject readAmmoObject(@Nullable JsonObject root) {
		if (root == null) {
			return null;
		}

		JsonObject hyguns = JsonValueUtils.Read.object(root, "HyGuns");
		if (hyguns == null) {
			return null;
		}

		return JsonValueUtils.Read.object(hyguns, "AmmoSettings");
	}

	@Nullable
	private static JsonObject readSettingsOverrides(@Nullable JsonObject ammoObject) {
		if (ammoObject == null) {
			return null;
		}

		return JsonValueUtils.Read.object(ammoObject, "SettingsOverrides");
	}

	@Nullable
	private static JsonObject readPiles(@Nullable JsonObject ammoObject) {
		if (ammoObject == null) {
			return null;
		}

		return JsonValueUtils.Read.object(ammoObject, "Piles");
	}

	@Nullable
	private static JsonObject readAmmoBox(@Nullable JsonObject ammoObject) {
		if (ammoObject == null) {
			return null;
		}

		return JsonValueUtils.Read.object(ammoObject, "AmmoBox");
	}

	public @Nullable String family() {
		return this.family;
	}

	public void setFamily(@Nullable String family) {
		this.family = family;
	}

	public @Nullable String weaponClass() {
		return this.weaponClass;
	}

	public void setWeaponClass(@Nullable String weaponClass) {
		this.weaponClass = weaponClass;
	}

	public @Nullable String icon() {
		return this.icon;
	}

	public @Nullable String quality() {
		return this.quality;
	}

	public void setQuality(@Nullable String quality) {
		this.quality = quality;
	}

	public void setIcon(@Nullable String icon) {
		this.icon = icon;
	}

	public @Nullable GunSettings settingsOverrides() {
		return this.settingsOverrides;
	}

	public void setSettingsOverrides(@Nullable GunSettings settingsOverrides) {
		this.settingsOverrides = settingsOverrides;
	}

	public @Nullable AmmoItemInteractions interactions() {
		return this.interactions;
	}

	public void setInteractions(@Nullable AmmoItemInteractions interactions) {
		this.interactions = interactions;
	}

	public @Nullable AmmoPileSettings piles() {
		return this.piles;
	}

	public void setPiles(@Nullable AmmoPileSettings piles) {
		this.piles = piles;
	}

	public @Nullable AmmoBoxSettings ammoBox() {
		return this.ammoBox;
	}

	public void setAmmoBox(@Nullable AmmoBoxSettings ammoBox) {
		this.ammoBox = ammoBox;
	}

	public boolean hasAnyValue() {
		return this.family != null || this.weaponClass != null || this.quality != null || this.icon != null
			|| this.settingsOverrides != null || this.interactions != null || this.piles != null || this.ammoBox != null;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof AmmoItemSettings other)) {
			return false;
		}

		return Objects.equals(this.family, other.family) && Objects.equals(this.weaponClass, other.weaponClass)
			&& Objects.equals(this.quality, other.quality) && Objects.equals(this.icon, other.icon)
			&& Objects.equals(this.settingsOverrides, other.settingsOverrides) && Objects.equals(this.interactions, other.interactions)
			&& Objects.equals(this.piles, other.piles) && Objects.equals(this.ammoBox, other.ammoBox);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.family, this.weaponClass, this.quality, this.icon, this.settingsOverrides, this.interactions, this.piles, this.ammoBox);
	}
}
