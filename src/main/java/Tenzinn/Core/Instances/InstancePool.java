package Tenzinn.Core.Instances;

import Tenzinn.Countertale;
import Tenzinn.Core.Listeners.MapListeners;
import Tenzinn.Core.Tools.RefactorTool;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.Universe;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Pool de instancias precargadas indexado por mapa.
 *
 * Responsabilidades:
 *  - Mantener instancias precargadas hasta el techo calculado
 *  - Rate-limit de creación: máximo 2 instancias en creación simultánea (global)
 *  - Asignar instancias al matchmaking y reponerse inmediatamente
 *  - Marcar excedentes como candidatas con TTL de 5 minutos antes de destruirlas
 *  - Ciclo periódico de reconciliación cada 15 segundos
 *  - Limpieza profunda cuando el servidor está genuinamente libre
 */
public class InstancePool {

    // ── Constantes ────────────────────────────────────────────────────────────
    public  static final int MAX_TOTAL_INSTANCES     = 11;
    private static final int MAX_CREATION_RATE       = 2;    // máx simultáneas en creación
    private static final long CANDIDATE_TTL_MS       = 5 * 60 * 1000L; // 5 minutos
    private static final long LISTENER_INTERVAL_S    = 15;
    private static final long LISTENER_INITIAL_S     = 10;

    // ── Estado ────────────────────────────────────────────────────────────────
    private final Countertale main;
    private final MapPopularityTracker popularity;

    /** Instancias precargadas listas para ser consumidas, por mapa. */
    private final Map<String, Deque<InstanceManager>> pool = new ConcurrentHashMap<>();

    /** Instancias candidatas a destrucción: mapa → (instancia, timestamp cuando fue marcada). */
    private final Map<InstanceManager, Long> candidates = new LinkedHashMap<>();

    /** Cuántas instancias están siendo creadas en este momento (global, rate-limited). */
    private int globalBeingCreated = 0;

    /** Cuántas instancias están siendo creadas por mapa. */
    private final Map<String, Integer> beingCreatedPerMap = new ConcurrentHashMap<>();

    private MatchManagerInstanceCounter counter;
    private ScheduledFuture<?> listenerTask;

    private boolean universeReady    = false;
    private boolean deepCleanPending = false; // evita loop de limpieza en períodos largos de baja actividad

    // ── Constructor ───────────────────────────────────────────────────────────

    public InstancePool(Countertale main) {
        this.main       = main;
        this.popularity = new MapPopularityTracker();

        for (String mapId : MapListeners.getMapNames()) {
            pool.put(mapId, new ArrayDeque<>());
            beingCreatedPerMap.put(mapId, 0);
        }
    }

    // ── Inicialización ────────────────────────────────────────────────────────

    public void markReady() {
        cleanOrphanedWorlds();
        universeReady = true;

        // Precargar 1 instancia por mapa de forma incondicional al arranque.
        // Esto garantiza que haya instancias disponibles antes de que llegue
        // el primer jugador, sin depender del techo (que sería 0 con 0 lobby players).
        for (String mapId : MapListeners.getMapNames()) {
            enqueueCreation(mapId);
        }

        // Iniciar el ciclo periódico DESPUÉS de encolar las precargas de arranque.
        // Se retrasa LISTENER_INITIAL_S para que las instancias tengan tiempo de
        // cargarse antes de que el ciclo evalúe si hay excedentes.
        listenerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(
                this::runListenerCycle,
                LISTENER_INITIAL_S, LISTENER_INTERVAL_S, TimeUnit.SECONDS
        );

        main.getLogger().at(Level.INFO).log("[Pool] Inicializado con mapas: " + MapListeners.getMapNames());
    }

    public void setCounter(MatchManagerInstanceCounter counter) { this.counter = counter; }

    public MapPopularityTracker getPopularity() { return popularity; }

    // ── API de matchmaking ────────────────────────────────────────────────────

    /**
     * Entrega una instancia del mapa solicitado.
     *
     * <p>Si hay una instancia precargada disponible, {@code whenReady} se ejecuta
     * <em>inmediatamente</em> antes de retornar (la instancia ya estaba lista).
     * Si no hay ninguna (fallback), se crea una en caliente de forma asíncrona y
     * {@code whenReady} se ejecuta <em>cuando el mapa termine de cargarse</em>,
     * evitando el teleport prematuro.</p>
     *
     * <p>En ambos casos se encola una nueva precarga de reemplazo (pool autoregenerativo).</p>
     *
     * @param mapId     slug del mapa (ej. "dust2")
     * @param whenReady callback a ejecutar en cuanto la instancia esté lista; puede ser null
     * @return la instancia asignada (puede no estar cargada todavía si es fallback)
     */
    public synchronized InstanceManager take(String mapId, Runnable whenReady) {
        Deque<InstanceManager> mapPool = pool.get(mapId);
        InstanceManager instance = (mapPool != null) ? mapPool.pollFirst() : null;

        if (instance != null) {
            // Instancia precargada disponible: ya está lista, ejecutar callback ahora
            main.getLogger().at(Level.INFO).log(
                    "[Pool] Instancia entregada [" + mapId + "]. Restantes: " + poolSize(mapId));
            if (whenReady != null) whenReady.run();
        } else {
            // Fallback: crear en caliente y ejecutar callback cuando termine de cargar
            main.getLogger().at(Level.WARNING).log(
                    "[Pool] FALLBACK en mapa '" + mapId + "': creando instancia en caliente.");
            popularity.recordFallback(mapId);
            instance = new InstanceManager(main, mapId);
            instance.preloadMap(whenReady); // whenReady se ejecutará cuando isMapLoaded = true
        }

        // Reposición inmediata: encolar una nueva precarga para mantener el pool
        enqueueCreation(mapId);

        return instance;
    }

