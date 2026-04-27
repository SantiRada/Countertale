package Tenzinn.Core.Instances;

import Tenzinn.Core.GameMatch;
import Tenzinn.Core.Listeners.MapListeners;
import Tenzinn.Core.Objects.PlayerStats;
import Tenzinn.OrbisOffensive;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.logging.Level;

public class InstanceManager {

    private boolean isMapLoaded = false;
    private final OrbisOffensive main;
    private World newWorld;
    private String worldName;

    private final String mapId;

    public InstanceManager(OrbisOffensive main, String mapId) {
        this.main = main;
        this.mapId = mapId.toLowerCase();
    }

    public String getMapId() {
        return mapId;
    }

    public boolean getMapLoaded() {
        return isMapLoaded;
    }

    private boolean ensureWorldStorageFolders() {
        if (worldName == null || worldName.isBlank()) return false;

        try {
            Path worldPath = Paths.get("universe", "worlds", worldName);
            Files.createDirectories(worldPath);
            Files.createDirectories(worldPath.resolve("chunks"));
            return true;
        } catch (IOException e) {
            main.getLogger().at(Level.SEVERE).log("[Instance] Failed to create world storage folders for " + worldName + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void preloadMap(Runnable onMapReady) {
        Universe universe = Universe.get();

        this.worldName = mapId + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        if (!ensureWorldStorageFolders()) {
            if (onMapReady != null) onMapReady.run();
            return;
        }

        universe.addWorld(this.worldName, "Flat", null).thenAccept(instanceWorld -> {
            main.getLogger().at(Level.INFO).log("[Instance] Empty arena created: " + instanceWorld.getName());

            WorldConfig config = instanceWorld.getWorldConfig();
            config.setDeleteOnRemove(true);
            config.markChanged();
            config.setGameTimePaused(true);

            try {
                config.setGameTime(java.time.Instant.parse("0001-01-01T12:00:00Z"));
            } catch (Exception e) {
                main.getLogger().at(Level.SEVERE).log("Error setting GameTime: " + e.getMessage());
            }

            config.setBlockTicking(true);
            config.setTicking(true);
            config.setIsAllNPCFrozen(false);
            config.setSpawningNPC(false);
            config.setPvpEnabled(true);
            config.setCanUnloadChunks(false);

            instanceWorld.execute(() -> {
                boolean placed = placePrefabInInstance(instanceWorld);

                if (!placed) {
                    isMapLoaded = false;
                    newWorld = null;
                    main.getLogger().at(Level.SEVERE).log("[Instance] Map failed to load and will not be marked ready: " + mapId);
                    if (onMapReady != null) onMapReady.run();
                    return;
                }

                newWorld = instanceWorld;
                isMapLoaded = true;

                main.getLogger().at(Level.INFO).log("[Instance] ✓ Instance ready [" + mapId + "]: " + worldName);
                if (onMapReady != null) onMapReady.run();
            });
        });
    }

    private String resolvePrefabName() {
        String[] parts = mapId.split("-");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1));
            sb.append("-");
        }
        // Remove trailing hyphen
        if (sb.length() > 0) sb.setLength(sb.length() - 1);
        return sb + ".prefab.json";
    }

