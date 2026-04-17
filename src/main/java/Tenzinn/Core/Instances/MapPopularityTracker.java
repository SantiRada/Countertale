package Tenzinn.Core.Instances;

import Tenzinn.Core.Listeners.MapListeners;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.google.gson.GsonBuilder;

import java.util.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URISyntaxException;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

public final class MapPopularityTracker {

    private static final Logger LOGGER = Logger.getLogger("Countertale");
    private static final String STATS_PATH = "Countertale/map_stats.json";

    private static final int FALLBACK_PROMOTION_THRESHOLD = 3;
    private static final int WEEKLY_WINDOW_DAYS = 7;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final class MapStats {
        int    totalPlayed          = 0;
        int    weeklyPlayed         = 0;
        String weeklyReset          = LocalDate.now().format(DATE_FMT);
        int    fallbackCount        = 0;
        int    consecutiveFallbacks = 0;
        boolean promoted            = false;

        void checkWeeklyReset() {
            LocalDate reset = LocalDate.parse(weeklyReset, DATE_FMT);
            if (LocalDate.now().isAfter(reset.plusDays(WEEKLY_WINDOW_DAYS))) {
                weeklyPlayed = 0;
                weeklyReset  = LocalDate.now().format(DATE_FMT);
            }
        }

        void recordMatch() {
            checkWeeklyReset();
            totalPlayed++;
            weeklyPlayed++;
            consecutiveFallbacks = 0;
        }

        void recordFallback() {
            fallbackCount++;
            consecutiveFallbacks++;
        }

        int popularityScore() {
            checkWeeklyReset();
            return (weeklyPlayed * 3) + totalPlayed;
        }
    }

    private final Map<String, MapStats> stats = new ConcurrentHashMap<>();
    private File statsFile;

    public MapPopularityTracker() {
        resolveStatsFile();
        load();

        for (String mapId : MapListeners.getMapNames()) { stats.computeIfAbsent(mapId.toLowerCase(), k -> new MapStats()); }
    }
    public synchronized void recordMatchFinished(String mapId) {
        MapStats s = getOrCreate(mapId);
        s.recordMatch();
        save();
        LOGGER.log(Level.FINE, "[Popularity] Partida registrada en mapa: " + mapId + " | weekly=" + s.weeklyPlayed + " total=" + s.totalPlayed);
    }
    public synchronized void recordFallback(String mapId) {
        MapStats s = getOrCreate(mapId);
        s.recordFallback();

        if (!s.promoted && s.consecutiveFallbacks >= FALLBACK_PROMOTION_THRESHOLD) {
            s.promoted = true;
            LOGGER.log(Level.WARNING, "[Popularity] Mapa '" + mapId + "' promovido a popular por " + s.consecutiveFallbacks + " fallbacks consecutivos.");
        }

        save();
        LOGGER.log(Level.INFO, "[Popularity] Fallback en mapa: " + mapId + " | consecutivos=" + s.consecutiveFallbacks);
    }
    public synchronized List<String> getMapsSortedByPriority() {
        List<String> allMaps = new ArrayList<>(stats.keySet());

        allMaps.sort((a, b) -> {
            MapStats sa = stats.get(a);
            MapStats sb = stats.get(b);
            if (sa.promoted != sb.promoted) return sa.promoted ? -1 : 1;
            return Integer.compare(sb.popularityScore(), sa.popularityScore());
        });

        return Collections.unmodifiableList(allMaps);
    }
    public synchronized String pickBestMap(List<String> candidates) {
        if (candidates == null || candidates.isEmpty()) return null;

        return candidates.stream()
                .max(Comparator.comparingInt(mapId -> {
                    MapStats s = stats.get(mapId.toLowerCase());
                    if (s != null && s.promoted) return Integer.MAX_VALUE; // promovidos siempre primero
                    return s != null ? s.popularityScore() : 0;
                }))
                .orElse(candidates.get(0));
    }
    public synchronized Map<String, Integer> distribute(int ceiling) {
        List<String> sorted = getMapsSortedByPriority();
        int mapCount = sorted.size();

        Map<String, Integer> result = new LinkedHashMap<>();
        for (String mapId : sorted) result.put(mapId, 0);

        if (ceiling <= 0 || mapCount == 0) return result;

        if (mapCount >= 4) {
            int popularCount  = mapCount / 2;
            int popularWeight = 2;
            int normalWeight  = 1;

            int totalWeight = (popularCount * popularWeight)
                    + ((mapCount - popularCount) * normalWeight);
            int remaining = ceiling;

            for (int i = 0; i < sorted.size(); i++) {
                String mapId  = sorted.get(i);
                int weight    = (i < popularCount) ? popularWeight : normalWeight;
                int allocated = (int) Math.round((double) weight / totalWeight * ceiling);
                allocated     = Math.min(allocated, remaining);
                result.put(mapId, allocated);
                remaining -= allocated;
            }

            for (String mapId : sorted) {
                if (remaining <= 0) break;
                result.merge(mapId, 1, Integer::sum);
                remaining--;
            }
        } else {
            int remaining = ceiling;

            for (String mapId : sorted) {
                int give = Math.min(1, remaining);
                result.put(mapId, give);
                remaining -= give;
            }
            for (String mapId : sorted) {
                if (remaining <= 0) break;
                result.merge(mapId, 1, Integer::sum);
                remaining--;
            }
        }

        return result;
    }
    public synchronized String getSummary() {
        StringBuilder sb = new StringBuilder("[Popularity] Estado actual:\n");
        for (Map.Entry<String, MapStats> e : stats.entrySet()) {
            MapStats s = e.getValue();
            sb.append("  ").append(e.getKey())
              .append(" | score=").append(s.popularityScore())
              .append(" total=").append(s.totalPlayed)
              .append(" weekly=").append(s.weeklyPlayed)
              .append(" fallbacks=").append(s.fallbackCount)
              .append(s.promoted ? " [PROMOVIDO]" : "")
              .append("\n");
        }
        return sb.toString();
    }

