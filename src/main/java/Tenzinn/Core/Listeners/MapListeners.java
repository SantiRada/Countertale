package Tenzinn.Core.Listeners;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import org.joml.Vector3d;

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

    private static File jsonFileRef = null;

    public static final class SpawnPoint {
        public final double x, y, z;

        public SpawnPoint(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }

        @Override
        public String toString() { return "SpawnPoint{x=" + x + ", y=" + y + ", z=" + z + "}"; }
    }

    // ── NUEVO: TemporalWall ──────────────────────────────────────────────────
    public static final class TemporalWall {
        public final double fromX, fromY, fromZ;
        public final double toX,   toY,   toZ;

        public TemporalWall(double fromX, double fromY, double fromZ,
                            double toX,   double toY,   double toZ) {
            this.fromX = fromX; this.fromY = fromY; this.fromZ = fromZ;
            this.toX   = toX;   this.toY   = toY;   this.toZ   = toZ;
        }

        @Override
        public String toString() {
            return "TemporalWall{from=(" + fromX + "," + fromY + "," + fromZ + ")" +
                    ", to=(" + toX + "," + toY + "," + toZ + ")}";
        }
    }
    // ────────────────────────────────────────────────────────────────────────

    public enum SpawnMode { DM, FVF }

    private static final class MapData {
        final List<SpawnPoint>   dm;
        final List<SpawnPoint>   fvf;
        final List<TemporalWall> walls; // NUEVO

        MapData(List<SpawnPoint> dm, List<SpawnPoint> fvf, List<TemporalWall> walls) {
            this.dm    = Collections.unmodifiableList(dm);
            this.fvf   = Collections.unmodifiableList(fvf);
            this.walls = Collections.unmodifiableList(walls); // NUEVO
        }

        List<SpawnPoint> get(SpawnMode mode) {
            return mode == SpawnMode.FVF ? fvf : dm;
        }
    }

    private static final Logger LOGGER    = Logger.getLogger("Countertale");
    private static final String JSON_PATH = "Countertale/maps.json";

    private static final Map<String, MapData> MAPS = new HashMap<>();
    private static Vector3d lobbyPosition = null;
    private static boolean loaded = false;

    private MapListeners() { }

    public static boolean load() {
        try {
            File jar      = new File(MapListeners.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File jsonFile = new File(jar.getParentFile(), JSON_PATH);
            jsonFileRef = jsonFile;
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

                List<SpawnPoint>   dmSpawns  = parseSpawnArray(spawnsObj.getAsJsonArray("dm"));
                List<SpawnPoint>   fvfSpawns = parseSpawnArray(spawnsObj.getAsJsonArray("fvf"));

                // NUEVO: parsear TemporalWall (opcional: puede no existir en el JSON)
                List<TemporalWall> walls = mapObj.has("TemporalWall")
                        ? parseWallArray(mapObj.getAsJsonArray("TemporalWall"))
                        : new ArrayList<>();

                MAPS.put(name, new MapData(dmSpawns, fvfSpawns, walls));
            }

            if (root.has("Lobby")) {
                JsonArray lobby = root.getAsJsonArray("Lobby");
                lobbyPosition = new Vector3d(
                        lobby.get(0).getAsDouble(),
                        lobby.get(1).getAsDouble(),
                        lobby.get(2).getAsDouble()
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

    // NUEVO
    private static List<TemporalWall> parseWallArray(JsonArray array) {
        List<TemporalWall> list = new ArrayList<>();
        if (array == null) return list;
        for (JsonElement el : array) {
            JsonObject obj  = el.getAsJsonObject();
            JsonArray  from = obj.getAsJsonArray("from");
            JsonArray  to   = obj.getAsJsonArray("to");
            list.add(new TemporalWall(
                    from.get(0).getAsDouble(), from.get(1).getAsDouble(), from.get(2).getAsDouble(),
                    to.get(0).getAsDouble(),   to.get(1).getAsDouble(),   to.get(2).getAsDouble()
            ));
        }
        return list;
    }

    // ── API pública ──────────────────────────────────────────────────────────
    public static List<SpawnPoint> get(String mapName, SpawnMode mode) {
        MapData data = MAPS.get(mapName.toLowerCase());
        if (data == null) {
            LOGGER.warning("[Countertale] Map not found: " + mapName);
            return Collections.emptyList();
        }
        return data.get(mode);
    }
    public static List<SpawnPoint>   getDM(String mapName)  { return get(mapName, SpawnMode.DM);  }
    public static List<SpawnPoint>   getFVF(String mapName) { return get(mapName, SpawnMode.FVF); }

    // NUEVO
    public static List<TemporalWall> getWalls(String mapName) {
        MapData data = MAPS.get(mapName.toLowerCase());
        if (data == null) {
            LOGGER.warning("[Countertale] Map not found: " + mapName);
            return Collections.emptyList();
        }
        return data.walls;
    }
    public static boolean addWall(String mapName, TemporalWall wall) {
        if (mapName == null || !loaded) return false;
        MapData data = MAPS.get(mapName.toLowerCase());
        if (data == null) return false;

        // Necesitamos una lista mutable; reemplazamos MapData
        List<TemporalWall> mutable = new ArrayList<>(data.walls);
        mutable.add(wall);
        MAPS.put(mapName.toLowerCase(), new MapData(data.dm, data.fvf, mutable));
        save();
        return true;
    }
    public static boolean replaceWall(String mapName, int index, TemporalWall wall) {
        if (mapName == null || !loaded) return false;
        MapData data = MAPS.get(mapName.toLowerCase());
        if (data == null || index < 0 || index >= data.walls.size()) return false;

        List<TemporalWall> mutable = new ArrayList<>(data.walls);
        mutable.set(index, wall);
        MAPS.put(mapName.toLowerCase(), new MapData(data.dm, data.fvf, mutable));
        save();
        return true;
    }
    public static boolean removeWall(String mapName, int index) {
        if (mapName == null || !loaded) return false;
        MapData data = MAPS.get(mapName.toLowerCase());
        if (data == null || index < 0 || index >= data.walls.size()) return false;

        List<TemporalWall> mutable = new ArrayList<>(data.walls);
        mutable.remove(index);
        MAPS.put(mapName.toLowerCase(), new MapData(data.dm, data.fvf, mutable));
        save();
        return true;
    }
    public static boolean isLoaded() { return loaded; }
    public static boolean exists(String mapName) { return MAPS.containsKey(mapName.toLowerCase()); }
    public static Vector3d getLobby() {
        return lobbyPosition != null ? lobbyPosition : new Vector3d(0, 80, 0);
    }
    public static java.util.Set<String> getMapNames() { return Collections.unmodifiableSet(MAPS.keySet()); }
    public static int size() { return MAPS.size(); }
    public static boolean save() {
        if (jsonFileRef == null || !loaded) return false;

        try {
            JsonObject root = new JsonObject();

            // Lobby
            if (lobbyPosition != null) {
                JsonArray lobby = new JsonArray();
                lobby.add(lobbyPosition.x);
                lobby.add(lobbyPosition.y);
                lobby.add(lobbyPosition.z);
                root.add("Lobby", lobby);
            }

            // Maps
            JsonArray mapsArray = new JsonArray();
            for (Map.Entry<String, MapData> entry : MAPS.entrySet()) {
                MapData data = entry.getValue();
                JsonObject mapObj = new JsonObject();
                mapObj.addProperty("name", entry.getKey());

                // Spawns
                JsonObject spawnsObj = new JsonObject();
                spawnsObj.add("dm",  spawnListToJson(data.dm));
                spawnsObj.add("fvf", spawnListToJson(data.fvf));
                mapObj.add("spawns", spawnsObj);

                // TemporalWall (solo si hay)
                if (!data.walls.isEmpty()) {
                    mapObj.add("TemporalWall", wallListToJson(data.walls));
                }

                mapsArray.add(mapObj);
            }
            root.add("Maps", mapsArray);

            // Escribir al archivo con pretty print
            String json = new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root);

            try (java.io.FileWriter writer = new java.io.FileWriter(jsonFileRef)) {
                writer.write(json);
            }

            LOGGER.info("[Countertale] maps.json guardado correctamente.");
            return true;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[Countertale] Error al guardar maps.json.", e);
            return false;
        }
    }
    private static JsonArray spawnListToJson(List<SpawnPoint> list) {
        JsonArray arr = new JsonArray();
        for (SpawnPoint sp : list) {
            JsonArray coords = new JsonArray();
            coords.add(sp.x); coords.add(sp.y); coords.add(sp.z);
            arr.add(coords);
        }
        return arr;
    }
    private static JsonArray wallListToJson(List<TemporalWall> list) {
        JsonArray arr = new JsonArray();
        for (TemporalWall w : list) {
            JsonObject obj  = new JsonObject();
            JsonArray  from = new JsonArray();
            from.add(w.fromX); from.add(w.fromY); from.add(w.fromZ);
            JsonArray  to   = new JsonArray();
            to.add(w.toX); to.add(w.toY); to.add(w.toZ);
            obj.add("from", from);
            obj.add("to",   to);
            arr.add(obj);
        }
        return arr;
    }
}