    private boolean placePrefabInInstance(World instanceWorld) {
        try {
            PrefabStore store = PrefabStore.get();
            String prefabName = resolvePrefabName();

            BlockSelection prefab = store.getAssetPrefabFromAnyPack(prefabName);
            if (prefab == null) {
                throw new RuntimeException("Prefab '" + prefabName + "' not found");
            }

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

            main.getLogger().at(Level.INFO).log("[Instance] ✓ Prefab '" + prefabName + "' placed in " + elapsed + "ms");
            return true;

        } catch (Exception e) {
            main.getLogger().at(Level.SEVERE).log("[Instance] Error placing prefab: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    public void teleportPlayers(List<PlayerRef> playerRefs) {
        main.getLogger().at(Level.INFO).log("[Instance] === TELEPORT START [" + mapId + "] ===");

        if (newWorld == null || !isMapLoaded) {
            main.getLogger().at(Level.WARNING).log("[Instance] Cannot teleport: map not loaded");
            return;
        }

        if (playerRefs == null || playerRefs.isEmpty()) {
            main.getLogger().at(Level.WARNING).log("[Instance] Cannot teleport: no players supplied");
            return;
        }

        PlayerStats firstStats = RefactorTool.getPlayerStats(playerRefs.getFirst());
        if (firstStats == null || firstStats.getCurrentMatch() == null) {
            main.getLogger().at(Level.SEVERE).log("[Instance] Cannot teleport: first player has no match stats");
            return;
        }

        GameMatch match = firstStats.getCurrentMatch();

        MapListeners.SpawnMode mode = match.getMode().equalsIgnoreCase("dm")
                ? MapListeners.SpawnMode.DM
                : MapListeners.SpawnMode.FVF;

        ArrayList<Vector3d> spawns = RefactorTool.getSpawns(mapId, mode);

        if (spawns == null || spawns.isEmpty()) {
            main.getLogger().at(Level.SEVERE).log("[Instance] No spawn points found for map: " + mapId + " mode=" + mode);
            return;
        }

        Map<UUID, Integer> teamAssignments = Map.of();

        if (mode == MapListeners.SpawnMode.FVF) {
            if (spawns.size() < 10) {
                main.getLogger().at(Level.SEVERE).log("[Instance] FVF map '" + mapId + "' must have 10 FVF spawn locations. Found: " + spawns.size());
                return;
            }

            match.formTeams();
            teamAssignments = match.getTeamAssignments();
        }

        int team1Count = 0;
        int team2Count = 0;

        for (int i = 0; i < playerRefs.size(); i++) {
            PlayerRef originalRef = playerRefs.get(i);
            if (originalRef == null) continue;

            Vector3d spawnPos;

            if (mode == MapListeners.SpawnMode.FVF) {
                int team = teamAssignments.getOrDefault(originalRef.getUuid(), 1);

                int spawnIndex;
                if (team == 1) {
                    spawnIndex = team1Count % 5;
                    team1Count++;
                } else {
                    spawnIndex = 5 + (team2Count % 5);
                    team2Count++;
                }

                spawnPos = spawns.get(spawnIndex);
            } else {
                spawnPos = spawns.get(i % spawns.size());
            }

            Transform spawnPoint = new Transform(spawnPos.x + 0.5f, spawnPos.y, spawnPos.z + 0.5f);

            try {
                UUID playerUUID = originalRef.getUuid();
                PlayerRef updatedRef = Universe.get().getPlayer(playerUUID);

                if (updatedRef == null || updatedRef.getReference() == null || updatedRef.getWorldUuid() == null) {
                    main.getLogger().at(Level.WARNING).log("[Instance] Skipping teleport for stale player ref: " + playerUUID);
                    continue;
                }

                Ref<EntityStore> ref = updatedRef.getReference();
                World currentWorld = Universe.get().getWorld(updatedRef.getWorldUuid());

                if (currentWorld == null) {
                    main.getLogger().at(Level.WARNING).log("[Instance] Skipping teleport because current world is null: " + playerUUID);
                    continue;
                }

                updatedRef.sendMessage(Message.raw(MessageListeners.get(MessageListeners.MessageKey.CHAT_TELEPORTING_GAME)));

                currentWorld.execute(() -> {
                    try {
                        Store<EntityStore> store = currentWorld.getEntityStore().getStore();
                        Teleport teleport = Teleport.createForPlayer(newWorld, spawnPoint);
                        store.addComponent(ref, Teleport.getComponentType(), teleport);
                        main.getLogger().at(Level.INFO).log("[Instance] ✓ Player teleported: " + playerUUID);
                    } catch (Exception e) {
                        main.getLogger().at(Level.SEVERE).log("[Instance] Error teleporting player: " + e.getMessage());
                        e.printStackTrace();
                    }
                });

            } catch (Exception e) {
                main.getLogger().at(Level.SEVERE).log("[Instance] Error in teleportPlayers: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (mode == MapListeners.SpawnMode.DM) {
            com.hypixel.hytale.server.core.HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                if (match.getState() == GameMatch.MatchState.STARTING) {
                    match.startTimer();
                }
            }, 3, java.util.concurrent.TimeUnit.SECONDS);
        }
    }
        public void removeInstance () {
            if (worldName == null) return;

            Universe universe = Universe.get();
            World instanceWorld = universe.getWorld(worldName);

            if (instanceWorld != null) {
                try {
                    universe.removeWorld(worldName);
                    main.getLogger().at(Level.INFO).log("[Instance] ✓ World removed: " + worldName);
                } catch (Exception e) {
                    main.getLogger().at(Level.WARNING).log("[Instance] Error removing world: " + e.getMessage());
                }
            }

            isMapLoaded = false;
            newWorld = null;
            worldName = null;
        }
    }
