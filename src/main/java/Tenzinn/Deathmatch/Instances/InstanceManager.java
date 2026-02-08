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
import java.util.logging.Level;

public class InstanceManager {

    private boolean isMapLoaded = false;
    private World newWorld;
    private final Countertale main;

    // ✅ CORRECCIÓN: Constructor que recibe Countertale
    public InstanceManager(Countertale main) {
        this.main = main;
    }

    public void preloadMap() {
        Universe universe = Universe.get();

        universe.addWorld("Test_Map_Instance", "Flat", null).thenAccept(instanceWorld -> {
            main.getLogger().at(Level.INFO).log("Arena vacía creada: " + instanceWorld.getName());

            WorldConfig config = instanceWorld.getWorldConfig();
            config.setGameTimePaused(true);

            try {
                config.setGameTime(java.time.Instant.parse("0001-01-01T12:00:00Z"));
            } catch (Exception e) {
                main.getLogger().at(Level.SEVERE).log("Error al establecer GameTime: " + e.getMessage());
            }

            config.setBlockTicking(true);
            config.setTicking(true);
            config.setIsAllNPCFrozen(true);
            config.setSpawningNPC(false);
            config.setPvpEnabled(true);
            config.setCanUnloadChunks(false);

            main.getLogger().at(Level.INFO).log("✓ WorldConfig configurado para arena PvP");

            instanceWorld.execute(() -> {
                placePrefabInInstance(instanceWorld);
                isMapLoaded = true;
                newWorld = instanceWorld;
                main.getLogger().at(Level.INFO).log("✓ Arena completamente lista");
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

            main.getLogger().at(Level.INFO).log("✓ Prefab colocado exitosamente en " + pos);
        } catch (Exception e) {
            main.getLogger().at(Level.SEVERE).log("Error al colocar prefab: " + e.getMessage());
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

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void removeInstance() {
        main.getLogger().at(Level.INFO).log("🗑️ removeInstance() llamado");

        Universe universe = Universe.get();
        World instanceWorld = universe.getWorld("Test_Map_Instance");

        if (instanceWorld != null) {
            main.getLogger().at(Level.INFO).log("✅ Instancia encontrada, removiendo del universo...");
            universe.removeWorld("Test_Map_Instance");
            main.getLogger().at(Level.INFO).log("✅ Instancia removida de la memoria");

            // ✅ Eliminar archivos del disco - LA RUTA CORRECTA ES universe/
            boolean deleted = false;
            String[] possiblePaths = {
                    "universe/Test_Map_Instance",      // ✅ RUTA CORRECTA
                    "universe/worlds/Test_Map_Instance",
                    "./universe/Test_Map_Instance",
                    "Worlds/Test_Map_Instance",
                    "worlds/Test_Map_Instance"
            };

            for (String pathString : possiblePaths) {
                try {
                    java.nio.file.Path worldPath = java.nio.file.Paths.get(pathString);
                    main.getLogger().at(Level.INFO).log("🔍 Buscando en: " + worldPath.toAbsolutePath());

                    if (java.nio.file.Files.exists(worldPath)) {
                        main.getLogger().at(Level.INFO).log("✅ ¡Carpeta encontrada! Eliminando archivos...");
                        deleteDirectory(worldPath);
                        main.getLogger().at(Level.INFO).log("✅ Archivos del disco eliminados correctamente!");
                        deleted = true;
                        break;
                    }
                } catch (Exception e) {
                    main.getLogger().at(Level.WARNING).log("Error al intentar eliminar " + pathString + ": " + e.getMessage());
                }
            }

            if (!deleted) {
                main.getLogger().at(Level.WARNING).log("⚠️ Carpeta del mundo no encontrada");
            }

            // Resetear estado
            isMapLoaded = false;
            newWorld = null;
            main.getLogger().at(Level.INFO).log("✅ Proceso de eliminación completado!");
        } else {
            main.getLogger().at(Level.WARNING).log("⚠️ Instancia 'Test_Map_Instance' NO encontrada en el universo!");
        }
    }

    // ✅ NUEVO: Método helper para eliminar directorios recursivamente
    private void deleteDirectory(java.nio.file.Path path) throws java.io.IOException {
        if (java.nio.file.Files.isDirectory(path)) {
            try (java.util.stream.Stream<java.nio.file.Path> entries = java.nio.file.Files.list(path)) {
                entries.forEach(entry -> {
                    try {
                        deleteDirectory(entry);
                    } catch (java.io.IOException e) {
                        main.getLogger().at(Level.WARNING).log("Error eliminando: " + entry + " - " + e.getMessage());
                    }
                });
            }
        }
        java.nio.file.Files.delete(path);
    }

    public boolean getMapLoaded() {
        return isMapLoaded;
    }
}