    private void resolveStatsFile() {
        try {
            File jar = new File(MapPopularityTracker.class
                    .getProtectionDomain().getCodeSource().getLocation().toURI());
            statsFile = new File(jar.getParentFile(), STATS_PATH);
        } catch (URISyntaxException e) {
            LOGGER.log(Level.WARNING, "[Popularity] No se pudo resolver la ruta de map_stats.json.", e);
            statsFile = null;
        }
    }
    private void load() {
        if (statsFile == null || !statsFile.exists()) return;

        try (FileReader reader = new FileReader(statsFile)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            for (String mapId : root.keySet()) {
                JsonObject obj = root.getAsJsonObject(mapId);
                MapStats s = new MapStats();
                if (obj.has("totalPlayed"))          s.totalPlayed          = obj.get("totalPlayed").getAsInt();
                if (obj.has("weeklyPlayed"))          s.weeklyPlayed         = obj.get("weeklyPlayed").getAsInt();
                if (obj.has("weeklyReset"))           s.weeklyReset          = obj.get("weeklyReset").getAsString();
                if (obj.has("fallbackCount"))         s.fallbackCount        = obj.get("fallbackCount").getAsInt();
                if (obj.has("consecutiveFallbacks"))  s.consecutiveFallbacks = obj.get("consecutiveFallbacks").getAsInt();
                if (obj.has("promoted"))              s.promoted             = obj.get("promoted").getAsBoolean();

                s.checkWeeklyReset();
                stats.put(mapId.toLowerCase(), s);
            }

            LOGGER.info("[Popularity] Stats cargadas desde map_stats.json (" + stats.size() + " mapas).");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[Popularity] Error al leer map_stats.json.", e);
        }
    }
    private synchronized void save() {
        if (statsFile == null) return;

        try {
            if (!statsFile.getParentFile().exists()) {
                statsFile.getParentFile().mkdirs();
            }

            JsonObject root = new JsonObject();
            for (Map.Entry<String, MapStats> e : stats.entrySet()) {
                MapStats s = e.getValue();
                JsonObject obj = new JsonObject();
                obj.addProperty("totalPlayed",          s.totalPlayed);
                obj.addProperty("weeklyPlayed",         s.weeklyPlayed);
                obj.addProperty("weeklyReset",          s.weeklyReset);
                obj.addProperty("fallbackCount",        s.fallbackCount);
                obj.addProperty("consecutiveFallbacks", s.consecutiveFallbacks);
                obj.addProperty("promoted",             s.promoted);
                root.add(e.getKey(), obj);
            }

            try (FileWriter writer = new FileWriter(statsFile)) {
                GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[Popularity] Error al guardar map_stats.json.", e);
        }
    }
    private MapStats getOrCreate(String mapId) { return stats.computeIfAbsent(mapId.toLowerCase(), k -> new MapStats()); }
}