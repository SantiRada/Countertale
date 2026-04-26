package com.thescar.hygunsplugin.content.settings;

import com.thescar.hygunsplugin.gameplay.projectile.DamageModifier;
import com.thescar.hygunsplugin.gameplay.projectile.HitDamageModifiers;

import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.util.Objects;

public final class GunSettings {
	public static final String WEAPON_ICON_KEY = "WeaponIcon";
	public static final String DEAL_LETHAL_DAMAGE_KEY = "DealLethalDamage";

	public static final GunSettings EMPTY = new GunSettings();
	public static final SettingsGroup<GunSettings> GROUP = SettingsGroup.inline(GunSettings::new);
	public static final SettingsGroup.Field<GunSettings, String> WEAPON_ICON = GROUP.field(
		WEAPON_ICON_KEY, CodecKind.STRING.codec(),
		JsonReadKind.STRING_NON_BLANK, GunSettings::weaponIcon, GunSettings::setWeaponIcon
	);
	public static final SettingsGroup.Field<GunSettings, Boolean> DEAL_LETHAL_DAMAGE = GROUP.field(
		DEAL_LETHAL_DAMAGE_KEY,
		CodecKind.BOOLEAN.codec(), JsonReadKind.BOOLEAN, GunSettings::dealLethalDamage, GunSettings::setDealLethalDamage
	);

	private @Nullable String weaponIcon;
	private @Nullable Boolean dealLethalDamage;
	private @Nullable WeaponAmmoSettings ammo;
	private @Nullable WeaponFireSettings fire;
	private @Nullable WeaponProjectileSettings projectiles;
	private @Nullable HitDamageModifiers damageModifiers;
	private @Nullable DamageModifier damageModifier;
	private @Nullable AutoGuidanceSettings autoGuidanceSettings;
	private @Nullable WallPenetrationSettings wallPenetrationSettings;

	public GunSettings() {
	}

	public GunSettings(@Nullable String weaponIcon, @Nullable Boolean dealLethalDamage, @Nullable WeaponAmmoSettings ammo,
	                   @Nullable WeaponFireSettings fire, @Nullable WeaponProjectileSettings projectiles, @Nullable HitDamageModifiers damageModifiers,
	                   @Nullable DamageModifier damageModifier, @Nullable AutoGuidanceSettings autoGuidanceSettings,
	                   @Nullable WallPenetrationSettings wallPenetrationSettings) {
		this.weaponIcon = weaponIcon;
		this.dealLethalDamage = dealLethalDamage;
		this.ammo = ammo;
		this.fire = fire;
		this.projectiles = projectiles;
		this.damageModifiers = damageModifiers;
		this.damageModifier = damageModifier;
		this.autoGuidanceSettings = autoGuidanceSettings;
		this.wallPenetrationSettings = wallPenetrationSettings;
	}

	public static GunSettings fromJson(@Nullable JsonObject settings) {
		GunSettings parsed = GROUP.read(settings);
		WeaponAmmoSettings ammo = WeaponAmmoSettings.fromJson(settings);
		parsed.ammo = ammo.hasAnyValue()
		              ? ammo
		              : null;
		WeaponFireSettings fire = WeaponFireSettings.fromJson(settings);
		parsed.fire = fire.hasAnyValue()
		              ? fire
		              : null;
		WeaponProjectileSettings projectiles = WeaponProjectileSettings.fromJson(settings);
		parsed.projectiles = projectiles.hasAnyValue()
		                     ? projectiles
		                     : null;
		parsed.damageModifiers = HitDamageModifiers.fromJson(settings);
		parsed.damageModifier = DamageModifier.fromRoot(settings, "DamageModifier");
		AutoGuidanceSettings autoGuidance = AutoGuidanceSettings.fromJson(settings);
		parsed.autoGuidanceSettings = autoGuidance.hasAnyValue()
		                              ? autoGuidance
		                              : null;
		WallPenetrationSettings wallPenetration = WallPenetrationSettings.fromJson(settings);
		parsed.wallPenetrationSettings = wallPenetration.hasAnyValue()
		                                 ? wallPenetration
		                                 : null;
		return parsed;
	}

	public @Nullable String weaponIcon() {
		return this.weaponIcon;
	}

	public void setWeaponIcon(@Nullable String weaponIcon) {
		this.weaponIcon = weaponIcon;
	}

	public @Nullable WeaponProjectileSettings projectiles() {
		return this.projectiles;
	}

	public void setProjectiles(@Nullable WeaponProjectileSettings projectiles) {
		this.projectiles = projectiles;
	}

	public @Nullable Boolean dealLethalDamage() {
		return this.dealLethalDamage;
	}

	public void setDealLethalDamage(@Nullable Boolean dealLethalDamage) {
		this.dealLethalDamage = dealLethalDamage;
	}

	public @Nullable WeaponAmmoSettings ammo() {
		return this.ammo;
	}

	public void setAmmo(@Nullable WeaponAmmoSettings ammo) {
		this.ammo = ammo;
	}

	public @Nullable WeaponFireSettings fire() {
		return this.fire;
	}

	public void setFire(@Nullable WeaponFireSettings fire) {
		this.fire = fire;
	}

	public @Nullable HitDamageModifiers damageModifiers() {
		return this.damageModifiers;
	}

	public void setDamageModifiers(@Nullable HitDamageModifiers damageModifiers) {
		this.damageModifiers = damageModifiers;
	}

	public @Nullable DamageModifier damageModifier() {
		return this.damageModifier;
	}

	public void setDamageModifier(@Nullable DamageModifier damageModifier) {
		this.damageModifier = damageModifier;
	}

	public @Nullable AutoGuidanceSettings autoGuidanceSettings() {
		return this.autoGuidanceSettings;
	}

	public void setAutoGuidanceSettings(@Nullable AutoGuidanceSettings autoGuidanceSettings) {
		this.autoGuidanceSettings = autoGuidanceSettings;
	}

	public @Nullable WallPenetrationSettings wallPenetrationSettings() {
		return this.wallPenetrationSettings;
	}

	public void setWallPenetrationSettings(@Nullable WallPenetrationSettings wallPenetrationSettings) {
		this.wallPenetrationSettings = wallPenetrationSettings;
	}

	public boolean hasAnyValue() {
		return GROUP.hasAnyValue(this) || this.ammo != null || this.fire != null || this.projectiles != null || this.damageModifiers != null
			|| this.damageModifier != null || this.autoGuidanceSettings != null || this.wallPenetrationSettings != null;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof GunSettings other)) {
			return false;
		}

		return Objects.equals(this.weaponIcon, other.weaponIcon) && Objects.equals(this.projectiles, other.projectiles)
			&& Objects.equals(this.dealLethalDamage, other.dealLethalDamage) && Objects.equals(this.ammo, other.ammo)
			&& Objects.equals(this.fire, other.fire) && Objects.equals(this.damageModifiers, other.damageModifiers)
			&& Objects.equals(this.damageModifier, other.damageModifier)
			&& Objects.equals(this.autoGuidanceSettings, other.autoGuidanceSettings)
			&& Objects.equals(this.wallPenetrationSettings, other.wallPenetrationSettings);
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			this.weaponIcon, this.dealLethalDamage, this.ammo, this.fire, this.projectiles, this.damageModifiers,
			this.damageModifier, this.autoGuidanceSettings, this.wallPenetrationSettings
		);
	}
}
