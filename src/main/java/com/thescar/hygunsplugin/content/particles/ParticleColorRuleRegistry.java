package com.thescar.hygunsplugin.content.particles;

import com.thescar.hygunsplugin.content.registry.ItemAssetScanner;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.stream.Stream;

public final class ParticleColorRuleRegistry {
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	private static final String RULES_FOLDER = "ColorRules";
	private static final String RULES_PREFIX_A = "Server/Particles/" + RULES_FOLDER + "/";
	private static final String RULES_PREFIX_B = "resources/Server/Particles/" + RULES_FOLDER + "/";
	private static volatile Map<String, SpawnerRules> RULES = Map.of();
	@Nullable
	private static volatile Path LAST_PLUGIN_PATH;

	private ParticleColorRuleRegistry() {
	}

	public static void load(@Nullable Path pluginPath) {
		LAST_PLUGIN_PATH = pluginPath;
		LinkedHashMap<String, SpawnerRules> loaded = new LinkedHashMap<>();
		try {
			if (pluginPath != null && Files.isDirectory(pluginPath)) {
				scanDirectory(pluginPath, loaded);
			} else if (pluginPath != null && Files.isRegularFile(pluginPath)) {
				scanZip(pluginPath, loaded);
			}
		} catch (Exception t) {
			LOGGER.atWarning().log("Particle color rule load failed for plugin path %s: %s", pluginPath, t);
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
			LOGGER.atWarning().log("Particle color rule runtime scan failed: %s", t);
		}

		RULES = Map.copyOf(loaded);
		LOGGER.atInfo().log("Loaded %d particle color rule set(s)", RULES.size());
	}

	public static void refresh() {
		load(LAST_PLUGIN_PATH);
	}

	@Nullable
	public static SpawnerRules get(@Nullable String spawnerId) {
		if (spawnerId == null || spawnerId.isBlank()) {
			return null;
		}
		return ItemAssetScanner.lookupWithVariants(RULES, spawnerId);
	}

	public static int size() {
		return RULES.size();
	}

	public static List<String> spawnerIds() {
		return RULES.keySet().stream().sorted().toList();
	}

	private static void scanDirectory(Path root, Map<String, SpawnerRules> out) throws Exception {
		scanRulesDirectory(root.resolve("Server").resolve("Particles").resolve(RULES_FOLDER), out);
		scanRulesDirectory(
			root
				.resolve("resources")
				.resolve("Server")
				.resolve("Particles")
				.resolve(RULES_FOLDER), out
		);
	}

	private static void scanRulesDirectory(Path dir, Map<String, SpawnerRules> out) throws Exception {
		if (!Files.isDirectory(dir)) {
			return;
		}
		try (Stream<Path> stream = Files.walk(dir)) {
			for (Path path : (Iterable<Path>) stream
				.filter(file -> Files.isRegularFile(file) && file.toString().endsWith(".json"))::iterator) {
				parseAndPut(path.toString(), Files.readString(path, StandardCharsets.UTF_8), out);
			}
		}
	}

	private static void scanZip(Path zipPath, Map<String, SpawnerRules> out) throws Exception {
		try (ZipFile zip = new ZipFile(zipPath.toFile())) {
			var entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry.isDirectory() || !entry.getName().endsWith(".json") || !isRulePath(entry.getName())) {
					continue;
				}
				try (InputStream in = zip.getInputStream(entry)) {
					parseAndPut(entry.getName(), new String(in.readAllBytes(), StandardCharsets.UTF_8), out);
				}
			}
		}
	}

	private static boolean isRulePath(String path) {
		return path.startsWith(RULES_PREFIX_A) || path.startsWith(RULES_PREFIX_B);
	}

	private static void parseAndPut(String sourceName, String json, Map<String, SpawnerRules> out) {
		JsonElement root;
		try {
			root = JsonParser.parseString(json);
		} catch (JsonParseException e) {
			LOGGER.atWarning().log("Invalid particle color rule json in %s: %s", sourceName, e.getMessage());
			return;
		}
		if (!root.isJsonObject()) {
			return;
		}
		JsonObject object = root.getAsJsonObject();
		String spawnerId = readString(object.get("SpawnerId"));
		if (spawnerId == null) {
			LOGGER.atWarning().log("Particle color rule has no SpawnerId: %s", sourceName);
			return;
		}
		JsonElement animationElement = object.get("Animation");
		if (animationElement == null || !animationElement.isJsonObject()) {
			LOGGER.atWarning().log("Particle color rule has no Animation object: %s", sourceName);
			return;
		}
		Map<Integer, FrameRule> frames = new HashMap<>();
		for (Map.Entry<String, JsonElement> entry : animationElement.getAsJsonObject().entrySet()) {
			Integer frame = parseFrame(entry.getKey());
			if (frame == null || entry.getValue() == null || !entry.getValue().isJsonObject()) {
				continue;
			}
			FrameRule rule = parseFrameRule(entry.getValue().getAsJsonObject());
			if (rule != null && rule.hasAnyValue()) {
				frames.put(frame, rule);
			}
		}
		if (frames.isEmpty()) {
			return;
		}
		out.put(spawnerId, new SpawnerRules(spawnerId, Map.copyOf(frames), sourceName));
	}

	@Nullable
	private static FrameRule parseFrameRule(JsonObject object) {
		String paletteColor = readString(object.get("PaletteColor"));
		Double brightness = readDouble(object.get("SetBrightness"));
		Double hueShift = readDouble(object.get("HueShift"));
		if (paletteColor == null && brightness == null && hueShift == null) {
			return null;
		}
		return new FrameRule(
			paletteColor, clamp01(brightness), hueShift != null
			                                   ? hueShift
			                                   : 0.0D
		);
	}

	@Nullable
	private static Integer parseFrame(String key) {
		if (key == null || key.isBlank()) {
			return null;
		}
		try {
			return Integer.parseInt(key.trim());
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	@Nullable
	private static String readString(@Nullable JsonElement value) {
		if (value == null || !value.isJsonPrimitive()) {
			return null;
		}
		try {
			String text = value.getAsString();
			return text == null || text.isBlank()
			       ? null
			       : text.trim();
		} catch (Exception ignored) {
			return null;
		}
	}

	@Nullable
	private static Double readDouble(@Nullable JsonElement value) {
		if (value == null || !value.isJsonPrimitive()) {
			return null;
		}
		try {
			double number = value.getAsDouble();
			return Double.isFinite(number)
			       ? number
			       : null;
		} catch (Exception ignored) {
			return null;
		}
	}

	@Nullable
	private static Double clamp01(@Nullable Double value) {
		if (value == null) {
			return null;
		}
		return Math.max(0.0D, Math.min(1.0D, value));
	}

	public record SpawnerRules(String spawnerId, Map<Integer, FrameRule> animation, String sourceName) {
	}

	public record FrameRule(@Nullable String paletteColor, @Nullable Double brightness, double hueShift) {
		boolean hasAnyValue() {
			return this.paletteColor != null || this.brightness != null || Math.abs(this.hueShift) > 1.0E-9D;
		}
	}
}
