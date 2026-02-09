package Tenzinn.Deathmatch.Instances;

import Tenzinn.Countertale;
import Tenzinn.Deathmatch.UI.DeathmatchHUD;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
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

import java.awt.*;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import static java.util.concurrent.TimeUnit.SECONDS;

public class InstanceManager {

    private boolean isMapLoaded = false;
    private World newWorld;
    private final Countertale main;

    private int instanceNumber = 0;

    public InstanceManager(Countertale main) { this.main = main; }

    public void preloadMap() {
        Universe universe = Universe.get();

        instanceNumber = ++instanceNumber;
        String worldName = "Test_Map_Instance_" + instanceNumber;

        universe.addWorld(worldName, "Flat", null).thenAccept(instanceWorld -> {
            main.getLogger().at(Level.INFO).log("Arena vacía creada: " + instanceWorld.getName());

            WorldConfig config = instanceWorld.getWorldConfig();
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

            if (prefab == null) throw new RuntimeException("Prefab 'Test_Map.prefab.json' no encontrado");

            Vector3i pos = new Vector3i(17, 50, 0);

            BlockSelection previousState = prefab.place(sender, instanceWorld, pos, null, null);
        } catch (Exception e) {
            main.getLogger().at(Level.SEVERE).log("Error al colocar prefab: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void teleportPlayers(List<PlayerRef> playerRefs) {
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
                    } catch (Exception e) { e.printStackTrace(); }
                });

                CompletableFuture.delayedExecutor(1, SECONDS).execute(() -> {
                    newWorld.execute(() -> {
                        try {
                            PlayerRef updatedRef = Universe.get().getPlayer(playerRef.getUuid());
                            if (updatedRef != null && updatedRef.getReference() != null) {
                                Store<EntityStore> newStore = updatedRef.getReference().getStore();
                                Player player = newStore.getComponent(updatedRef.getReference(), Player.getComponentType());

                                if (player != null) { getLootInGame(updatedRef, player); }
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    });
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

            instanceNumber = --instanceNumber;

            String[] possiblePaths = {
                    "universe/" + worldName,
                    "universe/worlds/" + worldName,
                    "./universe/" + worldName,
                    "Worlds/" + worldName,
                    "worlds/" + worldName
            };

            for (String pathString : possiblePaths) {
                try {
                    java.nio.file.Path worldPath = java.nio.file.Paths.get(pathString);

                    if (java.nio.file.Files.exists(worldPath)) {
                        deleteDirectory(worldPath);
                        break;
                    }
                } catch (Exception e) {
                    main.getLogger().at(Level.WARNING).log("Error al intentar eliminar " + pathString + ": " + e.getMessage());
                }
            }

            isMapLoaded = false;
            newWorld = null;
        }
    }

    private void deleteDirectory(java.nio.file.Path path) throws java.io.IOException {
        if (java.nio.file.Files.isDirectory(path)) {
            try (java.util.stream.Stream<java.nio.file.Path> entries = java.nio.file.Files.list(path)) {
                entries.forEach(entry -> {
                    try { deleteDirectory(entry); }
                    catch (java.io.IOException e) { main.getLogger().at(Level.WARNING).log("Error eliminando: " + entry + " - " + e.getMessage()); }
                });
            }
        }
        java.nio.file.Files.delete(path);
    }

    public boolean getMapLoaded() { return isMapLoaded; }

    public void getLootInGame(PlayerRef playerRef, Player player) {
        World playerWorld = player.getWorld();
        if (playerWorld == null || !playerWorld.equals(newWorld)) { return; }

        // Get-Item
        player.getInventory().clear();
        Inventory inv = player.getInventory();

        ItemStack gun = new ItemStack("Weapon_Handgun", 1);
        ItemStack knife = new ItemStack("Weapon_Daggers_Cobalt", 1);
        ItemStack bullet = new ItemStack("Weapon_Arrow_Crude", 3600);

        inv.getHotbar().addItemStack(gun);
        inv.getHotbar().addItemStack(knife);
        inv.getStorage().addItemStack(bullet);

        inv.setActiveSlot(0, (byte) 0);

        player.sendMessage(Message.raw("You received Loot!"));

        // Crear nuevo HUD
        DeathmatchHUD deathmatchHUD = new DeathmatchHUD(playerRef);
        player.getHudManager().setCustomHud(playerRef, deathmatchHUD);
    }
}