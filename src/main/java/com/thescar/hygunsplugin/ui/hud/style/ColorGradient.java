package com.thescar.hygunsplugin.ui.hud.style;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Comparator;

public final class ColorGradient {
	private final GradientStop[] stops;

	public ColorGradient(GradientStop... stops) {
		if (stops == null || stops.length == 0) {
			throw new IllegalArgumentException("Gradient requires at least one stop");
		}

		this.stops = Arrays.copyOf(stops, stops.length);
		Arrays.sort(this.stops, Comparator.comparingDouble(GradientStop::position));
	}

	@Nonnull
	public static String lerpColor(@Nonnull String fromHex, @Nonnull String toHex, float t) {
		float clamped = Math.max(0.0F, Math.min(t, 1.0F));
		int fromR = Integer.parseInt(normalizeHex(fromHex).substring(1, 3), 16);
		int fromG = Integer.parseInt(normalizeHex(fromHex).substring(3, 5), 16);
		int fromB = Integer.parseInt(normalizeHex(fromHex).substring(5, 7), 16);
		int toR = Integer.parseInt(normalizeHex(toHex).substring(1, 3), 16);
		int toG = Integer.parseInt(normalizeHex(toHex).substring(3, 5), 16);
		int toB = Integer.parseInt(normalizeHex(toHex).substring(5, 7), 16);
		int r = Math.round(fromR + (toR - fromR) * clamped);
		int g = Math.round(fromG + (toG - fromG) * clamped);
		int b = Math.round(fromB + (toB - fromB) * clamped);
		return String.format("#%02X%02X%02X", r, g, b);
	}

	@Nonnull
	private static String normalizeHex(@Nonnull String value) {
		if (!value.startsWith("#") || value.length() != 7) {
			throw new IllegalArgumentException("Expected #RRGGBB color, got: " + value);
		}

		return value.toUpperCase();
	}

	@Nonnull
	public String colorAt(float value) {
		if (this.stops.length == 1) {
			return normalizeHex(this.stops[0].color());
		}

		if (value <= this.stops[0].position()) {
			return normalizeHex(this.stops[0].color());
		}

		GradientStop last = this.stops[this.stops.length - 1];
		if (value >= last.position()) {
			return normalizeHex(last.color());
		}

		for (int i = 1; i < this.stops.length; i++) {
			GradientStop right = this.stops[i];
			GradientStop left = this.stops[i - 1];
			if (value <= right.position()) {
				float segment = right.position() - left.position();
				if (segment <= 0.0F) {
					return normalizeHex(right.color());
				}

				float t = (value - left.position()) / segment;
				return lerpColor(left.color(), right.color(), t);
			}
		}

		return normalizeHex(last.color());
	}

	@Nonnull
	public String colorAt(float value, float minValue, float maxValue) {
		if (maxValue <= minValue) {
			return colorAt(minValue);
		}

		float clamped = Math.max(minValue, Math.min(value, maxValue));
		return colorAt(clamped);
	}
}
