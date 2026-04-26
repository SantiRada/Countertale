package com.thescar.hygunsplugin.support.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class JsonValueUtils {
	private JsonValueUtils() {
	}

	public static final class Read {
		private Read() {
		}

		@Nullable
		public static JsonObject object(@Nullable JsonObject parent, @Nullable String key) {
			if (parent == null || key == null || key.isBlank()) {
				return null;
			}

			return asObject(parent.get(key));
		}

		@Nullable
		public static JsonObject objectCI(@Nullable JsonObject parent, @Nullable String key) {
			return asObject(elementCI(parent, key));
		}

		@Nullable
		public static JsonElement elementCI(@Nullable JsonObject obj, @Nullable String key) {
			if (obj == null || key == null || key.isBlank()) {
				return null;
			}

			for (var entry : obj.entrySet()) {
				String currentKey = entry.getKey();
				if (currentKey != null && currentKey.equalsIgnoreCase(key)) {
					return entry.getValue();
				}
			}

			return null;
		}

		@Nullable
		public static String nonBlankString(@Nullable JsonElement el) {
			if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
				return null;
			}

			String value = el.getAsString();
			if (value == null || value.isBlank()) {
				return null;
			}

			return value.trim();
		}

		@Nullable
		public static String string(@Nullable JsonObject parent, @Nullable String key) {
			return nonBlankString(parent != null
			                      ? parent.get(key)
			                      : null);
		}

		@Nullable
		public static String stringOrFirst(@Nullable JsonObject parent, @Nullable String key) {
			if (parent == null || key == null || key.isBlank()) {
				return null;
			}

			JsonElement raw = parent.get(key);
			if (raw == null) {
				return null;
			}

			if (raw.isJsonArray()) {
				for (JsonElement element : raw.getAsJsonArray()) {
					String value = nonBlankString(element);
					if (value != null) {
						return value;
					}
				}
				return null;
			}

			return nonBlankString(raw);
		}

		public static @Nonnull List<String> stringList(@Nullable JsonObject parent, @Nullable String key) {
			if (parent == null || key == null || key.isBlank()) {
				return List.of();
			}

			JsonElement raw = parent.get(key);
			if (!(raw instanceof JsonArray array)) {
				return List.of();
			}

			ArrayList<String> values = new ArrayList<>();
			for (JsonElement element : array) {
				String value = nonBlankString(element);
				if (value != null) {
					values.add(value);
				}
			}

			return List.copyOf(values);
		}

		@Nullable
		public static Boolean bool(@Nullable JsonElement el) {
			if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isBoolean()) {
				return null;
			}

			try {
				return el.getAsBoolean();
			} catch (Exception ignored) {
				return null;
			}
		}

		@Nullable
		public static Double dbl(@Nullable JsonElement el) {
			if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) {
				return null;
			}

			try {
				double value = el.getAsDouble();
				return Double.isFinite(value)
				       ? value
				       : null;
			} catch (Exception ignored) {
				return null;
			}
		}

		@Nullable
		public static Double positiveDouble(@Nullable JsonElement el) {
			Double value = dbl(el);
			if (value == null || value <= 0.0D) {
				return null;
			}

			return value;
		}

		@Nullable
		public static Double nonNegativeDouble(@Nullable JsonElement el) {
			Double value = dbl(el);
			if (value == null || value < 0.0D) {
				return null;
			}

			return value;
		}

		@Nullable
		public static Float nonNegativeFloat(@Nullable JsonElement el) {
			Double value = nonNegativeDouble(el);
			return value != null
			       ? value.floatValue()
			       : null;
		}

		@Nullable
		public static Double zeroToOneDouble(@Nullable JsonElement el) {
			Double value = dbl(el);
			if (value == null || value < 0.0D || value > 1.0D) {
				return null;
			}

			return value;
		}

		@Nullable
		public static Integer positiveInt(@Nullable JsonElement el) {
			if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) {
				return null;
			}

			try {
				int value = el.getAsInt();
				return value > 0
				       ? value
				       : null;
			} catch (Exception ignored) {
				return null;
			}
		}

		@Nullable
		public static Integer nonNegativeInt(@Nullable JsonElement el) {
			if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) {
				return null;
			}

			try {
				int value = el.getAsInt();
				return value >= 0
				       ? value
				       : null;
			} catch (Exception ignored) {
				return null;
			}
		}

		@Nullable
		private static JsonObject asObject(@Nullable JsonElement el) {
			if (el == null || !el.isJsonObject()) {
				return null;
			}

			return el.getAsJsonObject();
		}
	}

	public static final class Write {
		private Write() {
		}

		public static void putString(@Nonnull JsonObject obj, String key, @Nullable String value) {
			if (value != null) {
				obj.addProperty(key, value);
			}
		}

		public static void putBoolean(@Nonnull JsonObject obj, String key, @Nullable Boolean value) {
			if (value != null) {
				obj.addProperty(key, value);
			}
		}

		public static void putInt(@Nonnull JsonObject obj, String key, @Nullable Integer value) {
			if (value != null) {
				obj.addProperty(key, value);
			}
		}

		public static void putDouble(@Nonnull JsonObject obj, String key, @Nullable Double value) {
			if (value != null && Double.isFinite(value)) {
				obj.addProperty(key, value);
			}
		}

		public static void putObject(@Nonnull JsonObject obj, String key, @Nullable JsonObject value) {
			if (value != null) {
				obj.add(key, value);
			}
		}

		public static void putElement(@Nonnull JsonObject obj, String key, @Nullable JsonElement value) {
			if (value != null) {
				obj.add(key, value);
			}
		}
	}
}
