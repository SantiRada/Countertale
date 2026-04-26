package com.thescar.hygunsplugin.content.particles;

import com.thescar.hygunsplugin.content.registry.ItemAssetScanner;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.Color;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.stream.Stream;

public final class ParticleInteractionPaletteRegistry {
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	private static final String INTERACTIONS_PREFIX_A = "Server/Item/Interactions/";
	private static final String INTERACTIONS_PREFIX_B = "resources/Server/Item/Interactions/";
	private static volatile Map<String, InteractionPalette> PALETTES = Map.of();
	@Nullable
	private static volatile Path LAST_PLUGIN_PATH;

	private ParticleInteractionPaletteRegistry() {
	}

	public static void load(@Nullable Path pluginPath) {
		LAST_PLUGIN_PATH = pluginPath;
		LinkedHashMap<String, InteractionPalette> loaded = new LinkedHashMap<>();
		try {
			if (pluginPath != null && Files.isDirectory(pluginPath)) {
				scanDirectory(pluginPath, loaded);
			} else if (pluginPath != null && Files.isRegularFile(pluginPath)) {
				scanZip(pluginPath, loaded);
			}
		} catch (Exception t) {
			LOGGER.atWarning().log("Interaction color var load failed for plugin path %s: %s", pluginPath, t);
		}

		try {
			ItemAssetScanner.forEachRuntimeModRoot(
				pluginPath, entry -> {
					try {
						if (Files.isDirectory(entry)) {
							scanDirectory(entry, loaded);
							return;
						}

						String name = entry.getFileName() != null
						              ? entry.getFileName().toString().toLowerCase(Locale.ROOT)
						              : "";
						if (Files.isRegularFile(entry) && (name.endsWith(".jar") || name.endsWith(".zip"))) {
							scanZip(entry, loaded);
						}
					} catch (Exception ignored) {
						// Ignore one bad mod and continue loading the rest.
					}
				}
			);
		} catch (Exception t) {
			LOGGER.atWarning().log("Interaction color var runtime scan failed: %s", t);
		}

		PALETTES = Map.copyOf(loaded);
		LOGGER.atInfo().log("Loaded %d interaction palette set(s)", PALETTES.size());
	}

	public static void refresh() {
		load(LAST_PLUGIN_PATH);
	}

	@Nullable
	public static Map<String, Color> get(@Nullable String interactionId, boolean firstPerson, int particleIndex) {
		InteractionPalette palette = ItemAssetScanner.lookupWithVariants(PALETTES, interactionId);
		if (palette == null) {
			return null;
		}
		Map<String, Color> colors = (firstPerson
		                             ? palette.firstPersonParticles()
		                             : palette.particles()).get(particleIndex);
		return colors == null || colors.isEmpty()
		       ? null
		       : colors;
	}

	private static void scanDirectory(Path root, Map<String, InteractionPalette> out) throws Exception {
		scanInteractionsDirectory(root.resolve("Server").resolve("Item").resolve("Interactions"), out);
		scanInteractionsDirectory(
			root
				.resolve("resources")
				.resolve("Server")
				.resolve("Item")
				.resolve("Interactions"), out
		);
	}

	private static void scanInteractionsDirectory(Path dir, Map<String, InteractionPalette> out) throws Exception {
		if (!Files.isDirectory(dir)) {
			return;
		}
		try (Stream<Path> stream = Files.walk(dir)) {
			for (Path path : (Iterable<Path>) stream
				.filter(file -> Files.isRegularFile(file) && file.toString().endsWith(".json"))::iterator) {
				String interactionId = ItemAssetScanner.toItemId(dir, path);
				parseAndPut(interactionId, path.toString(), Files.readString(path, StandardCharsets.UTF_8), out);
			}
		}
	}

