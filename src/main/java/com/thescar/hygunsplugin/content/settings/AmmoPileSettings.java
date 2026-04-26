package com.thescar.hygunsplugin.content.settings;

import com.thescar.hygunsplugin.support.json.JsonValueUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AmmoPileSettings {
	private boolean generate;
	private List<AmmoPileVariant> variants = List.of();

	public AmmoPileSettings() {
	}

	public AmmoPileSettings(boolean generate, List<AmmoPileVariant> variants) {
		this.generate = generate;
		this.variants = variants != null
		                ? List.copyOf(variants)
		                : List.of();
	}

	public static AmmoPileSettings fromJson(@Nullable JsonObject root) {
		if (root == null) {
			return new AmmoPileSettings();
		}

		AmmoPileSettings settings = new AmmoPileSettings();
		settings.generate = Boolean.TRUE.equals(JsonValueUtils.Read.bool(root.get("Generate")));
		JsonElement variants = root.get("Variants");
		if (variants != null && variants.isJsonArray()) {
			List<AmmoPileVariant> parsed = new ArrayList<>();
			for (JsonElement element : variants.getAsJsonArray()) {
				if (element == null || !element.isJsonObject()) {
					continue;
				}

				AmmoPileVariant variant = AmmoPileVariant.fromJson(element.getAsJsonObject());
				if (variant != null) {
					parsed.add(variant);
				}
			}
			settings.variants = List.copyOf(parsed);
		}
		return settings;
	}

	public boolean generate() {
		return this.generate;
	}

	public void setGenerate(boolean generate) {
		this.generate = generate;
	}

	public List<AmmoPileVariant> variants() {
		return this.variants;
	}

	public void setVariants(List<AmmoPileVariant> variants) {
		this.variants = variants != null
		                ? List.copyOf(variants)
		                : List.of();
	}

	public boolean hasAnyValue() {
		return this.generate || !this.variants.isEmpty();
	}

	public boolean isEnabled() {
		return this.generate && !this.variants.isEmpty();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof AmmoPileSettings other)) {
			return false;
		}

		return this.generate == other.generate && Objects.equals(this.variants, other.variants);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.generate, this.variants);
	}
}
