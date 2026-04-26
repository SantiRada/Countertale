package com.thescar.hygunsplugin.content.settings;

import com.thescar.hygunsplugin.support.json.JsonValueUtils;
import com.thescar.hygunsplugin.support.text.StringUtil;

import com.google.gson.JsonObject;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

public final class AmmoPileVariant {
	private @Nullable String id;
	private @Nullable String model;
	private @Nullable String modelTexture;
	private @Nullable String icon;
	private @Nullable String hitboxType;
	private @Nullable String material;
	private @Nullable Float iconScale;
	private @Nullable Float scale;
	private List<String> categories = List.of();
	private int amount;

	public AmmoPileVariant() {
	}

	public AmmoPileVariant(@Nullable String id, @Nullable String model, @Nullable String modelTexture, @Nullable String icon,
	                       @Nullable String hitboxType, @Nullable String material, @Nullable Float iconScale, @Nullable Float scale,
	                       List<String> categories, int amount) {
		this.id = StringUtil.normalize(id);
		this.model = StringUtil.normalize(model);
		this.modelTexture = StringUtil.normalize(modelTexture);
		this.icon = StringUtil.normalize(icon);
		this.hitboxType = StringUtil.normalize(hitboxType);
		this.material = StringUtil.normalize(material);
		this.iconScale = iconScale;
		this.scale = scale;
		this.categories = categories != null
		                  ? List.copyOf(categories)
		                  : List.of();
		this.amount = Math.max(0, amount);
	}

	@Nullable
	public static AmmoPileVariant fromJson(@Nullable JsonObject root) {
		if (root == null) {
			return null;
		}

		AmmoPileVariant variant = new AmmoPileVariant();
		variant.id = JsonValueUtils.Read.string(root, "Id");
		variant.model = JsonValueUtils.Read.string(root, "Model");
		variant.modelTexture = JsonValueUtils.Read.string(root, "ModelTexture");
		variant.icon = JsonValueUtils.Read.string(root, "Icon");
		variant.hitboxType = JsonValueUtils.Read.string(root, "HitboxType");
		variant.material = JsonValueUtils.Read.string(root, "Material");
		variant.iconScale = JsonValueUtils.Read.nonNegativeFloat(root.get("IconScale"));
		variant.scale = JsonValueUtils.Read.nonNegativeFloat(root.get("Scale"));
		variant.categories = JsonValueUtils.Read.stringList(root, "Categories");
		Integer amount = JsonValueUtils.Read.nonNegativeInt(root.get("Amount"));
		variant.amount = amount != null
		                 ? amount
		                 : 0;
		return variant.hasRequiredValues()
		       ? variant
		       : null;
	}

	public @Nullable String id() {
		return this.id;
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

	public @Nullable String material() {
		return this.material;
	}

	public @Nullable Float iconScale() {
		return this.iconScale;
	}

	public @Nullable Float scale() {
		return this.scale;
	}

	public List<String> categories() {
		return this.categories;
	}

	public int amount() {
		return this.amount;
	}

	public boolean hasRequiredValues() {
		return this.id != null && this.model != null && this.modelTexture != null && this.amount > 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof AmmoPileVariant other)) {
			return false;
		}

		return Objects.equals(this.id, other.id) && Objects.equals(this.model, other.model)
			&& Objects.equals(this.modelTexture, other.modelTexture) && Objects.equals(this.icon, other.icon)
			&& Objects.equals(this.hitboxType, other.hitboxType) && Objects.equals(this.material, other.material)
			&& Objects.equals(this.iconScale, other.iconScale)
			&& Objects.equals(this.scale, other.scale)
			&& Objects.equals(this.categories, other.categories)
			&& this.amount == other.amount;
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			this.id, this.model, this.modelTexture, this.icon, this.hitboxType, this.material,
			this.iconScale, this.scale, this.categories, this.amount
		);
	}
}
