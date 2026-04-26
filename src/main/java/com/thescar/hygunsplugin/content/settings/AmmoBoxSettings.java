package com.thescar.hygunsplugin.content.settings;

import com.thescar.hygunsplugin.support.json.JsonValueUtils;

import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

public final class AmmoBoxSettings {
	private boolean generate;
	private List<String> categories = List.of();
	private @Nullable String model;
	private @Nullable String modelTexture;
	private @Nullable String icon;
	private @Nullable String hitboxType;
	private @Nullable Float iconScale;
	private @Nullable Float refillTime;
	private int refillAmount;

	public AmmoBoxSettings() {
	}

	@Nullable
	public static AmmoBoxSettings fromJson(@Nullable JsonObject root) {
		if (root == null) {
			return null;
		}

		AmmoBoxSettings settings = new AmmoBoxSettings();
		settings.generate = Boolean.TRUE.equals(JsonValueUtils.Read.bool(root.get("Generate")));
		settings.categories = readStringArray(root, "Categories");
		settings.model = JsonValueUtils.Read.nonBlankString(root.get("Model"));
		settings.modelTexture = JsonValueUtils.Read.nonBlankString(root.get("ModelTexture"));
		settings.icon = JsonValueUtils.Read.nonBlankString(root.get("Icon"));
		settings.hitboxType = JsonValueUtils.Read.nonBlankString(root.get("HitboxType"));
		Double iconScale = JsonValueUtils.Read.nonNegativeDouble(root.get("IconScale"));
		settings.iconScale = iconScale != null
		                     ? iconScale.floatValue()
		                     : null;
		Double refillTime = JsonValueUtils.Read.nonNegativeDouble(root.get("RefillTime"));
		settings.refillTime = refillTime != null
		                      ? refillTime.floatValue()
		                      : null;
		Integer refillAmount = JsonValueUtils.Read.nonNegativeInt(root.get("RefillAmount"));
		settings.refillAmount = refillAmount != null
		                        ? refillAmount
		                        : 0;
		return settings.hasAnyValue()
		       ? settings
		       : null;
	}

	private static List<String> readStringArray(@Nullable JsonObject root, String key) {
		return JsonValueUtils.Read.stringList(root, key);
	}

	@Nullable
	public boolean generate() {
		return this.generate;
	}

	public List<String> categories() {
		return this.categories;
	}

	public @Nullable String model() {
		return this.model;
	}

	public @Nullable String modelTexture() {
		return this.modelTexture;
	}

	public @Nullable String icon() {
		return this.icon;
	}

	public @Nullable String hitboxType() {
		return this.hitboxType;
	}

	public @Nullable Float iconScale() {
		return this.iconScale;
	}

	public @Nullable Float refillTime() {
		return this.refillTime;
	}

	public int refillAmount() {
		return this.refillAmount;
	}

	public boolean hasAnyValue() {
		return this.generate || !this.categories.isEmpty() || this.model != null || this.modelTexture != null || this.icon != null
			|| this.hitboxType != null || this.iconScale != null || this.refillTime != null || this.refillAmount > 0;
	}

	public boolean isEnabled() {
		return this.generate && this.model != null && this.modelTexture != null && this.refillAmount > 0
			&& this.refillTime != null && this.refillTime > 0.0F;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof AmmoBoxSettings other)) {
			return false;
		}

		return this.generate == other.generate && Objects.equals(this.categories, other.categories)
			&& Objects.equals(this.model, other.model) && Objects.equals(this.modelTexture, other.modelTexture)
			&& Objects.equals(this.icon, other.icon) && Objects.equals(this.hitboxType, other.hitboxType)
			&& Objects.equals(this.iconScale, other.iconScale)
			&& Objects.equals(this.refillTime, other.refillTime)
			&& this.refillAmount == other.refillAmount;
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			this.generate, this.categories, this.model, this.modelTexture, this.icon, this.hitboxType,
			this.iconScale, this.refillTime, this.refillAmount
		);
	}
}
