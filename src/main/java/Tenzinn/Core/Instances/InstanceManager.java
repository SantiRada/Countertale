package Tenzinn.Core.Instances;

import Tenzinn.Countertale;
import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.Listeners.MessageListeners;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;

import java.util.ArrayList;
import java.util.UUID;
import java.util.List;
import java.util.logging.Level;

public class InstanceManager {

    private boolean isMapLoaded = false;
    private final Countertale main;
    private World newWorld;
    private String worldName;

    /** Mapa que contiene esta instancia (ej. "dust2", "assault"). */
    private final String mapId;

    // ── Constructor ───────────────────────────────────────────────────────────

    public InstanceManager(Countertale main, String mapId) {
        this.main  = main;
        this.mapId = mapId.toLowerCase();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getMapId()        { return mapId; }
    public boolean getMapLoaded()   { return isMapLoaded; }

    // ── Preload ───────────────────────────────────────────────────────────────

    /**
     * Precarga el mapa en un mundo nuevo y llama a onMapReady cuando termina.
     * El nombre del mundo se genera como: {@code <mapId>_<uuid_corto>}
     * para distinguir instancias del mismo mapa.
     *
     * @param onMapReady callback invocado cuando la instancia está lista (puede ser null en fallbacks)
     */
    public void preloadMap(Runnable onMapReady) {
        Universe universe = Universe.get();

        this.worldName = mapId + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        universe.addWorld(this.worldName, "Flat", null).thenAccept(instanceWorld -> {
            main.getLogger().at(Level.INFO).log("[Instance] Arena vacía creada: " + instanceWorld.getName());

            WorldConfig config = instanceWorld.getWorldConfig();
            config.setDeleteOnRemove(true);
            config.markChanged();
            config.setGameTimePaused(true);

            try { config.setGameTime(java.time.Instant.parse("0001-01-01T12:00:00Z")); }
            catch (Exception e) { main.getLogger().at(Level.SEVERE).log("Error al establecer GameTime: " + e.getMessage()); }

            config.setBlockTicking(true);
            config.setTicking(true);
            config.setIsAllNPCFrozen(true);
            config.setSpawningNPC(false);
            config.setPvpEnabled(true);
            config.setCanUnloadChunks(false);

            instanceWorld.execute(() -> {
                placePrefabInInstance(instanceWorld);
                newWorld     = instanceWorld;
                isMapLoaded  = true;

                main.getLogger().at(Level.INFO).log("[Instance] ✓ Instancia lista [" + mapId + "]: " + worldName);

                if (onMapReady != null) onMapReady.run();
            });
        });
    }

    // ── Prefab ────────────────────────────────────────────────────────────────

    /**
     * Resuelve el nombre del archivo de prefab a partir del mapId.
     * Convención: {@code <MapId con primera letra en mayúscula>.prefab.json}
     * Ejemplos:
     *   "dust2"   → "Dust2.prefab.json"
     *   "assault" → "Assault.prefab.json"
     */
    private String resolvePrefabName() {
        String capitalized = Character.toUpperCase(mapId.charAt(0)) + mapId.substring(1);
        return capitalized + ".prefab.json";
    }

    private void placePrefabInInstance(World instanceWorld) {
        try {
            PrefabStore store = PrefabStore.get();
            String prefabName = resolvePrefabName();

            BlockSelection prefab = store.getAssetPrefabFromAnyPack(prefabName);
            if (prefab == null) { throw new RuntimeException("Prefab '" + prefabName + "' no encontrado"); }

            BlockSelection cleanPrefab = new BlockSelection();
            cleanPrefab.setPosition(0, 52, 0);

            prefab.forEachBlock((x, y, z, block) -> {
                cleanPrefab.addBlockAtLocalPos(x, y, z, block.blockId(), block.rotation(), block.filler(), block.supportValue());
            });
            prefab.forEachFluid(cleanPrefab::addFluidAtLocalPos);

            Vector3i pos = new Vector3i(0, 52, 0);

            WorldConfig config = instanceWorld.getWorldConfig();
            config.setBlockTicking(false);
            config.setTicking(false);

            long start = System.currentTimeMillis();
            cleanPrefab.placeNoReturn(instanceWorld, pos, null);
            long elapsed = System.currentTimeMillis() - start;

            config.setBlockTicking(true);
            config.setTicking(true);

            main.getLogger().at(Level.INFO).log("[Instance] ✓ Prefab '" + prefabName + "' colocado en " + elapsed + "ms");

        } catch (Exception e) {
            main.getLogger().at(Level.SEVERE).log("[Instance] Error al colocar prefab: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Teleport ──────────────────────────────────────────────────────────────

    public void teleportPlayers(List<PlayerRef> playerRefs) {
        main.getLogger().at(Level.INFO).log("[Instance] === INICIO TELEPORT [" + mapId + "] ===");

        if (newWorld == null || !isMapLoaded) {
            main.getLogger().at(Level.WARNING).log("[Instance] No se puede teletransportar: mapa no cargado");
            return;
        }

        ArrayList<Vector3d> spawns = RefactorTool.getSpawns(mapId, RefactorTool.getModeForPlayer(playerRefs.getFirst()));

        for (int i = 0; i < playerRefs.size(); i++) {
            Vector3d spawnPos = spawns.get(i % spawns.size());
            Transform spawnPoint = new Transform(spawnPos.x, spawnPos.y, spawnPos.z);
            PlayerRef playerRef  = playerRefs.get(i);

            try {
                UUID playerUUID = playerRef.getUuid();
                PlayerRef updatedRef = Universe.get().getPlayer(playerUUID);

                if (updatedRef == null || updatedRef.getReference() == null) continue;

                Ref<EntityStore> ref   = updatedRef.getReference();
                assert updatedRef.getWorldUuid() != null;
                World currentWorld = Universe.get().getWorld(updatedRef.getWorldUuid());

                if (currentWorld == null) continue;

                updatedRef.sendMessage(Message.raw(MessageListeners.get(MessageListeners.MessageKey.CHAT_TELEPORTING_GAME)));

                currentWorld.execute(() -> {
                    try {
                        Store<EntityStore> store = currentWorld.getEntityStore().getStore();
                        Teleport teleport = Teleport.createForPlayer(newWorld, spawnPoint);
                        store.addComponent(ref, Teleport.getComponentType(), teleport);
                        main.getLogger().at(Level.INFO).log("[Instance] ✓ Jugador teletransportado: " + playerUUID);
                    } catch (Exception e) {
                        main.getLogger().at(Level.SEVERE).log("[Instance] Error al teletransportar: " + e.getMessage());
                        e.printStackTrace();
                    }
                });

            } catch (Exception e) {
                main.getLogger().at(Level.SEVERE).log("[Instance] Error en teleportPlayers: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // ── Destrucción ───────────────────────────────────────────────────────────

    public void removeInstance() {
        if (worldName == null) return;

        Universe universe = Universe.get();
        World instanceWorld = universe.getWorld(worldName);

        if (instanceWorld != null) {
            try {
                universe.removeWorld(worldName);
                main.getLogger().at(Level.INFO).log("[Instance] ✓ Mundo eliminado: " + worldName);
            } catch (Exception e) {
                main.getLogger().at(Level.WARNING).log("[Instance] Error eliminando mundo: " + e.getMessage());
            }
        }

        isMapLoaded = false;
        newWorld    = null;
        worldName   = null;
    }
}
