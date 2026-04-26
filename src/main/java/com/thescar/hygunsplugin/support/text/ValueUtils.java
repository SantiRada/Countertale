package com.thescar.hygunsplugin.support.text;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

public final class ValueUtils {
	private ValueUtils() {
	}

	public static final class Numbers {
		private Numbers() {
		}

		public static float clamp(float value, float min, float max) {
			if (value <= min) {
				return min;
			}
			if (value >= max) {
				return max;
			}
			return value;
		}

		public static double clamp(double value, double min, double max) {
			if (value <= min) {
				return min;
			}
			if (value >= max) {
				return max;
			}
			return value;
		}

		public static float remap(float value, float min, float max, float newMin, float newMax) {
			if (!Float.isFinite(value) || !Float.isFinite(min) || !Float.isFinite(max) || !Float.isFinite(newMin) || !Float.isFinite(newMax)) {
				return newMin;
			}
			if (max == min) {
				return newMin;
			}
			float normalized = (value - min) / (max - min);
			return newMin + normalized * (newMax - newMin);
		}

		public static double remap(double value, double min, double max, double newMin, double newMax) {
			if (!Double.isFinite(value) || !Double.isFinite(min) || !Double.isFinite(max) || !Double.isFinite(newMin) || !Double.isFinite(newMax)) {
				return newMin;
			}
			if (max == min) {
				return newMin;
			}
			double normalized = (value - min) / (max - min);
			return newMin + normalized * (newMax - newMin);
		}

		public static float remapClamped(float value, float min, float max, float newMin, float newMax) {
			return remap(clamp(value, min, max), min, max, newMin, newMax);
		}

		public static double remapClamped(double value, double min, double max, double newMin, double newMax) {
			return remap(clamp(value, min, max), min, max, newMin, newMax);
		}
	}

	public static final class Boolean {
		private static final Set<String> TRUE_BOOLEAN_VALUES = Set.of("true", "1", "yes", "y", "on", "enabled");
		private static final Set<String> FALSE_BOOLEAN_VALUES = Set.of("false", "0", "no", "n", "off", "disabled");

		private Boolean() {
		}

		public static boolean parse(@Nullable String value) {
			String normalized = Checks.nonBlankOrNull(value);
			if (normalized == null) {
				return false;
			}

			String key = normalized.toLowerCase(Locale.ROOT);
			return TRUE_BOOLEAN_VALUES.contains(key);
		}

		public static boolean isKnown(@Nullable String value) {
			String normalized = Checks.nonBlankOrNull(value);
			if (normalized == null) {
				return false;
			}

			String key = normalized.toLowerCase(Locale.ROOT);
			return TRUE_BOOLEAN_VALUES.contains(key) || FALSE_BOOLEAN_VALUES.contains(key);
		}
	}

	public static final class Checks {
		private Checks() {
		}

		public static double positiveOrDefault(double value, double fallback) {
			if (Double.isFinite(value) && value > 0.0D) {
				return value;
			}

			return fallback;
		}

		public static double nonNegativeOrDefault(double value, double fallback) {
			if (Double.isFinite(value) && value >= 0.0D) {
				return value;
			}

			return fallback;
		}

		@Nullable
		public static String nonBlankOrNull(@Nullable String value) {
			if (value == null || value.isBlank()) {
				return null;
			}
			return value.trim();
		}
	}

	public static final class Validators {
		private Validators() {
		}

		public static boolean positiveInt(@Nullable Integer value) {
			return value != null && value > 0;
		}

		public static Predicate<Integer> minInt(int min) {
			return value -> value != null && value >= min;
		}

		public static boolean nonNegativeDouble(@Nullable Double value) {
			return value != null && Double.isFinite(value) && value >= 0.0D;
		}

		public static boolean positiveDouble(@Nullable Double value) {
			return value != null && Double.isFinite(value) && value > 0.0D;
		}

		public static boolean zeroToOneDouble(@Nullable Double value) {
			return value != null && Double.isFinite(value) && value >= 0.0D && value <= 1.0D;
		}

		public static boolean positiveFloat(@Nullable Float value) {
			return value != null && Float.isFinite(value) && value > 0.0F;
		}

		public static Predicate<Float> minFloat(float min) {
			return value -> value != null && Float.isFinite(value) && value >= min;
		}
	}
}