	private static void scanZip(Path zipPath, Map<String, InteractionPalette> out) throws Exception {
		try (ZipFile zip = new ZipFile(zipPath.toFile())) {
			var entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry.isDirectory() || !entry.getName().endsWith(".json") || !isInteractionPath(entry.getName())) {
					continue;
				}
				String interactionId = interactionId(entry.getName());
				try (InputStream in = zip.getInputStream(entry)) {
					parseAndPut(interactionId, entry.getName(), new String(in.readAllBytes(), StandardCharsets.UTF_8), out);
				}
			}
		}
	}

	private static boolean isInteractionPath(String path) {
		return path.startsWith(INTERACTIONS_PREFIX_A) || path.startsWith(INTERACTIONS_PREFIX_B);
	}

	private static String interactionId(String path) {
		String normalized = path.replace('\\', '/');
		String prefix = normalized.startsWith(INTERACTIONS_PREFIX_A)
		                ? INTERACTIONS_PREFIX_A
		                : INTERACTIONS_PREFIX_B;
		return ItemAssetScanner.toItemId(normalized.substring(prefix.length(), normalized.length() - ".json".length()));
	}

	private static void parseAndPut(String interactionId, String sourceName, String json, Map<String, InteractionPalette> out) {
		JsonElement root;
		try {
			root = JsonParser.parseString(json);
		} catch (JsonParseException e) {
			LOGGER.atWarning().log("Invalid interaction color var json in %s: %s", sourceName, e.getMessage());
			return;
		}
		if (!root.isJsonObject()) {
			return;
		}
		JsonElement effectsElement = root.getAsJsonObject().get("Effects");
		if (effectsElement == null || !effectsElement.isJsonObject()) {
			return;
		}

		Map<Integer, Map<String, Color>> particles = parseParticles(effectsElement.getAsJsonObject().get("Particles"));
		Map<Integer, Map<String, Color>> firstPersonParticles = parseParticles(effectsElement
			.getAsJsonObject()
			.get("FirstPersonParticles"));
		if (particles.isEmpty() && firstPersonParticles.isEmpty()) {
			return;
		}
		out.put(interactionId, new InteractionPalette(Map.copyOf(particles), Map.copyOf(firstPersonParticles)));
	}

	private static Map<Integer, Map<String, Color>> parseParticles(@Nullable JsonElement particlesElement) {
		if (particlesElement == null || !particlesElement.isJsonArray()) {
			return Map.of();
		}
		Map<Integer, Map<String, Color>> out = new HashMap<>();
		for (int i = 0; i < particlesElement.getAsJsonArray().size(); i++) {
			JsonElement particleElement = particlesElement.getAsJsonArray().get(i);
			if (particleElement == null || !particleElement.isJsonObject()) {
				continue;
			}
			JsonElement paletteElement = particleElement.getAsJsonObject().get("Palette");
			if (paletteElement == null || !paletteElement.isJsonObject()) {
				continue;
			}
			Map<String, Color> colors = parseColors(paletteElement.getAsJsonObject());
			if (!colors.isEmpty()) {
				out.put(i, Map.copyOf(colors));
			}
		}
		return out;
	}

	private static Map<String, Color> parseColors(JsonObject object) {
		Map<String, Color> colors = new HashMap<>();
		for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
			Color color = parseColor(entry.getValue());
			if (entry.getKey() != null && !entry.getKey().isBlank() && color != null) {
				colors.put(entry.getKey().trim(), color);
			}
		}
		return colors;
	}

	@Nullable
	private static Color parseColor(@Nullable JsonElement value) {
		if (value == null || !value.isJsonPrimitive()) {
			return null;
		}
		try {
			String text = value.getAsString();
			if (text == null) {
				return null;
			}
			String hex = text.trim();
			if (hex.startsWith("#")) {
				hex = hex.substring(1);
			}
			if (hex.length() != 6) {
				return null;
			}
			int rgb = Integer.parseInt(hex, 16);
			return new Color((byte) ((rgb >> 16) & 0xFF), (byte) ((rgb >> 8) & 0xFF), (byte) (rgb & 0xFF));
		} catch (Exception ignored) {
			return null;
		}
	}

	private record InteractionPalette(
		Map<Integer, Map<String, Color>> particles,
		Map<Integer, Map<String, Color>> firstPersonParticles
	) {
	}
}