    /**
     * Notifica al pool que una partida terminó.
     * Debe llamarse desde MatchManager al finalizar una partida.
     */
    public synchronized void onMatchFinished(String mapId, InstanceManager instance) {
        popularity.recordMatchFinished(mapId);
        instance.removeInstance();

        // Recalcular techo por si ahora hay más/menos jugadores disponibles
        runListenerCycle();
    }

    // ── Ciclo periódico (listener) ────────────────────────────────────────────

    private synchronized void runListenerCycle() {
        if (!universeReady) return;

        int ceiling = calcCeiling();

        main.getLogger().at(Level.INFO).log(
                "[Pool] Ciclo listener | techo=" + ceiling
                + " playersReady=" + RefactorTool.getPlayersReady()
                + " imminent=" + RefactorTool.getPlayersReadyInOneMinute());

        if (ceiling == 0 && RefactorTool.getPlayersReadyInOneMinute() < 10) {
            // Servidor genuinamente libre
            if (!deepCleanPending) {
                deepCleanPending = true;
                scheduleDeepClean();
            }
            return;
        }

        deepCleanPending = false;

        // Distribuir instancias ideales por mapa según popularidad
        Map<String, Integer> ideal = popularity.distribute(ceiling);

        for (String mapId : MapListeners.getMapNames()) {
            int idealCount = ideal.getOrDefault(mapId, 0);
            int current    = poolSize(mapId) + beingCreatedPerMap.getOrDefault(mapId, 0);
            int missing    = idealCount - current;

            if (missing > 0) {
                for (int i = 0; i < missing; i++) enqueueCreation(mapId);
            } else if (missing < 0) {
                // Marcar excedente como candidata con TTL
                markExcess(mapId, -missing);
            }
        }

        // Procesar candidatas cuyo TTL venció
        evictExpiredCandidates();
    }

    // ── Techo ─────────────────────────────────────────────────────────────────

    /**
     * techo = floor((playersReady + inminentes_confiables) / 10)
     * inminentes_confiables = floor(playersReadyInOneMinute / 10) * 10
     * (el residuo se descarta: jugadores sueltos que no forman partida)
     */
    private int calcCeiling() {
        int ready     = RefactorTool.getPlayersReady();
        int imminent  = RefactorTool.getPlayersReadyInOneMinute();
        int confReady = (imminent / 10) * 10;
        return (ready + confReady) / 10;
    }

    // ── Creación con rate limiting ────────────────────────────────────────────

    /**
     * Encola la creación de una instancia para el mapa dado.
     * Respeta el límite global de MAX_CREATION_RATE instancias creándose a la vez.
     */
    private synchronized void enqueueCreation(String mapId) {
        if (!universeReady) return;
        if (!canCreateInstance())  return;
        if (globalBeingCreated >= MAX_CREATION_RATE) {
            main.getLogger().at(Level.FINE).log(
                    "[Pool] Rate limit alcanzado (" + MAX_CREATION_RATE + "). Se creará en el próximo ciclo.");
            return;
        }

        createBackgroundInstance(mapId);
    }

    private void createBackgroundInstance(String mapId) {
        globalBeingCreated++;
        beingCreatedPerMap.merge(mapId, 1, Integer::sum);

        main.getLogger().at(Level.INFO).log(
                "[Pool] Iniciando preload [" + mapId + "]. En creación global: " + globalBeingCreated);

        InstanceManager bg = new InstanceManager(main, mapId);
        bg.preloadMap(() -> {
            synchronized (this) {
                globalBeingCreated--;
                beingCreatedPerMap.merge(mapId, -1, Integer::sum);

                // Si fue marcada candidata mientras se creaba, la destruimos directamente
                if (candidates.containsKey(bg)) {
                    candidates.remove(bg);
                    bg.removeInstance();
                    main.getLogger().at(Level.INFO).log(
                            "[Pool] Instancia creada pero ya no necesaria [" + mapId + "]. Destruida.");
                } else {
                    Deque<InstanceManager> mapPool = pool.computeIfAbsent(mapId, k -> new ArrayDeque<>());
                    mapPool.addLast(bg);
                    main.getLogger().at(Level.INFO).log(
                            "[Pool] ✓ Instancia lista [" + mapId + "]. Pool=" + mapPool.size()
                            + " en creación global=" + globalBeingCreated);
                }
            }
        });
    }

