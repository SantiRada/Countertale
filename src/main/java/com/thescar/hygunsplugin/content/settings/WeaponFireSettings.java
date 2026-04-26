package com.thescar.hygunsplugin.content.settings;

import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.util.Objects;

public final class WeaponFireSettings {
	public static final WeaponFireSettings EMPTY = new WeaponFireSettings();
	public static final SettingsGroup<WeaponFireSettings> GROUP = createGroup();

	private @Nullable Double cooldown;

	public WeaponFireSettings() {
	}

	public WeaponFireSettings(@Nullable Double cooldown) {
		this.cooldown = cooldown;
	}

	public static WeaponFireSettings fromJson(@Nullable JsonObject settings) {
		return GROUP.read(settings);
	}

	public static WeaponFireSettings resolve(@Nullable WeaponFireSettings primary, @Nullable WeaponFireSettings fallback) {
		return GROUP.merge(primary, fallback);
	}

	private static SettingsGroup<WeaponFireSettings> createGroup() {
		SettingsGroup<WeaponFireSettings> group = SettingsGroup.nested("Fire", WeaponFireSettings::new);
		group.field(
			"Cooldown", CodecKind.DOUBLE.codec(), JsonReadKind.DOUBLE_POSITIVE, WeaponFireSettings::cooldown,
			WeaponFireSettings::setCooldown
		);
		return group;
	}

	public @Nullable Double cooldown() {
		return this.cooldown;
	}

	public void setCooldown(@Nullable Double cooldown) {
		this.cooldown = cooldown;
	}

	public boolean hasAnyValue() {
		return GROUP.hasAnyValue(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof WeaponFireSettings other)) {
			return false;
		}

		return Objects.equals(this.cooldown, other.cooldown);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.cooldown);
	}
}
