package com.thescar.hygunsplugin.content.settings;

import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.util.Objects;

public final class WallPenetrationSettings {
	public static final WallPenetrationSettings EMPTY = new WallPenetrationSettings();
	public static final WallPenetrationSettings DEFAULTS = new WallPenetrationSettings(false, 1.0D, 0.0D, 0.5D);
	public static final SettingsGroup<WallPenetrationSettings> GROUP = createGroup();

	private @Nullable Boolean canPenetrateWalls;
	private @Nullable Double wallPenetrationBlocks;
	private @Nullable Double damageReductionModifier;
	private @Nullable Double damageReductionDistance;

	public WallPenetrationSettings() {
	}

	public WallPenetrationSettings(@Nullable Boolean canPenetrateWalls, @Nullable Double wallPenetrationBlocks,
	                               @Nullable Double damageReductionModifier, @Nullable Double damageReductionDistance) {
		this.canPenetrateWalls = canPenetrateWalls;
		this.wallPenetrationBlocks = wallPenetrationBlocks;
		this.damageReductionModifier = damageReductionModifier;
		this.damageReductionDistance = damageReductionDistance;
	}

	public static WallPenetrationSettings of(boolean canPenetrateWalls, double wallPenetrationBlocks, double damageReductionModifier,
	                                         double damageReductionDistance) {
		return new WallPenetrationSettings(canPenetrateWalls, wallPenetrationBlocks, damageReductionModifier, damageReductionDistance);
	}

	public static WallPenetrationSettings fromJson(@Nullable JsonObject settings) {
		return GROUP.read(settings);
	}

	public static WallPenetrationSettings resolve(@Nullable WallPenetrationSettings interaction, @Nullable WallPenetrationSettings settings,
	                                              @Nullable WallPenetrationSettings defaults) {
		return GROUP.merge(interaction, settings, defaults);
	}

	private static SettingsGroup<WallPenetrationSettings> createGroup() {
		SettingsGroup<WallPenetrationSettings> group = SettingsGroup.nested("WallPenetration", WallPenetrationSettings::new);
		group.field(
			"Enabled", CodecKind.BOOLEAN.codec(), JsonReadKind.BOOLEAN, WallPenetrationSettings::canPenetrateWalls,
			WallPenetrationSettings::setCanPenetrateWalls
		);
		group.field(
			"Blocks", CodecKind.DOUBLE.codec(), JsonReadKind.DOUBLE_POSITIVE, WallPenetrationSettings::wallPenetrationBlocks,
			WallPenetrationSettings::setWallPenetrationBlocks
		);
		group.field(
			"DamageReductionModifier", CodecKind.DOUBLE.codec(), JsonReadKind.DOUBLE_NON_NEGATIVE,
			WallPenetrationSettings::damageReductionModifier, WallPenetrationSettings::setDamageReductionModifier
		);
		group.field(
			"DamageReductionDistance", CodecKind.DOUBLE.codec(), JsonReadKind.DOUBLE_POSITIVE,
			WallPenetrationSettings::damageReductionDistance, WallPenetrationSettings::setDamageReductionDistance
		);
		return group;
	}

	public @Nullable Boolean canPenetrateWalls() {
		return this.canPenetrateWalls;
	}

	public void setCanPenetrateWalls(@Nullable Boolean canPenetrateWalls) {
		this.canPenetrateWalls = canPenetrateWalls;
	}

	public @Nullable Double wallPenetrationBlocks() {
		return this.wallPenetrationBlocks;
	}

	public void setWallPenetrationBlocks(@Nullable Double wallPenetrationBlocks) {
		this.wallPenetrationBlocks = wallPenetrationBlocks;
	}

	public @Nullable Double damageReductionModifier() {
		return this.damageReductionModifier;
	}

	public void setDamageReductionModifier(@Nullable Double damageReductionModifier) {
		this.damageReductionModifier = damageReductionModifier;
	}

	public @Nullable Double damageReductionDistance() {
		return this.damageReductionDistance;
	}

	public void setDamageReductionDistance(@Nullable Double damageReductionDistance) {
		this.damageReductionDistance = damageReductionDistance;
	}

	public boolean hasAnyValue() {
		return GROUP.hasAnyValue(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof WallPenetrationSettings other)) {
			return false;
		}

		return Objects.equals(this.canPenetrateWalls, other.canPenetrateWalls)
			&& Objects.equals(this.wallPenetrationBlocks, other.wallPenetrationBlocks)
			&& Objects.equals(this.damageReductionModifier, other.damageReductionModifier)
			&& Objects.equals(this.damageReductionDistance, other.damageReductionDistance);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.canPenetrateWalls, this.wallPenetrationBlocks, this.damageReductionModifier, this.damageReductionDistance);
	}
}
