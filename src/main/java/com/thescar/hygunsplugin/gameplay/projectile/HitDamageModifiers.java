package com.thescar.hygunsplugin.gameplay.projectile;

import com.thescar.hygunsplugin.support.json.JsonValueUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

public record HitDamageModifiers(
	@Nullable DamageModifier head, @Nullable DamageModifier body, @Nullable DamageModifier fallback
) {
	public static final HitDamageModifiers DEFAULT = new HitDamageModifiers(
		DamageModifier.multiply(1.5D), DamageModifier.multiply(1.0D),
		DamageModifier.multiply(1.0D)
	);

	public static HitDamageModifiers fromNullable(@Nullable DamageModifier head, @Nullable DamageModifier body,
	                                              @Nullable DamageModifier fallback) {
		return new HitDamageModifiers(
			head != null
			? head
			: DEFAULT.head, body != null
			                ? body
			                : DEFAULT.body,
			fallback != null
			? fallback
			: DEFAULT.fallback
		);
	}

	public static HitDamageModifiers fromJson(@Nullable JsonObject settings) {
		if (settings == null) {
			return DEFAULT;
		}

		JsonObject damageModifiers = JsonValueUtils.Read.object(settings, "DamageModifiers");
		if (damageModifiers == null || !hasHitSpecificModifiers(damageModifiers)) {
			return DEFAULT;
		}

		return fromObject(damageModifiers);
	}

	private static HitDamageModifiers fromObject(@Nonnull JsonObject obj) {
		return fromNullable(readModifier(obj, "Head"), readModifier(obj, "Body"), fallbackModifier(obj));
	}

	private static boolean hasHitSpecificModifiers(@Nonnull JsonObject obj) {
		return obj.has("Head") || obj.has("Body") || obj.has("*") || JsonValueUtils.Read.elementCI(obj, "Fallback") != null;
	}

	private static @Nullable DamageModifier readModifier(@Nonnull JsonObject obj, @Nonnull String key) {
		JsonElement element = JsonValueUtils.Read.elementCI(obj, key);
		return element != null && element.isJsonObject()
		       ? DamageModifier.fromJsonObject(element.getAsJsonObject())
		       : null;
	}

	private static @Nullable DamageModifier fallbackModifier(@Nonnull JsonObject obj) {
		JsonElement wildcard = obj.get("*");
		if (wildcard != null && wildcard.isJsonObject()) {
			DamageModifier modifier = DamageModifier.fromJsonObject(wildcard.getAsJsonObject());
			if (modifier != null) {
				return modifier;
			}
		}

		return readModifier(obj, "Fallback");
	}

	public int apply(int baseDamage, @Nullable String collisionDetail) {
		DamageModifier modifier = resolveModifier(collisionDetail);
		return modifier.apply(baseDamage);
	}

	private DamageModifier resolveModifier(@Nullable String collisionDetail) {
		if (collisionDetail == null || collisionDetail.isBlank()) {
			return this.fallback != null
			       ? this.fallback
			       : DEFAULT.fallback;
		}

		String detail = collisionDetail.trim().toLowerCase(Locale.ROOT);
		if (detail.contains("head")) {
			return this.head != null
			       ? this.head
			       : DEFAULT.head;
		}

		if (detail.contains("body") || detail.contains("torso") || detail.contains("chest")) {
			return this.body != null
			       ? this.body
			       : DEFAULT.body;
		}

		return this.fallback != null
		       ? this.fallback
		       : DEFAULT.fallback;
	}
}
