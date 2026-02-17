package Tenzinn.Deathmatch.Instances;

import Tenzinn.Countertale;
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

    public InstancePool(Countertale main) { this.main = main; }

    public void setCounter(MatchManagerInstanceCounter counter) { this.counter = counter; }

    public synchronized InstanceManager take() {
        InstanceManager instance = pool.pollFirst();

        if (instance != null) {
            main.getLogger().at(Level.INFO).log("[Pool] Instancia entregada. Restantes en pool: " + pool.size());
        }

        refill();
        return instance;
    }

    public synchronized void clear() {
        for (InstanceManager inst : pool) inst.removeInstance();
        pool.clear();
        main.getLogger().at(Level.INFO).log("[Pool] Pool limpiado.");
    }

    public synchronized int size()               { return pool.size(); }
    public synchronized int getBeingCreated()    { return instancesBeingCreated; }

    public synchronized void refill() {
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