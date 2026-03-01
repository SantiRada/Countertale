package Tenzinn.Deathmatch.Instances;

import Tenzinn.Countertale;

import Tenzinn.Listeners.MessageListeners;
import Tenzinn.Tools.RefactorTool;
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

    private static final java.util.concurrent.atomic.AtomicInteger instanceCounter = new java.util.concurrent.atomic.AtomicInteger(0);
    private int instanceNumber;

    public InstanceManager(Countertale main) {
        this.main = main;
    }

    public void preloadMap(Runnable onMapReady) {
        Universe universe = Universe.get();

        instanceNumber = instanceCounter.incrementAndGet();
        String worldName = "Dust2_Instance_" + instanceNumber;

        universe.addWorld(worldName, "Flat", null).thenAccept(instanceWorld -> {
            main.getLogger().at(Level.INFO).log("Arena vacía creada: " + instanceWorld.getName());

            WorldConfig config = instanceWorld.getWorldConfig();
            config.setDeleteOnRemove(true);
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
                newWorld = instanceWorld;
                isMapLoaded = true;

                main.getLogger().at(Level.INFO).log("✓ Instancia lista para jugar");

                if (onMapReady != null) onMapReady.run();
            });
        });
    }

    private void placePrefabInInstance(World instanceWorld) {
        try {
            PrefabStore store = PrefabStore.get();

            BlockSelection prefab = store.getAssetPrefabFromAnyPack("Dust2.prefab.json");

            if (prefab == null) { throw new RuntimeException("Prefab 'Dust2.prefab.json' no encontrado"); }

            BlockSelection cleanPrefab = new BlockSelection();
            cleanPrefab.setPosition(0, 52, 0);

            prefab.forEachBlock((x, y, z, block) -> {
                cleanPrefab.addBlockAtLocalPos(x, y, z, block.blockId(),
                        block.rotation(), block.filler(), block.supportValue());
            });

            prefab.forEachFluid((x, y, z, fluidId, level) -> {
                cleanPrefab.addFluidAtLocalPos(x, y, z, fluidId, level);
            });

            Vector3i pos = new Vector3i(0, 52, 0);

            WorldConfig config = instanceWorld.getWorldConfig();
            config.setBlockTicking(false);
            config.setTicking(false);

            long startTime = System.currentTimeMillis();
            cleanPrefab.placeNoReturn(instanceWorld, pos, null);
            long elapsed = System.currentTimeMillis() - startTime;

            config.setBlockTicking(true);
            config.setTicking(true);

            main.getLogger().at(Level.INFO).log("✓ Prefab colocado en " + elapsed + "ms");

        } catch (Exception e) {
            main.getLogger().at(Level.SEVERE).log("Error al colocar prefab: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void teleportPlayers(List<PlayerRef> playerRefs) {
        main.getLogger().at(Level.INFO).log("=== INICIO TELEPORT ===");
        main.getLogger().at(Level.INFO).log("Jugadores a TP: " + playerRefs.size());
        main.getLogger().at(Level.INFO).log("Mundo destino: " + (newWorld != null ? newWorld.getName() : "NULL"));
        main.getLogger().at(Level.INFO).log("Mapa cargado: " + isMapLoaded);

        if (newWorld == null || !isMapLoaded) {
            main.getLogger().at(Level.WARNING).log("No se puede teletransportar: mapa no cargado");
            return;
        }

        ArrayList<Vector3d> spawns = RefactorTool.getSpawns();

        for (int i = 0; i < playerRefs.size(); i++) {
            Vector3d spawnPos = spawns.get(i % spawns.size());
            Transform spawnPoint = new Transform(spawnPos.x, spawnPos.y, spawnPos.z);

            PlayerRef playerRef = playerRefs.get(i);

            try {
                UUID playerUUID = playerRef.getUuid();
                PlayerRef updatedPlayerRef = Universe.get().getPlayer(playerUUID);

                if (updatedPlayerRef == null || updatedPlayerRef.getReference() == null) continue;

                Ref<EntityStore> ref = updatedPlayerRef.getReference();
                World currentWorld = Universe.get().getWorld(updatedPlayerRef.getWorldUuid());

                if (currentWorld == null) continue;

                updatedPlayerRef.sendMessage(Message.raw(MessageListeners.get(MessageListeners.MessageKey.CHAT_TELEPORTING_GAME)));

                currentWorld.execute(() -> {
                    try {
                        Store<EntityStore> store = currentWorld.getEntityStore().getStore();
                        Teleport teleport = Teleport.createForPlayer(newWorld, spawnPoint);
                        store.addComponent(ref, Teleport.getComponentType(), teleport);
                        main.getLogger().at(Level.INFO).log("✓ Jugador teletransportado: " + playerUUID);
                    } catch (Exception e) {
                        main.getLogger().at(Level.SEVERE).log("Error al teletransportar: " + e.getMessage());
                        e.printStackTrace();
                    }
                });

            } catch (Exception e) {
                main.getLogger().at(Level.SEVERE).log("Error en teleportPlayers: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void removeInstance() {
        Universe universe = Universe.get();
        String worldName = "Dust2_Instance_" + instanceNumber;
        World instanceWorld = universe.getWorld(worldName);

        if (instanceWorld != null) {
            universe.removeWorld(worldName);

            isMapLoaded = false;
            newWorld = null;
        }
    }

    public boolean getMapLoaded() { return isMapLoaded; }
}