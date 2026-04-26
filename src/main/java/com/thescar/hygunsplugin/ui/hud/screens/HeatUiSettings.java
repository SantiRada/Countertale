package com.thescar.hygunsplugin.ui.hud.screens;

import com.thescar.hygunsplugin.support.text.ValueUtils;
import com.thescar.hygunsplugin.ui.hud.style.ColorGradient;
import com.thescar.hygunsplugin.ui.hud.style.GradientStop;
import com.thescar.hygunsplugin.ui.hud.style.Gradients;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonValue;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public record HeatUiSettings(
	float rootLeft,
	float rootTop,
	float width,
	float height,
	float valueMin,
	float valueMax,
	String heatTexturePath,
	float heatLeft,
	float heatTop,
	String outlineTexturePath,
	float outlineLeft,
	float outlineTop,
	String overlayTexturePath,
	float overlayLeft,
	float overlayTop,
	ColorGradient gradient
) {
	private static final String DEFAULT_BAR_PATH = "Bars/HalfCircular";
	private static final String MASK_FILE_NAME = "Mask.png";
	private static final String OUTLINE_FILE_NAME = "Outline.png";
	private static final String OVERLAY_FILE_NAME = "Overlay.png";

	private static final HeatUiSettings DEFAULT = new HeatUiSettings(
		0.0F, 0.0F, 128.0F, 128.0F, 0.25F, 0.75F,
		barTexture(DEFAULT_BAR_PATH, MASK_FILE_NAME), 0.0F, 0.0F,
		barTexture(DEFAULT_BAR_PATH, OUTLINE_FILE_NAME), 0.0F, 0.0F,
		barTexture(DEFAULT_BAR_PATH, OVERLAY_FILE_NAME), 0.0F, 0.0F,
		Gradients.OVERHEAT
	);

	public static HeatUiSettings defaults() {
		return DEFAULT;
	}

	public static HeatUiSettings fromBson(@Nullable BsonDocument raw) {
		if (raw == null || raw.isEmpty()) {
			return DEFAULT;
		}

		float[] rootOffset = readFloatPair(raw.get("Offset"), DEFAULT.rootLeft, DEFAULT.rootTop);
		float[] size = readSize(raw.get("Size"), DEFAULT.width, DEFAULT.height);
		float[] valueRange = readFloatPair(raw.get("ValueRange"), DEFAULT.valueMin, DEFAULT.valueMax);
		BsonDocument heat = document(raw.get("Heat"));
		BsonDocument outline = document(raw.get("HeatOutline"));
		BsonDocument overlay = document(raw.get("HeatOverlay"));
		float[] heatOffset = readFloatPair(value(heat, "Offset"), DEFAULT.heatLeft, DEFAULT.heatTop);
		float[] outlineOffset = readFloatPair(value(outline, "Offset"), DEFAULT.outlineLeft, DEFAULT.outlineTop);
		float[] overlayOffset = readFloatPair(value(overlay, "Offset"), DEFAULT.overlayLeft, DEFAULT.overlayTop);
		String barPath = normalizeBarPath(string(first(raw.get("Bar"), raw.get("BarPath")), DEFAULT_BAR_PATH));
		String heatTexturePath = string(value(heat, "TexturePath"), barTexture(barPath, MASK_FILE_NAME));
		String outlineTexturePath = string(value(outline, "TexturePath"), barTexture(barPath, OUTLINE_FILE_NAME));
		String overlayTexturePath = string(value(overlay, "TexturePath"), barTexture(barPath, OVERLAY_FILE_NAME));

		return new HeatUiSettings(
			rootOffset[0],
			rootOffset[1],
			size[0],
			size[1],
			valueRange[0],
			valueRange[1],
			heatTexturePath,
			heatOffset[0],
			heatOffset[1],
			outlineTexturePath,
			outlineOffset[0],
			outlineOffset[1],
			overlayTexturePath,
			overlayOffset[0],
			overlayOffset[1],
			gradient(raw.get("Gradient"), DEFAULT.gradient)
		);
	}

	private static BsonValue value(@Nullable BsonDocument document, String key) {
		return document != null
		       ? document.get(key)
		       : null;
	}

	@Nullable
	private static BsonValue first(@Nullable BsonValue primary, @Nullable BsonValue fallback) {
		return primary != null
		       ? primary
		       : fallback;
	}

	@Nullable
	private static BsonDocument document(@Nullable BsonValue value) {
		return value != null && value.isDocument()
		       ? value.asDocument()
		       : null;
	}

	private static String string(@Nullable BsonValue value, String fallback) {
		if (value == null || !value.isString()) {
			return fallback;
		}
		String text = value.asString().getValue();
		return text == null || text.isBlank()
		       ? fallback
		       : text.trim();
	}

	private static String normalizeBarPath(String value) {
		if (value == null || value.isBlank()) {
			return DEFAULT_BAR_PATH;
		}
		String path = value.trim().replace('\\', '/');
		while (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		return path.isBlank()
		       ? DEFAULT_BAR_PATH
		       : path;
	}

	private static String barTexture(String barPath, String fileName) {
		return normalizeBarPath(barPath) + "/" + fileName;
	}

	private static float positiveFloat(@Nullable BsonValue value, float fallback) {
		Float parsed = number(value);
		return parsed != null && parsed > 0.0F
		       ? parsed
		       : fallback;
	}

	private static float[] readFloatPair(@Nullable BsonValue value, float fallbackLeft, float fallbackTop) {
		if (!(value instanceof BsonArray array) || array.size() < 2) {
			return new float[] {fallbackLeft, fallbackTop};
		}

		Float left = number(array.get(0));
		Float top = number(array.get(1));
		return new float[] {
			left != null ? left : fallbackLeft,
			top != null ? top : fallbackTop
		};
	}

	private static float[] readSize(@Nullable BsonValue value, float fallbackWidth, float fallbackHeight) {
		if (value != null && value.isNumber()) {
			float size = positiveFloat(value, fallbackWidth);
			return new float[] {size, size};
		}
		return readFloatPair(value, fallbackWidth, fallbackHeight);
	}

	@Nullable
	private static Float number(@Nullable BsonValue value) {
		if (value == null || !value.isNumber()) {
			return null;
		}

		double number = value.asNumber().doubleValue();
		return Double.isFinite(number)
		       ? (float) number
		       : null;
	}

	private static ColorGradient gradient(@Nullable BsonValue value, ColorGradient fallback) {
		if (value == null) {
			return fallback;
		}

		if (value.isArray()) {
			return arrayGradient(value.asArray(), fallback);
		}

		if (value.isDocument()) {
			return objectGradient(value.asDocument(), fallback);
		}

		return fallback;
	}

	private static ColorGradient arrayGradient(BsonArray array, ColorGradient fallback) {
		List<String> colors = new ArrayList<>();
		for (BsonValue element : array) {
			String color = string(element, null);
			if (color != null) {
				colors.add(color);
			}
		}
		if (colors.isEmpty()) {
			return fallback;
		}
		if (colors.size() == 1) {
			return safeGradient(fallback, new GradientStop(0.0F, colors.get(0)));
		}

		List<GradientStop> stops = new ArrayList<>();
		float denominator = colors.size() - 1.0F;
		for (int i = 0; i < colors.size(); i++) {
			stops.add(new GradientStop(i / denominator, colors.get(i)));
		}
		return safeGradient(fallback, stops.toArray(GradientStop[]::new));
	}

	private static ColorGradient objectGradient(BsonDocument document, ColorGradient fallback) {
		List<GradientStop> stops = new ArrayList<>();
		for (Map.Entry<String, BsonValue> entry : document.entrySet()) {
			if (entry == null) {
				continue;
			}
			float position;
			try {
				position = Float.parseFloat(entry.getKey());
			} catch (Exception ignored) {
				continue;
			}

			String color = string(entry.getValue(), null);
			if (color == null) {
				continue;
			}
			stops.add(new GradientStop(ValueUtils.Numbers.clamp(position, 0.0F, 1.0F), color));
		}

		if (stops.isEmpty()) {
			return fallback;
		}
		stops.sort(Comparator.comparingDouble(GradientStop::position));
		return safeGradient(fallback, stops.toArray(GradientStop[]::new));
	}

	private static ColorGradient safeGradient(ColorGradient fallback, GradientStop... stops) {
		try {
			ColorGradient gradient = new ColorGradient(stops);
			gradient.colorAt(0.5F);
			return gradient;
		} catch (Exception ignored) {
			return fallback;
		}
	}
}