    // createHotInstance fue inlined en take(mapId, whenReady) para poder pasar el callback correcto.

    // ── Candidatas y TTL ──────────────────────────────────────────────────────

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

    // ── Limpieza profunda ─────────────────────────────────────────────────────

    private void scheduleDeepClean() {
        main.getLogger().at(Level.INFO).log("[Pool] Servidor libre. Programando limpieza profunda.");

        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            synchronized (this) {
                if (!isServerGenuinelyFree()) {
                    deepCleanPending = false;
                    return; // llegó actividad antes de que corriéramos
                }

                main.getLogger().at(Level.INFO).log("[Pool] Ejecutando limpieza profunda.");

                // 1. Destruir todas las candidatas sin esperar TTL
                for (InstanceManager inst : new ArrayList<>(candidates.keySet())) {
                    inst.removeInstance();
                }
                candidates.clear();

                // 2. Destruir instancias del pool también (no hay demanda)
                for (Map.Entry<String, Deque<InstanceManager>> e : pool.entrySet()) {
                    for (InstanceManager inst : e.getValue()) inst.removeInstance();
                    e.getValue().clear();
                }

                // 3. Limpiar mundos huérfanos en disco
                cleanOrphanedWorlds();

                main.getLogger().at(Level.INFO).log("[Pool] Limpieza profunda completada.");
                deepCleanPending = false; // permitir próxima limpieza si el server vuelve a quedar libre
            }
        }, 30, TimeUnit.SECONDS); // pequeña demora para evitar false-positives
    }

    private boolean isServerGenuinelyFree() {
        // No consideramos el servidor libre mientras haya instancias cargándose:
        // las precargas de arranque (o reposiciones) aún no han terminado.
        if (globalBeingCreated > 0) return false;
        return RefactorTool.getPlayersReady() < 10
            && RefactorTool.getPlayersReadyInOneMinute() < 10;
    }

    // ── Eventos de jugadores (ajuste inmediato sin esperar ciclo) ─────────────

    /**
     * Llamar cuando un jugador pasa a estar disponible (termina partida).
     * Solo actúa si el incremento cruzó un múltiplo de 10 (techo subió).
     *
     * @param newReadyCount nuevo valor total de getPlayersReady() ya actualizado
     */
    public synchronized void onPlayerBecameReady(int newReadyCount) {
        if (newReadyCount < 10) return; // mínimo para una partida

        // ¿Cruzamos un múltiplo de 10?
        int prevCount = newReadyCount - 1;
        boolean ceilingRose = (newReadyCount / 10) > (prevCount / 10);

        if (ceilingRose || newReadyCount == 10) {
            main.getLogger().at(Level.FINE).log(
                    "[Pool] Techo subió por jugador disponible. Encolando precarga.");
            // Encolar una instancia del mapa con más prioridad
            String priorityMap = popularity.getMapsSortedByPriority().stream().findFirst().orElse(null);
            if (priorityMap != null) enqueueCreation(priorityMap);
        }
    }

    /**
     * Llamar cuando un jugador se desconecta.
     * Si el techo bajó, el excedente pasa a candidata con TTL.
     *
     * @param newReadyCount nuevo valor total de getPlayersReady() ya actualizado
     */
    public synchronized void onPlayerDisconnected(int newReadyCount) {
        int prevCount = newReadyCount + 1;
        boolean ceilingFell = (prevCount / 10) > (newReadyCount / 10);

        if (ceilingFell) {
            main.getLogger().at(Level.FINE).log(
                    "[Pool] Techo bajó por desconexión. Marcando candidata.");
            // Marcar 1 excedente en el mapa más popular (más probable que sobre)
            String priorityMap = popularity.getMapsSortedByPriority().stream().findFirst().orElse(null);
            if (priorityMap != null) markExcess(priorityMap, 1);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean canCreateInstance() {
        int inUse  = counter != null ? counter.getActiveInstanceCount() : 0;
        int inPool = pool.values().stream().mapToInt(Deque::size).sum();
        int inCand = candidates.size();

        if ((inUse + inPool + inCand + globalBeingCreated) >= MAX_TOTAL_INSTANCES) {
            main.getLogger().at(Level.WARNING).log(
                    "[Pool] Límite global alcanzado (" + MAX_TOTAL_INSTANCES + ").");
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

            // Limpiar mundos huérfanos para cada mapa conocido (patrón: <mapId>_<uuid>)
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

    // ── Shutdown ──────────────────────────────────────────────────────────────

    public synchronized void shutdown() {
        if (listenerTask != null) listenerTask.cancel(false);

        for (Deque<InstanceManager> q : pool.values()) {
            for (InstanceManager inst : q) inst.removeInstance();
            q.clear();
        }
        for (InstanceManager inst : candidates.keySet()) inst.removeInstance();
        candidates.clear();
    }

    // ── Diagnóstico ───────────────────────────────────────────────────────────

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

    // ── Inner interface ───────────────────────────────────────────────────────

    public interface MatchManagerInstanceCounter { int getActiveInstanceCount(); }
}
