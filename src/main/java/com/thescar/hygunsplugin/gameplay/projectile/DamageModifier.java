package com.thescar.hygunsplugin.gameplay.projectile;

import com.thescar.hygunsplugin.support.json.JsonValueUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

public record DamageModifier(Type type, double value) {
	public static final DamageModifier DEFAULT = multiply(1.0D);

	public DamageModifier(Type type, double value) {
		this.type = type != null
		            ? type
		            : Type.MULTIPLY;
		this.value = Double.isFinite(value)
		             ? value
		             : defaultValue(this.type);
	}

	public static DamageModifier additive(double value) {
		return new DamageModifier(Type.ADDITIVE, value);
	}

	public static DamageModifier multiply(double value) {
		return new DamageModifier(Type.MULTIPLY, value);
	}

	public static DamageModifier constant(double value) {
		return new DamageModifier(Type.STATIC, value);
	}

	public static @Nullable DamageModifier fromJsonElement(@Nullable JsonElement element) {
		if (element == null) {
			return null;
		}

		return element.isJsonObject()
		       ? fromJsonObject(element.getAsJsonObject())
		       : null;
	}

	public static @Nullable DamageModifier fromJsonObject(@Nullable JsonObject object) {
		if (object == null) {
			return null;
		}

		Type type = Type.fromString(JsonValueUtils.Read.nonBlankString(JsonValueUtils.Read.elementCI(object, "Type")));
		Double value = JsonValueUtils.Read.dbl(JsonValueUtils.Read.elementCI(object, "Value"));
		if (type == null || value == null) {
			return null;
		}

		return new DamageModifier(type, value);
	}

	public static @Nullable DamageModifier fromRoot(@Nullable JsonObject settings, @Nonnull String key) {
		JsonObject object = JsonValueUtils.Read.object(settings, key);
		return fromJsonObject(object);
	}

	private static double defaultValue(Type type) {
		return type == Type.MULTIPLY
		       ? 1.0D
		       : 0.0D;
	}


	public int apply(int baseDamage) {
		if (baseDamage <= 0 && this.type != Type.STATIC) {
			return 0;
		}

		double result = switch (this.type) {
			case ADDITIVE -> baseDamage + this.value;
			case MULTIPLY -> baseDamage * this.value;
			case STATIC -> this.value;
		};
		return Math.max(0, (int) Math.round(result));
	}

	public boolean isDefaultMultiplier() {
		return this.type == Type.MULTIPLY && Math.abs(this.value - 1.0D) <= 1.0E-9D;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof DamageModifier(Type type1, double value1))) {
			return false;
		}

		return this.type == type1 && Double.compare(this.value, value1) == 0;
	}

	@Override
	public String toString() {
		return "DamageModifier[type=" + this.type + ", value=" + this.value + "]";
	}

	public enum Type {
		ADDITIVE,
		MULTIPLY,
		STATIC;

		static @Nullable Type fromString(@Nullable String raw) {
			if (raw == null) {
				return null;
			}

			try {
				return Type.valueOf(raw.trim().toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException ignored) {
				return null;
			}
		}
	}
}
