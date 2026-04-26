package com.thescar.hygunsplugin.content.settings;

import com.google.gson.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public final class AutoGuidanceSettings {
	public static final AutoGuidanceSettings EMPTY = new AutoGuidanceSettings();
	public static final AutoGuidanceSettings DEFAULTS = new AutoGuidanceSettings(false, false, 25.0D, 80.0D, 240.0D, null);
	private static final String KEY = "AutoGuidance";
	public static final SettingsGroup<AutoGuidanceSettings> GROUP = createGroup();
	private static final String LEGACY_KEY = "AmmoGuidance";
	private @Nullable Boolean enabled;
	private @Nullable Boolean affectsPlayers;
	private @Nullable Double coneDegrees;
	private @Nullable Double maxDistance;
	private @Nullable Double turnRate;
	private @Nullable String effectId;

	public AutoGuidanceSettings() {
	}

	public AutoGuidanceSettings(@Nullable Boolean enabled, @Nullable Boolean affectsPlayers, @Nullable Double coneDegrees,
	                            @Nullable Double maxDistance, @Nullable Double turnRate, @Nullable String effectId) {
		this.enabled = enabled;
		this.affectsPlayers = affectsPlayers;
		this.coneDegrees = coneDegrees;
		this.maxDistance = maxDistance;
		this.turnRate = turnRate;
		this.effectId = normalizeEffectId(effectId);
	}

	public static AutoGuidanceSettings of(boolean enabled, double coneDegrees, double maxDistance, boolean affectsPlayers,
	                                      double turnRate) {
		return new AutoGuidanceSettings(enabled, affectsPlayers, coneDegrees, maxDistance, turnRate, null);
	}

	public static AutoGuidanceSettings fromJson(@Nullable JsonObject settings) {
		AutoGuidanceSettings parsed = GROUP.read(settings);
		if (parsed.hasAnyValue()) {
			return parsed;
		}

		JsonObject legacy = settings != null
		                    ? settings.getAsJsonObject(LEGACY_KEY)
		                    : null;
		return legacy != null
		       ? GROUP.read(wrap(KEY, legacy))
		       : parsed;
	}

	public static AutoGuidanceSettings resolve(@Nullable AutoGuidanceSettings interaction, @Nullable AutoGuidanceSettings settings,
	                                           @Nullable AutoGuidanceSettings defaults) {
		return GROUP.merge(interaction, settings, defaults);
	}

	private static SettingsGroup<AutoGuidanceSettings> createGroup() {
		SettingsGroup<AutoGuidanceSettings> group = SettingsGroup.nested(KEY, AutoGuidanceSettings::new);
		group.field(
			"Enabled", CodecKind.BOOLEAN.codec(), JsonReadKind.BOOLEAN, AutoGuidanceSettings::enabled,
			AutoGuidanceSettings::setEnabled
		);
		group.field(
			"AffectsPlayers", CodecKind.BOOLEAN.codec(), JsonReadKind.BOOLEAN, AutoGuidanceSettings::affectsPlayers,
			AutoGuidanceSettings::setAffectsPlayers
		);
		group.field(
			"ConeDegrees", CodecKind.DOUBLE.codec(), JsonReadKind.DOUBLE_POSITIVE, AutoGuidanceSettings::coneDegrees,
			AutoGuidanceSettings::setConeDegrees
		);
		group.field(
			"MaxDistance", CodecKind.DOUBLE.codec(), JsonReadKind.DOUBLE_POSITIVE, AutoGuidanceSettings::maxDistance,
			AutoGuidanceSettings::setMaxDistance
		);
		group.field(
			"TurnRate", CodecKind.DOUBLE.codec(), JsonReadKind.DOUBLE_POSITIVE, AutoGuidanceSettings::turnRate,
			AutoGuidanceSettings::setTurnRate
		);
		group.field(
			"EffectId", CodecKind.STRING.codec(), JsonReadKind.STRING_NON_BLANK, AutoGuidanceSettings::effectId,
			AutoGuidanceSettings::setEffectId
		);
		return group;
	}

	private static @Nullable String normalizeEffectId(@Nullable String effectId) {
		if (effectId == null) {
			return null;
		}

		String trimmed = effectId.trim();
		return trimmed.isEmpty()
		       ? null
		       : trimmed;
	}

	private static JsonObject wrap(@Nonnull String key, @Nonnull JsonObject value) {
		JsonObject root = new JsonObject();
		root.add(key, value);
		return root;
	}

	public @Nullable Boolean enabled() {
		return this.enabled;
	}

	public void setEnabled(@Nullable Boolean enabled) {
		this.enabled = enabled;
	}

	public @Nullable Boolean affectsPlayers() {
		return this.affectsPlayers;
	}

	public void setAffectsPlayers(@Nullable Boolean affectsPlayers) {
		this.affectsPlayers = affectsPlayers;
	}

	public @Nullable Double coneDegrees() {
		return this.coneDegrees;
	}

	public void setConeDegrees(@Nullable Double coneDegrees) {
		this.coneDegrees = coneDegrees;
	}

	public @Nullable Double maxDistance() {
		return this.maxDistance;
	}

	public void setMaxDistance(@Nullable Double maxDistance) {
		this.maxDistance = maxDistance;
	}

	public @Nullable Double turnRate() {
		return this.turnRate;
	}

	public void setTurnRate(@Nullable Double turnRate) {
		this.turnRate = turnRate;
	}

	public @Nullable String effectId() {
		return this.effectId;
	}

	public void setEffectId(@Nullable String effectId) {
		this.effectId = normalizeEffectId(effectId);
	}

	public boolean hasAnyValue() {
		return GROUP.hasAnyValue(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof AutoGuidanceSettings other)) {
			return false;
		}

		return Objects.equals(this.enabled, other.enabled) && Objects.equals(this.affectsPlayers, other.affectsPlayers)
			&& Objects.equals(this.coneDegrees, other.coneDegrees) && Objects.equals(this.maxDistance, other.maxDistance)
			&& Objects.equals(this.turnRate, other.turnRate) && Objects.equals(this.effectId, other.effectId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.enabled, this.affectsPlayers, this.coneDegrees, this.maxDistance, this.turnRate, this.effectId);
	}
}
