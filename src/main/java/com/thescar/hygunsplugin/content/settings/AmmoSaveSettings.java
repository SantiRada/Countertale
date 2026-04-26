package com.thescar.hygunsplugin.content.settings;

import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.util.Objects;

public final class AmmoSaveSettings {
	public static final AmmoSaveSettings EMPTY = new AmmoSaveSettings();
	public static final AmmoSaveSettings DEFAULTS = new AmmoSaveSettings(false, 0.0D);
	public static final SettingsGroup<AmmoSaveSettings> GROUP = createGroup();

	private @Nullable Boolean enabled;
	private @Nullable Double chance;

	public AmmoSaveSettings() {
	}

	public AmmoSaveSettings(@Nullable Boolean enabled, @Nullable Double chance) {
		this.enabled = enabled;
		this.chance = chance;
	}

	public static AmmoSaveSettings of(boolean enabled, double chance) {
		return new AmmoSaveSettings(enabled, chance);
	}

	public static AmmoSaveSettings fromJson(@Nullable JsonObject settings) {
		return GROUP.read(settings);
	}

	public static AmmoSaveSettings resolve(@Nullable AmmoSaveSettings interaction, @Nullable AmmoSaveSettings settings,
	                                       @Nullable AmmoSaveSettings defaults) {
		return GROUP.merge(interaction, settings, defaults);
	}

	private static SettingsGroup<AmmoSaveSettings> createGroup() {
		SettingsGroup<AmmoSaveSettings> group = SettingsGroup.nested("AmmoSave", AmmoSaveSettings::new);
		group.field("Enabled", CodecKind.BOOLEAN.codec(), JsonReadKind.BOOLEAN, AmmoSaveSettings::enabled, AmmoSaveSettings::setEnabled);
		group.field(
			"Chance", CodecKind.DOUBLE.codec(), JsonReadKind.DOUBLE_ZERO_TO_ONE, AmmoSaveSettings::chance,
			AmmoSaveSettings::setChance
		);
		return group;
	}

	public @Nullable Boolean enabled() {
		return this.enabled;
	}

	public void setEnabled(@Nullable Boolean enabled) {
		this.enabled = enabled;
	}

	public @Nullable Double chance() {
		return this.chance;
	}

	public void setChance(@Nullable Double chance) {
		this.chance = chance;
	}

	public boolean hasAnyValue() {
		return GROUP.hasAnyValue(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof AmmoSaveSettings other)) {
			return false;
		}

		return Objects.equals(this.enabled, other.enabled) && Objects.equals(this.chance, other.chance);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.enabled, this.chance);
	}
}
