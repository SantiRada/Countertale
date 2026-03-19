package Tenzinn.Core.Listeners;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.hypixel.hytale.math.vector.Vector3f;

import java.io.File;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.FileNotFoundException;

public final class MapListeners {

    public static final class SpawnPoint {
        public final double x, y, z;

        public SpawnPoint(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }

        @Override
        public String toString() { return "SpawnPoint{x=" + x + ", y=" + y + ", z=" + z + "}"; }
    }

    public enum SpawnMode { DM, FVF }

    // Estructura interna: nombre del mapa → (modo → lista de spawns)
    private static final class MapData {
        final List<SpawnPoint> dm;
        final List<SpawnPoint> fvf;

        MapData(List<SpawnPoint> dm, List<SpawnPoint> fvf) {
            this.dm  = Collections.unmodifiableList(dm);
            this.fvf = Collections.unmodifiableList(fvf);
        }

        List<SpawnPoint> get(SpawnMode mode) {
            return mode == SpawnMode.FVF ? fvf : dm;
        }
    }

    private static final Logger LOGGER    = Logger.getLogger("Countertale");
    private static final String JSON_PATH = "Countertale/maps.json";

    private static final Map<String, MapData> MAPS = new HashMap<>();
    private static Vector3f lobbyPosition = null;
    private static boolean loaded = false;

    private MapListeners() { }

    public static boolean load() {
        try {
            File jar      = new File(MapListeners.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File jsonFile = new File(jar.getParentFile(), JSON_PATH);
            return load(jsonFile);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[Countertale] No se pudo resolver la ruta del .jar.", e);
            return false;
        }
    }

    public static boolean load(File jsonFile) {
        lobbyPosition = null;
        MAPS.clear();
        loaded = false;

        if (!jsonFile.exists()) {
            LOGGER.warning("[Countertale] maps.json not found at: " + jsonFile.getAbsolutePath());
            return false;
        }

        try (FileReader reader = new FileReader(jsonFile)) {

            JsonObject root      = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray  mapsArray = root.getAsJsonArray("Maps");

            for (JsonElement mapElement : mapsArray) {
                JsonObject mapObj    = mapElement.getAsJsonObject();
                String     name      = mapObj.get("name").getAsString().toLowerCase();
                JsonObject spawnsObj = mapObj.getAsJsonObject("spawns");

                List<SpawnPoint> dmSpawns  = parseSpawnArray(spawnsObj.getAsJsonArray("dm"));
                List<SpawnPoint> fvfSpawns = parseSpawnArray(spawnsObj.getAsJsonArray("fvf"));

                MAPS.put(name, new MapData(dmSpawns, fvfSpawns));
            }

            if (root.has("Lobby")) {
                JsonArray lobby = root.getAsJsonArray("Lobby");
                lobbyPosition = new Vector3f(
                        lobby.get(0).getAsFloat(),
                        lobby.get(1).getAsFloat(),
                        lobby.get(2).getAsFloat()
                );
            }

            loaded = true;
            LOGGER.info("[Countertale] Loaded " + MAPS.size() + " maps from maps.json");

        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, "[Countertale] maps.json not found.", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[Countertale] Failed to parse maps.json.", e);
        }

        return loaded;
    }

    // ── Helpers de parseo ────────────────────────────────────────────────────

    private static List<SpawnPoint> parseSpawnArray(JsonArray array) {
        List<SpawnPoint> list = new ArrayList<>();
        if (array == null) return list;
        for (JsonElement el : array) {
            JsonArray coords = el.getAsJsonArray();
            list.add(new SpawnPoint(
                    coords.get(0).getAsDouble(),
                    coords.get(1).getAsDouble(),
                    coords.get(2).getAsDouble()
            ));
        }
        return list;
    }

    // ── API pública ──────────────────────────────────────────────────────────

    /** Devuelve los spawns de un mapa según el modo (DM o FVF). */
    public static List<SpawnPoint> get(String mapName, SpawnMode mode) {
        MapData data = MAPS.get(mapName.toLowerCase());
        if (data == null) {
            LOGGER.warning("[Countertale] Map not found: " + mapName);
            return Collections.emptyList();
        }
        return data.get(mode);
    }

    /** Devuelve los spawns DM de un mapa (shorthand). */
    public static List<SpawnPoint> getDM(String mapName)  { return get(mapName, SpawnMode.DM);  }

    /** Devuelve los spawns FVF de un mapa (shorthand). */
    public static List<SpawnPoint> getFVF(String mapName) { return get(mapName, SpawnMode.FVF); }

    public static Vector3f getLobby() {
        return lobbyPosition != null ? lobbyPosition : new Vector3f(0f, 80f, 0f);
    }

    public static boolean exists(String mapName)            { return MAPS.containsKey(mapName.toLowerCase()); }
    public static java.util.Set<String> getMapNames()       { return Collections.unmodifiableSet(MAPS.keySet()); }
    public static boolean isLoaded()                        { return loaded; }
    public static int size()                                { return MAPS.size(); }
}