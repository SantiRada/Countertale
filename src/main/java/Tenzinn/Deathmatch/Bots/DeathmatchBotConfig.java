package Tenzinn.Deathmatch.Bots;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

public record DeathmatchBotConfig(
        boolean enabled,
        int fillDeathmatchTo,
        long tickMs,
        double targetRange,
        double desiredRangeMin,
        double desiredRangeMax,
        double tooCloseRange,
        double chaseRange,
        long respawnDelayMs,
        long movementUpdateMs,
        long repositionCooldownMs,
        double hitChanceClose,
        double hitChanceMedium,
        double hitChanceLong,
        int akDamage,
        int m4Damage,
        long akCooldownMs,
        long m4CooldownMs,
        boolean useGunConfig,
        double botDamageMultiplier,
        int maxBotDamage,
        int minBotDamage,
        boolean botFireSound,
        boolean botUseRealProjectiles,
        boolean botDirectDamageFallback,
        double botYawOffsetDegrees,
        double botPitchOffsetDegrees,
        double botProjectileEyeHeight,
        double botProjectileForwardOffset,
        boolean debugLogging
) {
    private static final Logger LOGGER = Logger.getLogger(DeathmatchBotConfig.class.getName());
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String RELATIVE_PATH = "OrbisOffensive/bots.json";

    public static LoadResult loadOrCreate() {
        Path path = resolveConfigPath();
        DeathmatchBotConfig defaults = defaults();

        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[DeathmatchBot] Failed creating config directory for bots.json.", e);
        }

        if (!Files.exists(path)) {
            writeConfig(path, defaults);
            LOGGER.log(Level.INFO, "[DeathmatchBot] Created default bots config at: " + path.toAbsolutePath());
            return new LoadResult(defaults, path.toAbsolutePath().toString(), true, true);
        }

        try (FileReader reader = new FileReader(path.toFile())) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            DeathmatchBotConfig parsed = fromJson(root);
            return new LoadResult(parsed, path.toAbsolutePath().toString(), true, false);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[DeathmatchBot] Invalid bots.json at " + path.toAbsolutePath()
                    + ". Using safe defaults for this runtime.", e);
            return new LoadResult(defaults, path.toAbsolutePath().toString(), false, false);
        }
    }

    public static DeathmatchBotConfig defaults() {
        return new DeathmatchBotConfig(
                true,
                10,
                500L,
                64.0d,
                8.0d,
                18.0d,
                5.0d,
                64.0d,
                3000L,
                1000L,
                2000L,
                0.65d,
                0.55d,
                0.35d,
                8,
                7,
                750L,
                650L,
                true,
                1.0d,
                100,
                1,
                true,
                true,
                false,
                0.0d,
                0.0d,
                1.45d,
                0.4d,
                false
        );
    }

    public static String getConfigPath() {
        return resolveConfigPath().toAbsolutePath().toString();
    }

    private static DeathmatchBotConfig fromJson(JsonObject root) {
        DeathmatchBotConfig defaults = defaults();

        boolean enabled = readBoolean(root, "enabled", defaults.enabled);

        int fillDeathmatchTo = clampInt(
                readInt(root, "fillDeathmatchTo", defaults.fillDeathmatchTo),
                0, 10
        );

        long tickMs = Math.max(250L, readLong(root, "tickMs", defaults.tickMs));
        double targetRange = Math.max(4.0d, readDouble(root, "targetRange", defaults.targetRange));

        double desiredRangeMin = Math.max(1.0d, readDouble(root, "desiredRangeMin", defaults.desiredRangeMin));
        double desiredRangeMax = Math.max(desiredRangeMin, readDouble(root, "desiredRangeMax", defaults.desiredRangeMax));
        desiredRangeMax = Math.min(desiredRangeMax, targetRange);
        desiredRangeMin = Math.min(desiredRangeMin, desiredRangeMax);

        double tooCloseRange = Math.max(1.0d, readDouble(root, "tooCloseRange", defaults.tooCloseRange));
        tooCloseRange = Math.min(tooCloseRange, desiredRangeMin);

        double chaseRange = Math.max(desiredRangeMax, readDouble(root, "chaseRange", defaults.chaseRange));
        chaseRange = Math.min(chaseRange, targetRange);

        long respawnDelayMs = Math.max(1000L, readLong(root, "respawnDelayMs", defaults.respawnDelayMs));
        long movementUpdateMs = Math.max(250L, readLong(root, "movementUpdateMs", defaults.movementUpdateMs));
        long repositionCooldownMs = Math.max(500L, readLong(root, "repositionCooldownMs", defaults.repositionCooldownMs));

        double hitChanceClose = clampDouble(readDouble(root, "hitChanceClose", defaults.hitChanceClose), 0.0d, 1.0d);
        double hitChanceMedium = clampDouble(readDouble(root, "hitChanceMedium", defaults.hitChanceMedium), 0.0d, 1.0d);
        double hitChanceLong = clampDouble(readDouble(root, "hitChanceLong", defaults.hitChanceLong), 0.0d, 1.0d);

        int akDamage = clampInt(readInt(root, "akDamage", defaults.akDamage), 1, 25);
        int m4Damage = clampInt(readInt(root, "m4Damage", defaults.m4Damage), 1, 25);

        long akCooldownMs = Math.max(250L, readLong(root, "akCooldownMs", defaults.akCooldownMs));
        long m4CooldownMs = Math.max(250L, readLong(root, "m4CooldownMs", defaults.m4CooldownMs));

        boolean useGunConfig = readBoolean(root, "useGunConfig", defaults.useGunConfig);
        double botDamageMultiplier = clampDouble(readDouble(root, "botDamageMultiplier", defaults.botDamageMultiplier), 0.05d, 2.0d);
        int maxBotDamage = clampInt(readInt(root, "maxBotDamage", defaults.maxBotDamage), 1, 200);
        int minBotDamage = clampInt(readInt(root, "minBotDamage", defaults.minBotDamage), 1, maxBotDamage);
        boolean botFireSound = readBoolean(root, "botFireSound", defaults.botFireSound);
        boolean botUseRealProjectiles = readBoolean(root, "botUseRealProjectiles", defaults.botUseRealProjectiles);
        boolean botDirectDamageFallback = readBoolean(root, "botDirectDamageFallback", defaults.botDirectDamageFallback);
        double botYawOffsetDegrees = clampDouble(readDouble(root, "botYawOffsetDegrees", defaults.botYawOffsetDegrees), -180.0d, 180.0d);
        double botPitchOffsetDegrees = clampDouble(readDouble(root, "botPitchOffsetDegrees", defaults.botPitchOffsetDegrees), -90.0d, 90.0d);
        double botProjectileEyeHeight = clampDouble(readDouble(root, "botProjectileEyeHeight", defaults.botProjectileEyeHeight), 0.5d, 2.5d);
        double botProjectileForwardOffset = clampDouble(readDouble(root, "botProjectileForwardOffset", defaults.botProjectileForwardOffset), 0.0d, 2.0d);

        boolean debugLogging = readBoolean(root, "debugLogging", defaults.debugLogging);

        return new DeathmatchBotConfig(
                enabled,
                fillDeathmatchTo,
                tickMs,
                targetRange,
                desiredRangeMin,
                desiredRangeMax,
                tooCloseRange,
                chaseRange,
                respawnDelayMs,
                movementUpdateMs,
                repositionCooldownMs,
                hitChanceClose,
                hitChanceMedium,
                hitChanceLong,
                akDamage,
                m4Damage,
                akCooldownMs,
                m4CooldownMs,
                useGunConfig,
                botDamageMultiplier,
                maxBotDamage,
                minBotDamage,
                botFireSound,
                botUseRealProjectiles,
                botDirectDamageFallback,
                botYawOffsetDegrees,
                botPitchOffsetDegrees,
                botProjectileEyeHeight,
                botProjectileForwardOffset,
                debugLogging
        );
    }

    private static boolean writeConfig(Path path, DeathmatchBotConfig config) {
        if (path == null || config == null) {
            return false;
        }

        try (FileWriter writer = new FileWriter(path.toFile())) {
            writer.write(GSON.toJson(config.toJsonObject()));
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[DeathmatchBot] Failed writing bots.json at: " + path.toAbsolutePath(), e);
            return false;
        }
    }

    private JsonObject toJsonObject() {
        JsonObject root = new JsonObject();
        root.addProperty("enabled", enabled);
        root.addProperty("fillDeathmatchTo", fillDeathmatchTo);
        root.addProperty("tickMs", tickMs);
        root.addProperty("targetRange", targetRange);
        root.addProperty("desiredRangeMin", desiredRangeMin);
        root.addProperty("desiredRangeMax", desiredRangeMax);
        root.addProperty("tooCloseRange", tooCloseRange);
        root.addProperty("chaseRange", chaseRange);
        root.addProperty("respawnDelayMs", respawnDelayMs);
        root.addProperty("movementUpdateMs", movementUpdateMs);
        root.addProperty("repositionCooldownMs", repositionCooldownMs);
        root.addProperty("hitChanceClose", hitChanceClose);
        root.addProperty("hitChanceMedium", hitChanceMedium);
        root.addProperty("hitChanceLong", hitChanceLong);
        root.addProperty("akDamage", akDamage);
        root.addProperty("m4Damage", m4Damage);
        root.addProperty("akCooldownMs", akCooldownMs);
        root.addProperty("m4CooldownMs", m4CooldownMs);
        root.addProperty("useGunConfig", useGunConfig);
        root.addProperty("botDamageMultiplier", botDamageMultiplier);
        root.addProperty("maxBotDamage", maxBotDamage);
        root.addProperty("minBotDamage", minBotDamage);
        root.addProperty("botFireSound", botFireSound);
        root.addProperty("botUseRealProjectiles", botUseRealProjectiles);
        root.addProperty("botDirectDamageFallback", botDirectDamageFallback);
        root.addProperty("botYawOffsetDegrees", botYawOffsetDegrees);
        root.addProperty("botPitchOffsetDegrees", botPitchOffsetDegrees);
        root.addProperty("botProjectileEyeHeight", botProjectileEyeHeight);
        root.addProperty("botProjectileForwardOffset", botProjectileForwardOffset);
        root.addProperty("debugLogging", debugLogging);
        return root;
    }

    private static Path resolveConfigPath() {
        try {
            File jar = new File(DeathmatchBotConfig.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File base = jar.isDirectory() ? jar : jar.getParentFile();
            return base.toPath().resolve(RELATIVE_PATH);
        } catch (Exception e) {
            return Paths.get(RELATIVE_PATH).toAbsolutePath();
        }
    }

    private static int readInt(JsonObject root, String key, int fallback) {
        try {
            if (root != null && root.has(key) && !root.get(key).isJsonNull()) {
                return root.get(key).getAsInt();
            }
        } catch (Exception ignored) {
            // Use fallback below.
        }
        return fallback;
    }

    private static long readLong(JsonObject root, String key, long fallback) {
        try {
            if (root != null && root.has(key) && !root.get(key).isJsonNull()) {
                return root.get(key).getAsLong();
            }
        } catch (Exception ignored) {
            // Use fallback below.
        }
        return fallback;
    }

    private static double readDouble(JsonObject root, String key, double fallback) {
        try {
            if (root != null && root.has(key) && !root.get(key).isJsonNull()) {
                return root.get(key).getAsDouble();
            }
        } catch (Exception ignored) {
            // Use fallback below.
        }
        return fallback;
    }

    private static boolean readBoolean(JsonObject root, String key, boolean fallback) {
        try {
            if (root != null && root.has(key) && !root.get(key).isJsonNull()) {
                return root.get(key).getAsBoolean();
            }
        } catch (Exception ignored) {
            // Use fallback below.
        }
        return fallback;
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public record LoadResult(DeathmatchBotConfig config, String path, boolean parseSuccessful, boolean createdDefaultFile) { }
}
