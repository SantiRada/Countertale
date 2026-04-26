package com.thescar.hygunsplugin.content.settings;

import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.util.Objects;

public final class WeaponProjectileSettings {
	public static final WeaponProjectileSettings EMPTY = new WeaponProjectileSettings();
	public static final SettingsGroup<WeaponProjectileSettings> GROUP = createGroup();

	private @Nullable String configId;
	private @Nullable String projectileId;
	private @Nullable Double spread;
	private @Nullable Integer count;
	private @Nullable Integer damage;

	public WeaponProjectileSettings() {
	}

	public WeaponProjectileSettings(@Nullable String configId, @Nullable String projectileId, @Nullable Double spread,
	                                @Nullable Integer count, @Nullable Integer damage) {
		this.configId = configId;
		this.projectileId = projectileId;
		this.spread = spread;
		this.count = count;
		this.damage = damage;
	}

	public static WeaponProjectileSettings fromJson(@Nullable JsonObject settings) {
		return GROUP.read(settings);
	}

	public static WeaponProjectileSettings resolve(@Nullable WeaponProjectileSettings primary,
	                                               @Nullable WeaponProjectileSettings fallback) {
		return GROUP.merge(primary, fallback);
	}

	private static SettingsGroup<WeaponProjectileSettings> createGroup() {
		SettingsGroup<WeaponProjectileSettings> group = SettingsGroup.nested("Projectiles", WeaponProjectileSettings::new);
		group.field(
			"ConfigId", CodecKind.STRING.codec(), JsonReadKind.STRING_NON_BLANK, WeaponProjectileSettings::configId,
			WeaponProjectileSettings::setConfigId
		);
		group.field(
			"ProjectileId", CodecKind.STRING.codec(), JsonReadKind.STRING_NON_BLANK, WeaponProjectileSettings::projectileId,
			WeaponProjectileSettings::setProjectileId, "ProjectileID"
		);
		group.field(
			"Spread", CodecKind.DOUBLE.codec(), JsonReadKind.DOUBLE, WeaponProjectileSettings::spread,
			WeaponProjectileSettings::setSpread
		);
		group.field(
			"Count", CodecKind.INTEGER.codec(), JsonReadKind.INTEGER_POSITIVE, WeaponProjectileSettings::count,
			WeaponProjectileSettings::setCount
		);
		group.field(
			"Damage", CodecKind.INTEGER.codec(), JsonReadKind.INTEGER_NON_NEGATIVE, WeaponProjectileSettings::damage,
			WeaponProjectileSettings::setDamage
		);
		return group;
	}

	public @Nullable String configId() {
		return this.configId;
	}

	public void setConfigId(@Nullable String configId) {
		this.configId = configId;
	}

	public @Nullable String projectileId() {
		return this.projectileId;
	}

	public void setProjectileId(@Nullable String projectileId) {
		this.projectileId = projectileId;
	}

	public @Nullable Double spread() {
		return this.spread;
	}

	public void setSpread(@Nullable Double spread) {
		this.spread = spread;
	}

	public @Nullable Integer count() {
		return this.count;
	}

	public void setCount(@Nullable Integer count) {
		this.count = count;
	}

	public @Nullable Integer damage() {
		return this.damage;
	}

	public void setDamage(@Nullable Integer damage) {
		this.damage = damage;
	}

	public boolean hasAnyValue() {
		return GROUP.hasAnyValue(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof WeaponProjectileSettings other)) {
			return false;
		}

		return Objects.equals(this.configId, other.configId) && Objects.equals(this.projectileId, other.projectileId)
			&& Objects.equals(this.spread, other.spread) && Objects.equals(this.count, other.count)
			&& Objects.equals(this.damage, other.damage);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.configId, this.projectileId, this.spread, this.count, this.damage);
	}
}
