package Tenzinn.Deathmatch.Instances;

import Tenzinn.Countertale;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;

import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;
import java.util.List;

public class InstanceManager {

    private boolean isMapLoaded = false;
    private World newWorld;

    public void preloadMap() {
        Universe universe = Universe.get();

        universe.addWorld("Test_Map_Instance", "Flat", null).thenAccept(instanceWorld -> {
            System.out.println("Arena vacía creada: " + instanceWorld.getName());

            WorldConfig config = instanceWorld.getWorldConfig();
            config.setGameTimePaused(true);

            try { config.setGameTime(java.time.Instant.parse("0001-01-01T12:00:00Z")); }
            catch (Exception e) { System.err.println("Error al establecer GameTime: " + e.getMessage()); }

            config.setBlockTicking(true);
            config.setTicking(true);
            config.setIsAllNPCFrozen(true);
            config.setSpawningNPC(false);
            config.setPvpEnabled(true);
            config.setCanUnloadChunks(false);

            System.out.println("✓ WorldConfig configurado para arena PvP");

            instanceWorld.execute(() -> {
                placePrefabInInstance(instanceWorld);
                isMapLoaded = true;
                newWorld = instanceWorld;
                System.out.println("✓ Arena completamente lista");
            });
        });
    }

    private void placePrefabInInstance(World instanceWorld) {
        try {
            CommandSender sender = ConsoleSender.INSTANCE;
            PrefabStore store = PrefabStore.get();

            BlockSelection prefab = store.getAssetPrefabFromAnyPack("Test_Map.prefab.json");

            if (prefab == null) throw new RuntimeException("Prefab 'Test_Map.prefab.json' no encontrado");

            Vector3i pos = new Vector3i(17, 50, 0);

            BlockSelection previousState = prefab.place(sender, instanceWorld, pos, null, null);

            System.out.println("✓ Prefab colocado exitosamente en " + pos);
        } catch (Exception e) {
            System.err.println("Error al colocar prefab: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void teleportPlayers(List<PlayerRef> playerRefs, Countertale main) {
        if (newWorld == null || !isMapLoaded) return;

        Transform spawnPoint = new Transform(36, 56, 0, 0, 90, 0);

        for (int i = 0; i < playerRefs.size(); i++) {
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

                        main.getLootGame(playerRef);

                    } catch (Exception e) { e.printStackTrace(); }
                });

            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public void removeInstance() {  }

    public boolean getMapLoaded () { return isMapLoaded; }
}