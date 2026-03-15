package Tenzinn.Deathmatch.Content.Instances;

import Tenzinn.Countertale;
import com.hypixel.hytale.server.core.universe.Universe;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Level;

public class InstancePool {

    private static final int BACKGROUND_BUFFER   = 2;
    public  static final int MAX_TOTAL_INSTANCES = 11;

    private final Countertale main;
    private final Deque<InstanceManager> pool = new ArrayDeque<>();

    private int instancesBeingCreated = 0;
    private MatchManagerInstanceCounter counter;

    private boolean universeReady = false;

    public InstancePool(Countertale main) { this.main = main; }

    public void markReady() {
        cleanOrphanedWorlds();
        universeReady = true;
        refill();
    }

    public void setCounter(MatchManagerInstanceCounter counter) { this.counter = counter; }

    public synchronized InstanceManager take() {
        InstanceManager instance = pool.pollFirst();

        if (instance != null) {
            main.getLogger().at(Level.INFO).log("[Pool] Instancia entregada. Restantes en pool: " + pool.size());
        }

        refill();
        return instance;
    }

    private void cleanOrphanedWorlds() {
        try {
            Universe universe = Universe.get();
            Path worldsPath = java.nio.file.Paths.get("universe/worlds"); // ajustar al path real

            if (!java.nio.file.Files.isDirectory(worldsPath)) return;

            try (java.nio.file.DirectoryStream<java.nio.file.Path> stream =
                         java.nio.file.Files.newDirectoryStream(worldsPath, "dm_*")) {
                for (java.nio.file.Path dir : stream) {
                    String name = dir.getFileName().toString();
                    // Solo borrar si NO está cargado en memoria
                    if (universe.getWorld(name) == null) {
                        com.hypixel.hytale.server.core.util.io.FileUtil.deleteDirectory(dir);
                        main.getLogger().at(Level.INFO).log("[Pool] 🧹 Mundo huérfano eliminado: " + name);
                    }
                }
            }
        } catch (Exception e) {
            main.getLogger().at(Level.WARNING).log("[Pool] Error limpiando mundos huérfanos: " + e.getMessage());
        }
    }

    public synchronized int size()               { return pool.size(); }
    public synchronized int getBeingCreated()    { return instancesBeingCreated; }

    public synchronized void refill() {
        if (!universeReady) return;

        int needed = BACKGROUND_BUFFER - pool.size() - instancesBeingCreated;
        if (needed <= 0) return;

        for (int i = 0; i < needed; i++) {
            if (!canCreateInstance()) {
                main.getLogger().at(Level.WARNING).log("[Pool] Límite global alcanzado (" + MAX_TOTAL_INSTANCES + "). No se crean más instancias de fondo.");
                break;
            }
            createBackgroundInstance();
        }
    }

    private void createBackgroundInstance() {
        instancesBeingCreated++;
        main.getLogger().at(Level.INFO).log("[Pool] Iniciando preload de fondo. En creación: " + instancesBeingCreated + " | En pool: " + pool.size());

        InstanceManager bgInstance = new InstanceManager(main);
        bgInstance.preloadMap(() -> {
            synchronized (this) {
                instancesBeingCreated--;
                pool.addLast(bgInstance);
                main.getLogger().at(Level.INFO).log("[Pool] ✓ Instancia lista. Pool: " + pool.size() + " | En creación: " + instancesBeingCreated);
            }
        });
    }

    private boolean canCreateInstance() {
        int inUse      = counter != null ? counter.getActiveInstanceCount() : 0;
        int inPool     = pool.size();
        int inCreation = instancesBeingCreated;
        return (inUse + inPool + inCreation) < MAX_TOTAL_INSTANCES;
    }

    public interface MatchManagerInstanceCounter { int getActiveInstanceCount(); }
}