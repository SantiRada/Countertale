package com.thescar.hygunsplugin.content.settings;

import com.thescar.hygunsplugin.gameplay.projectile.HitDamageModifiers;

import javax.annotation.Nullable;

public final class GunSettingsMerger {
	private GunSettingsMerger() {
	}

	public static GunSettings merge(@Nullable GunSettings primary, @Nullable GunSettings fallback) {
		GunSettings merged = GunSettings.GROUP.merge(primary, fallback);
		merged.setAmmo(WeaponAmmoSettings.resolve(
			primary != null
			? primary.ammo()
			: null, fallback != null
			        ? fallback.ammo()
			        : null
		));
		merged.setFire(WeaponFireSettings.resolve(
			primary != null
			? primary.fire()
			: null, fallback != null
			        ? fallback.fire()
			        : null
		));
		merged.setProjectiles(WeaponProjectileSettings.resolve(
			primary != null
			? primary.projectiles()
			: null,
			fallback != null
			? fallback.projectiles()
			: null
		));
		merged.setDamageModifiers(primary != null && primary.damageModifiers() != null
		                          ? primary.damageModifiers()
		                          : fallback != null
		                            ? fallback.damageModifiers()
		                            : HitDamageModifiers.DEFAULT);
		merged.setDamageModifier(primary != null && primary.damageModifier() != null
		                         ? primary.damageModifier()
		                         : fallback != null
		                           ? fallback.damageModifier()
		                           : null);
		merged.setAutoGuidanceSettings(AutoGuidanceSettings.resolve(
			primary != null
			? primary.autoGuidanceSettings()
			: null,
			fallback != null
			? fallback.autoGuidanceSettings()
			: null, AutoGuidanceSettings.DEFAULTS
		));
		merged.setWallPenetrationSettings(WallPenetrationSettings.resolve(
			primary != null
			? primary.wallPenetrationSettings()
			: null,
			fallback != null
			? fallback.wallPenetrationSettings()
			: null, WallPenetrationSettings.DEFAULTS
		));
		return merged;
	}

	public static GunSettings merge(@Nullable GunSettings primary, @Nullable GunSettings secondary, @Nullable GunSettings fallback) {
		return merge(primary, merge(secondary, fallback));
	}
}
