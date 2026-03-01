package Tenzinn.Listeners;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;

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

    private static final Logger LOGGER       = Logger.getLogger("Countertale");
    private static final String JSON_PATH    = "Countertale/maps.json";

    // Clave: nombre del mapa en minúsculas → lista de spawns
    private static final Map<String, List<SpawnPoint>> MAPS = new HashMap<>();
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
                JsonObject mapObj = mapElement.getAsJsonObject();

                String     name   = mapObj.get("name").getAsString().toLowerCase();
                JsonArray  spawnsArray = mapObj.getAsJsonArray("spawns");

                List<SpawnPoint> spawns = new ArrayList<>();
                for (JsonElement spawnElement : spawnsArray) {
                    JsonArray coords = spawnElement.getAsJsonArray();
                    double x = coords.get(0).getAsDouble();
                    double y = coords.get(1).getAsDouble();
                    double z = coords.get(2).getAsDouble();
                    spawns.add(new SpawnPoint(x, y, z));
                }

                MAPS.put(name, Collections.unmodifiableList(spawns));
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
    public static List<SpawnPoint> get(String mapName) {
        List<SpawnPoint> spawns = MAPS.get(mapName.toLowerCase());
        if (spawns == null) {
            LOGGER.warning("[Countertale] Map not found: " + mapName);
            return Collections.emptyList();
        }
        return spawns;
    }
    public static boolean exists(String mapName) { return MAPS.containsKey(mapName.toLowerCase()); }
    public static java.util.Set<String> getMapNames() { return Collections.unmodifiableSet(MAPS.keySet()); }
    public static int size() { return MAPS.size(); }
}