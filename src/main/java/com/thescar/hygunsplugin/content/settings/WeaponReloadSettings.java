package com.thescar.hygunsplugin.content.settings;

import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.util.Objects;

public final class WeaponReloadSettings {
	public static final WeaponReloadSettings EMPTY = new WeaponReloadSettings();
	public static final SettingsGroup<WeaponReloadSettings> GROUP = createGroup();

	private @Nullable Double time;
	private @Nullable Integer amount;

	public WeaponReloadSettings() {
	}

	public WeaponReloadSettings(@Nullable Double time, @Nullable Integer amount) {
		this.time = time;
		this.amount = amount;
	}

	public static WeaponReloadSettings fromJson(@Nullable JsonObject settings) {
		return GROUP.read(settings);
	}

	public static WeaponReloadSettings resolve(@Nullable WeaponReloadSettings primary, @Nullable WeaponReloadSettings fallback) {
		return GROUP.merge(primary, fallback);
	}

	private static SettingsGroup<WeaponReloadSettings> createGroup() {
		SettingsGroup<WeaponReloadSettings> group = SettingsGroup.nested("Reload", WeaponReloadSettings::new);
		group.field("Time", CodecKind.DOUBLE.codec(), JsonReadKind.DOUBLE, WeaponReloadSettings::time, WeaponReloadSettings::setTime);
		group.field(
			"Amount", CodecKind.INTEGER.codec(), JsonReadKind.INTEGER_POSITIVE, WeaponReloadSettings::amount,
			WeaponReloadSettings::setAmount
		);
		return group;
	}

	public @Nullable Double time() {
		return this.time;
	}

	public void setTime(@Nullable Double time) {
		this.time = time;
	}

	public @Nullable Integer amount() {
		return this.amount;
	}

	public void setAmount(@Nullable Integer amount) {
		this.amount = amount;
	}

	public boolean hasAnyValue() {
		return GROUP.hasAnyValue(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof WeaponReloadSettings other)) {
			return false;
		}

		return Objects.equals(this.time, other.time) && Objects.equals(this.amount, other.amount);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.time, this.amount);
	}
}
