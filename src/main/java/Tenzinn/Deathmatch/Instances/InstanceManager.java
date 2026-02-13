package Tenzinn.Deathmatch.Instances;

import Tenzinn.Countertale;

import Tenzinn.Tools.RefactorTool;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static java.util.concurrent.TimeUnit.SECONDS;

public class InstanceManager {

    private boolean isMapLoaded = false;
    private final Countertale main;
    private World newWorld;
    private int instanceNumber = 0;

    public InstanceManager(Countertale main) {
        this.main = main;
    }

    public void preloadMap() {
        Universe universe = Universe.get();

        instanceNumber++;
        String worldName = "Test_Map_Instance_" + instanceNumber;

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
            });
        });
    }

    private void placePrefabInInstance(World instanceWorld) {
        try {
            CommandSender sender = ConsoleSender.INSTANCE;
            PrefabStore store = PrefabStore.get();

            BlockSelection prefab = store.getAssetPrefabFromAnyPack("Test_Map.prefab.json");

            if (prefab == null) {
                throw new RuntimeException("Prefab 'Test_Map.prefab.json' no encontrado");
            }

            Vector3i pos = new Vector3i(36, 52, 2);
            prefab.place(sender, instanceWorld, pos, null, null);

        } catch (Exception e) {
            main.getLogger().at(Level.SEVERE).log("Error al colocar prefab: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void teleportPlayers(List<PlayerRef> playerRefs) {
        if (newWorld == null || !isMapLoaded) return;

        Transform[] spawns = {
                new Transform(36, 52, 2),
                new Transform(14, 52, -9),
                new Transform(5, 52, -4),
                new Transform(4, 52, 2),
                new Transform(8, 52, 9),
                new Transform(16, 52, 10),
                new Transform(8, 52, 4),
                new Transform(1, 56, 1),
                new Transform(21, 56, -11),
                new Transform(-2, 59, 2)
        };


        for (int i = 0; i < playerRefs.size(); i++) {
            Transform spawnPoint = spawns[i];

            PlayerRef playerRef = playerRefs.get(i);

            try {
                UUID playerUUID = playerRef.getUuid();
                PlayerRef updatedPlayerRef = Universe.get().getPlayer(playerUUID);

                if (updatedPlayerRef == null || updatedPlayerRef.getReference() == null) continue;

                Ref<EntityStore> ref = updatedPlayerRef.getReference();
                World currentWorld = Universe.get().getWorld(updatedPlayerRef.getWorldUuid());

                if (currentWorld == null) continue;

                updatedPlayerRef.sendMessage(Message.raw("Teleportando a la arena..."));

                currentWorld.execute(() -> {
                    try {
                        Store<EntityStore> store = ref.getStore();

                        Teleport teleport = Teleport.createForPlayer(newWorld, spawnPoint);
                        store.addComponent(ref, Teleport.getComponentType(), teleport);
                    } catch (Exception e) { e.printStackTrace(); }
                });
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public void removeInstance() {
        Universe universe = Universe.get();
        String worldName = "Test_Map_Instance_" + instanceNumber;
        World instanceWorld = universe.getWorld(worldName);

        if (instanceWorld != null) {
            universe.removeWorld(worldName);
            main.getLogger().at(Level.INFO).log("Mundo " + worldName + " removido");

            instanceNumber--;
            isMapLoaded = false;
            newWorld = null;
        }
    }

    public boolean getMapLoaded() {
        return isMapLoaded;
    }
}