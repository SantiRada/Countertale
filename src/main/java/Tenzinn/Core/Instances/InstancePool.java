package Tenzinn.Core.Instances;

import Tenzinn.Countertale;
import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.Listeners.MapListeners;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.Universe;

import java.util.*;
import java.util.logging.Level;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class InstancePool {

    public  static final int MAX_TOTAL_INSTANCES     = 11;
    private static final int MAX_CREATION_RATE       = 2;
    private static final long CANDIDATE_TTL_MS       = 5 * 60 * 1000L;
    private static final long LISTENER_INTERVAL_S    = 15;
    private static final long LISTENER_INITIAL_S     = 10;

    private final Countertale main;
    private final MapPopularityTracker popularity;

    private final Map<String, Deque<InstanceManager>> pool = new ConcurrentHashMap<>();
    private final Map<InstanceManager, Long> candidates = new LinkedHashMap<>();
    private int globalBeingCreated = 0;
    private final Map<String, Integer> beingCreatedPerMap = new ConcurrentHashMap<>();

    private MatchManagerInstanceCounter counter;
    private ScheduledFuture<?> listenerTask;

    private boolean universeReady    = false;
    private boolean deepCleanPending = false;

    public InstancePool(Countertale main) {
        this.main       = main;
        this.popularity = new MapPopularityTracker();

        for (String mapId : MapListeners.getMapNames()) {
            String normalized = mapId.toLowerCase();
            pool.put(normalized, new ArrayDeque<>());
            beingCreatedPerMap.put(normalized, 0);
        }
    }
    public void markReady() {
        cleanOrphanedWorlds();
        universeReady = true;

        for (String mapId : MapListeners.getMapNames()) { enqueueCreation(mapId.toLowerCase()); }

        listenerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(this::runListenerCycle, LISTENER_INITIAL_S, LISTENER_INTERVAL_S, TimeUnit.SECONDS);
        main.getLogger().at(Level.INFO).log("[Pool] Inicializado con mapas: " + MapListeners.getMapNames());
    }
    public void setCounter(MatchManagerInstanceCounter counter) { this.counter = counter; }
    public MapPopularityTracker getPopularity() { return popularity; }
    public synchronized InstanceManager take(String mapId, Runnable whenReady) {
        return take(mapId, instance -> {
            if (whenReady != null) whenReady.run();
        });
    }
    public synchronized InstanceManager take(String mapId, Consumer<InstanceManager> whenReady) {
        Deque<InstanceManager> mapPool = pool.get(mapId);
        InstanceManager instance = (mapPool != null) ? mapPool.pollFirst() : null;

        if (instance != null) {
            main.getLogger().at(Level.INFO).log("[Pool] Instancia entregada [" + mapId + "]. Restantes: " + poolSize(mapId));
            enqueueCreation(mapId);
            if (whenReady != null) whenReady.accept(instance);
            return instance;
        } else {
            main.getLogger().at(Level.WARNING).log("[Pool] FALLBACK en mapa '" + mapId + "': creando instancia en caliente.");
            popularity.recordFallback(mapId);
            instance = new InstanceManager(main, mapId);
            InstanceManager pendingInstance = instance;
            instance.preloadMap(() -> {
                if (whenReady != null) whenReady.accept(pendingInstance);
            });
        }

        enqueueCreation(mapId);
        return instance;
    }
    public synchronized void onMatchFinished(String mapId, InstanceManager instance) {
        popularity.recordMatchFinished(mapId);
        instance.removeInstance();

        runListenerCycle();
    }
    private synchronized void runListenerCycle() {
        if (!universeReady) return;

        int ceiling = calcCeiling();

        main.getLogger().at(Level.INFO).log("[Pool] Ciclo listener | techo=" + ceiling + " playersReady=" + RefactorTool.getPlayersReady()
                + " imminent=" + RefactorTool.getPlayersReadyInOneMinute());

        if (ceiling == 0 && RefactorTool.getPlayersReadyInOneMinute() < 10) {
            if (!deepCleanPending) { deepCleanPending = true; scheduleDeepClean(); }
            return;
        }

        deepCleanPending = false;
        Map<String, Integer> ideal = popularity.distribute(ceiling);

        for (String mapId : MapListeners.getMapNames()) {
            int idealCount = ideal.getOrDefault(mapId, 0);
            int current    = poolSize(mapId) + beingCreatedPerMap.getOrDefault(mapId, 0);
            int missing    = idealCount - current;

            if (missing > 0) {
                for (int i = 0; i < missing; i++) enqueueCreation(mapId);
            } else if (missing < 0) { markExcess(mapId, -missing); }
        }

        evictExpiredCandidates();
    }
    private int calcCeiling() {
        int ready     = RefactorTool.getPlayersReady();
        int imminent  = RefactorTool.getPlayersReadyInOneMinute();
        int confReady = (imminent / 10) * 10;
        return (ready + confReady) / 10;
    }
    private synchronized void enqueueCreation(String mapId) {
        if (!universeReady) return;
        if (!canCreateInstance())  return;
        if (globalBeingCreated >= MAX_CREATION_RATE) {
            main.getLogger().at(Level.FINE).log("[Pool] Rate limit alcanzado (" + MAX_CREATION_RATE + "). Se creará en el próximo ciclo.");
            return;
        }

        createBackgroundInstance(mapId);
    }
    private void createBackgroundInstance(String mapId) {
        globalBeingCreated++;
        beingCreatedPerMap.merge(mapId, 1, Integer::sum);

        main.getLogger().at(Level.INFO).log("[Pool] Iniciando preload [" + mapId + "]. En creación global: " + globalBeingCreated);

        InstanceManager bg = new InstanceManager(main, mapId);
        bg.preloadMap(() -> {
            synchronized (this) {
                globalBeingCreated--;
                beingCreatedPerMap.merge(mapId, -1, Integer::sum);

                if (candidates.containsKey(bg)) {
                    candidates.remove(bg);
                    bg.removeInstance();
                    main.getLogger().at(Level.INFO).log("[Pool] Instancia creada pero ya no necesaria [" + mapId + "]. Destruida.");
                } else {
                    Deque<InstanceManager> mapPool = pool.computeIfAbsent(mapId, k -> new ArrayDeque<>());
                    mapPool.addLast(bg);
                    main.getLogger().at(Level.INFO).log("[Pool] ✓ Instancia lista [" + mapId + "]. Pool=" + mapPool.size() + " en creación global=" + globalBeingCreated);
                }
            }
        });
    }
    private synchronized void markExcess(String mapId, int count) {
        Deque<InstanceManager> mapPool = pool.get(mapId);
        if (mapPool == null) return;

        int marked = 0;
        Iterator<InstanceManager> it = ((ArrayDeque<InstanceManager>) mapPool).descendingIterator();
        while (it.hasNext() && marked < count) {
            InstanceManager inst = it.next();
            if (!candidates.containsKey(inst)) {
                candidates.put(inst, System.currentTimeMillis());
                it.remove();
                marked++;
                main.getLogger().at(Level.INFO).log(
                        "[Pool] Instancia marcada candidata [" + mapId + "]. TTL=5min");
            }
        }
    }
    private synchronized void evictExpiredCandidates() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<InstanceManager, Long>> it = candidates.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<InstanceManager, Long> entry = it.next();
            if (now - entry.getValue() >= CANDIDATE_TTL_MS) {
                entry.getKey().removeInstance();
                it.remove();
                main.getLogger().at(Level.INFO).log("[Pool] Candidata destruida por TTL vencido.");
            }
        }
    }
    private void scheduleDeepClean() {
        main.getLogger().at(Level.INFO).log("[Pool] Servidor libre. Programando limpieza profunda.");

        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            synchronized (this) {
                if (!isServerGenuinelyFree()) {
                    deepCleanPending = false;
                    return;
                }

                main.getLogger().at(Level.INFO).log("[Pool] Ejecutando limpieza profunda.");

                for (InstanceManager inst : new ArrayList<>(candidates.keySet())) {
                    inst.removeInstance();
                }
                candidates.clear();

                for (Map.Entry<String, Deque<InstanceManager>> e : pool.entrySet()) {
                    for (InstanceManager inst : e.getValue()) inst.removeInstance();
                    e.getValue().clear();
                }

                cleanOrphanedWorlds();

                main.getLogger().at(Level.INFO).log("[Pool] Limpieza profunda completada.");
                deepCleanPending = false;
            }
        }, 30, TimeUnit.SECONDS);
    }
    private boolean isServerGenuinelyFree() {
        if (globalBeingCreated > 0) return false;
        return RefactorTool.getPlayersReady() < 10
            && RefactorTool.getPlayersReadyInOneMinute() < 10;
    }
    public synchronized void onPlayerBecameReady(int newReadyCount) {
        if (newReadyCount < 10) return;

        int prevCount = newReadyCount - 1;
        boolean ceilingRose = (newReadyCount / 10) > (prevCount / 10);

        if (ceilingRose || newReadyCount == 10) {
            main.getLogger().at(Level.FINE).log("[Pool] Techo subió por jugador disponible. Encolando precarga.");

            String priorityMap = popularity.getMapsSortedByPriority().stream().findFirst().orElse(null);
            if (priorityMap != null) enqueueCreation(priorityMap);
        }
    }
    public synchronized void onPlayerDisconnected(int newReadyCount) {
        int prevCount = newReadyCount + 1;
        boolean ceilingFell = (prevCount / 10) > (newReadyCount / 10);

        if (ceilingFell) {
            main.getLogger().at(Level.FINE).log("[Pool] Techo bajó por desconexión. Marcando candidata.");
            String priorityMap = popularity.getMapsSortedByPriority().stream().findFirst().orElse(null);
            if (priorityMap != null) markExcess(priorityMap, 1);
        }
    }
    private boolean canCreateInstance() {
        int inUse  = counter != null ? counter.getActiveInstanceCount() : 0;
        int inPool = pool.values().stream().mapToInt(Deque::size).sum();
        int inCand = candidates.size();

        if ((inUse + inPool + inCand + globalBeingCreated) >= MAX_TOTAL_INSTANCES) {
            main.getLogger().at(Level.WARNING).log("[Pool] Límite global alcanzado (" + MAX_TOTAL_INSTANCES + ").");
            return false;
        }
        return true;
    }
    private int poolSize(String mapId) {
        Deque<InstanceManager> q = pool.get(mapId);
        return q == null ? 0 : q.size();
    }
    private void cleanOrphanedWorlds() {
        try {
            Universe universe = Universe.get();
            java.nio.file.Path worldsPath = java.nio.file.Paths.get("universe/worlds");

            if (!java.nio.file.Files.isDirectory(worldsPath)) return;

            for (String mapId : MapListeners.getMapNames()) {
                try (java.nio.file.DirectoryStream<java.nio.file.Path> stream =
                             java.nio.file.Files.newDirectoryStream(worldsPath, mapId + "_*")) {
                    for (java.nio.file.Path dir : stream) {
                        String name = dir.getFileName().toString();
                        if (universe.getWorld(name) == null) {
                            com.hypixel.hytale.server.core.util.io.FileUtil.deleteDirectory(dir);
                            main.getLogger().at(Level.INFO).log("[Pool] Mundo huérfano eliminado: " + name);
                        }
                    }
                }
            }
        } catch (Exception e) {
            main.getLogger().at(Level.WARNING).log("[Pool] Error limpiando mundos huérfanos: " + e.getMessage());
        }
    }
    public synchronized void shutdown() {
        if (listenerTask != null) listenerTask.cancel(false);

        for (Deque<InstanceManager> q : pool.values()) {
            for (InstanceManager inst : q) inst.removeInstance();
            q.clear();
        }
        for (InstanceManager inst : candidates.keySet()) inst.removeInstance();
        candidates.clear();
    }
    public synchronized String getStatusSummary() {
        StringBuilder sb = new StringBuilder("[Pool] Estado:\n");
        for (String mapId : MapListeners.getMapNames()) {
            sb.append("  ").append(mapId)
              .append(" | pool=").append(poolSize(mapId))
              .append(" creando=").append(beingCreatedPerMap.getOrDefault(mapId, 0))
              .append("\n");
        }
        sb.append("  candidatas=").append(candidates.size())
          .append(" | techo=").append(calcCeiling())
          .append(" | globalCreando=").append(globalBeingCreated);
        return sb.toString();
    }
    public synchronized int size()           { return pool.values().stream().mapToInt(Deque::size).sum(); }
    public synchronized int getBeingCreated(){ return globalBeingCreated; }
    public interface MatchManagerInstanceCounter { int getActiveInstanceCount(); }
}
