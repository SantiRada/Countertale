package Tenzinn.Core.Tools;

import Tenzinn.Core.GameMatch;
import Tenzinn.Core.UI.GameHUD;
import Tenzinn.Core.LootManager;
import Tenzinn.Deathmatch.UI.MvpPage;
import Tenzinn.Core.Objects.PlayerStats;
import Tenzinn.FiveVSfive.Flow.MatchFVF;
import Tenzinn.FiveVSfive.UI.EndMatchPage;
import Tenzinn.Core.Objects.WeaponStats;
import Tenzinn.Core.Listeners.MapListeners;
import Tenzinn.Deathmatch.UI.ScoreboardPage;
import Tenzinn.Core.Listeners.MessageListeners;
import Tenzinn.FiveVSfive.UI.ScoreboardPageFVF;
import Tenzinn.Core.Listeners.MapListeners.SpawnMode;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.math.vector.Rotation3f;
import org.joml.Vector3d;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.CopyOnWriteArrayList;

public class RefactorTool {

    public enum TypeData { SCORE, DEATH, KILL }
    public static List<PlayerStats> playerStatsList = new CopyOnWriteArrayList<>();
    public static List<WeaponStats> slots = new CopyOnWriteArrayList<>();
    private static final Map<UUID, PlayerStats> playerStatsByUuid = new ConcurrentHashMap<>();

    public static void setPlayerStats (PlayerStats playerStats) {
        playerStatsList.add(playerStats);
        playerStatsByUuid.put(playerStats.getPlayerRef().getUuid(), playerStats);
    }
    public static void setQuitPlayerStats (PlayerStats playerStats) {
        playerStatsList.remove(playerStats);
        playerStatsByUuid.remove(playerStats.getPlayerRef().getUuid());
    }
    public static void setSlots (ArrayList<WeaponStats> newSlots){
        slots.clear();
        slots.addAll(newSlots);
    }
    public static void setLoot(PlayerRef playerRef, int index) {
        if (getSizeSlots() <= 0) return;

        WeaponStats newWeapon = slots.get(index - 1);

        if (newWeapon.nameWeapon.equalsIgnoreCase("comingsoon")) return;

        playerRef.sendMessage(MessageListeners.message(MessageListeners.MessageKey.CHAT_WHEN_BUYING)
                .param("weapon", newWeapon.nameWeapon)
                .color(Color.cyan));

        PlayerStats stats = getPlayerStats(playerRef);
        if (stats == null) return;

        switch (newWeapon.typeWeapon.toLowerCase()) {
            case "primary":     stats.primaryWeapon = newWeapon;    break;
            case "secondary":   stats.secondaryWeapon = newWeapon;  break;
            case "shield":      stats.shield = newWeapon;           break;
        }

        if (!stats.canReceivedLoot && !stats.getCurrentMatch().isBuyPhase()) {
            playerRef.sendMessage(MessageListeners.message(MessageListeners.MessageKey.CHAT_BUYING_LATE).color(Color.yellow));
        }
        else { LootManager.giveLoot(stats.getPlayer(), getLoot(stats.getPlayerRef())); }
    }
    public static void setAllLoot(PlayerRef playerRef, ArrayList<WeaponStats> list) {

        if (list == null) { list = LootManager.getStarterKit(); }
        if (list.isEmpty()) { list = LootManager.getStarterKit(); }

        PlayerStats playerStats = getPlayerStats(playerRef);
        if (playerStats == null) return;

        for (WeaponStats item : list) {
            if(item.typeWeapon.equalsIgnoreCase("primary")) playerStats.primaryWeapon = item;
            if(item.typeWeapon.equalsIgnoreCase("secondary")) { playerStats.secondaryWeapon = item; }
            if(item.typeWeapon.equalsIgnoreCase("shield")) playerStats.shield = item;
        }
    }
    public static void setHealthPlayer(PlayerRef playerRef, float value) {
        PlayerStats playerStats = getPlayerStats(playerRef);
        playerStats.setHealth((int) value);
    }
    // ============================================ //
    public static ArrayList<WeaponStats> getLoot(PlayerRef playerRef) {
        PlayerStats playerStats = RefactorTool.getPlayerStats(playerRef);
        if(playerStats == null) return null;

        ArrayList<WeaponStats> list = new ArrayList<>();

        if(playerStats.primaryWeapon != null) list.add(playerStats.primaryWeapon);

        if(playerStats.secondaryWeapon != null) list.add(playerStats.secondaryWeapon);

        if(playerStats.shield != null) list.add(playerStats.shield);

        return !list.isEmpty() ? list : null;
    }
    public static int getSizeSlots () { return slots.size(); }
    public static PlayerStats getPlayerStats(PlayerRef playerRef) {
        return playerRef == null ? null : playerStatsByUuid.get(playerRef.getUuid());
    }
    public static List<PlayerStats> getPlayerList(GameMatch match) {
        return playerStatsList.stream()
                .filter(playerStats -> match.equals(playerStats.getCurrentMatch()))
                .collect(Collectors.toList());
    }
    // ============================================ //
    public static ArrayList<Vector3d> getSpawns (String nameMap, SpawnMode mode) {
        try {
            List<MapListeners.SpawnPoint> allSpawns = MapListeners.get(nameMap, mode);

            ArrayList<Vector3d> spawns = new ArrayList<>();

            for(MapListeners.SpawnPoint spawn : allSpawns) { spawns.add(new Vector3d(spawn.x, spawn.y, spawn.z)); }

            return spawns;
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    public static Vector3d getRandomSpawn(String nameMap) {
        ArrayList<Vector3d> spawns = new ArrayList<>(getSpawns(nameMap, SpawnMode.DM));

        int randomPosition = ThreadLocalRandom.current().nextInt(spawns.size());

        return spawns.get(randomPosition);
    }
    public static Vector3d getSpawnForTeam(PlayerRef playerRef) {
        PlayerStats playerStats = getPlayerStats(playerRef);
        assert playerStats != null;

        // El mapa viene del match al que pertenece el jugador
        String mapId = playerStats.getCurrentMatch().getMapId();

        ArrayList<Vector3d> spawns = getSpawns(mapId, SpawnMode.FVF);

        List<PlayerStats> playerList = getPlayerList(playerStats.getCurrentMatch());
        int index = playerList.indexOf(playerStats);

        return spawns.get(index);
    }
    // ============================================ //
    public static SpawnMode getModeForPlayer(PlayerRef playerRef) {
        String mode = Objects.requireNonNull(getPlayerStats(playerRef)).getCurrentMatch().getMode();

        if (mode.equalsIgnoreCase("dm")) { return SpawnMode.DM; }

        return SpawnMode.FVF;
    }
    public static Player getPlayer(PlayerRef playerRef) {
        Ref<EntityStore> ref = playerRef.getReference();
        Store<EntityStore> store = ref.getStore();

        Player player = store.getComponent(ref, Player.getComponentType());

        return player;
    }
    // ============================================ //
    public static void Respawn(PlayerRef playerRef) {
        SpawnMode mode = getModeForPlayer(playerRef);

        assert playerRef.getWorldUuid() != null;
        World currentWorld = Universe.get().getWorld(playerRef.getWorldUuid());
        if (currentWorld == null) { return; }

        PlayerStats playerStats = getPlayerStats(playerRef);
        if (playerStats == null) return;
        String mapId = playerStats.getCurrentMatch().getMapId();

        Vector3d spawnPos = getRandomSpawn(mapId);
        if (mode == SpawnMode.FVF) { spawnPos = getSpawnForTeam(playerRef); }
        Vector3d finalSpawnPos = spawnPos;

        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            currentWorld.execute(() -> {
                try {
                    Ref<EntityStore> ref = playerRef.getReference();
                    Store<EntityStore> store = ref.getStore();

                    Teleport teleport = Teleport.createForPlayer(finalSpawnPos, Rotation3f.ZERO);
                    store.putComponent(ref, Teleport.getComponentType(), teleport);
                } catch (Exception e) { e.printStackTrace(); }
            });
        }, 150, TimeUnit.MILLISECONDS);

        CustomUIHud customHUD = RefactorTool.getPlayer(playerRef).getHudManager().getCustomHud(playerRef.getUuid().toString());
        boolean isInGame = false;
        if (customHUD instanceof GameHUD hud) {
            isInGame = true;
            if (mode == SpawnMode.DM) {
                if (playerStats.getCurrentMatch().getState() == GameMatch.MatchState.IN_PROGRESS) {
                    hud.setShopTimer();
                    hud.setInvulnerability();
                }
            }
        }
        GameHUD newHud = isInGame ? (GameHUD) customHUD : null;
        if (newHud == null) return;

        if (mode == SpawnMode.DM) {
            playerStats.isInvulnerable = true;
            newHud.setEffect(PlayerStats.Effects.INVULNERABILITY);

            if (!playerStats.getCurrentMatch().isBuyPhase()) {
                HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                    currentWorld.execute(() -> {
                        playerStats.isInvulnerable = false;
                        newHud.setEffect(PlayerStats.Effects.NULL);
                    });
                }, 3, TimeUnit.SECONDS);
            }

            playerStats.canReceivedLoot = true;
            playerStats.timerCanReceivedLoot = 15;

            HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                currentWorld.execute(() -> {
                    playerStats.canReceivedLoot = false;
                });
            }, 15, TimeUnit.SECONDS);
        }
    }
    // ============================================ //
    public static void setChangesInUI(GameMatch match) {
        ArrayList<PlayerStats> playersList = new ArrayList<>(getPlayerList(match));

        for (PlayerStats playerStats : playersList) {
            if(playerStats.getPlayer().getHudManager().getCustomHud(playerStats.getPlayerRef().getUuid().toString()) == null) continue;

            Object testHud = playerStats.getPlayer().getHudManager().getCustomHud(playerStats.getPlayerRef().getUuid().toString());

            if(testHud instanceof GameHUD gameHUD) { gameHUD.setData(); }
            else if(testHud instanceof ScoreboardPage scoreboard) { scoreboard.setData(); }
            else if(testHud instanceof ScoreboardPageFVF scoreboard) { scoreboard.setData(); }
        }
    }
    public static void setChangesInSlots (int value, PlayerRef playerRef) {
        Player player = getPlayer(playerRef);
        if(player == null) return;

        Object testHud = player.getHudManager().getCustomHud(playerRef.getUuid().toString());

        if(testHud == null) return;

        if(testHud instanceof GameHUD currentHUD) {
            if(value > 3) return;

            currentHUD.setWeapons(value);
        }
    }
    public static void setDataScore(PlayerRef playerRef, TypeData typeData, float value) {

        PlayerStats playerStats = getPlayerStats(playerRef);
        assert playerStats != null;

        switch (typeData) {
            case TypeData.SCORE: int finalValue = (int)value; playerStats.setScore(finalValue); break;
            case TypeData.DEATH: playerStats.setDeaths(); break;
            case TypeData.KILL: playerStats.setKills(); break;
        }
    }
    // ============================================ //
    public static void finishGame(List<PlayerRef> players, Tenzinn.Core.GameMatch match) {
        if (match != null) {
            match.stopTimer();
            match.setState(Tenzinn.Core.GameMatch.MatchState.FINISHED);
        }

        int winningTeam = MatchFVF.winner;

        for (PlayerRef playerRef : players) {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null) continue;
            Store<EntityStore> store = ref.getStore();

            Player player = getPlayer(playerRef);
            if (player == null) continue;

            // Roll for a case drop before opening the end screen
            PlayerStats stats = getPlayerStats(playerRef);
            if (stats != null) {
                stats.hasPendingCase = Tenzinn.Core.Cases.CaseManager.rollForCase();
                if (stats.hasPendingCase) {
                    Tenzinn.Core.Cases.CaseManager.addCase(playerRef.getUuid());
                }
            }

            if (getModeForPlayer(playerRef) == SpawnMode.DM) {
                player.getPageManager().openCustomPage(ref, store, new MvpPage(playerRef));
            } else {
                UUID worldId = playerRef.getWorldUuid();
                if (worldId == null) continue;
                World world = Universe.get().getWorld(worldId);
                if (world == null) continue;
                final int fWinningTeam = winningTeam;
                world.execute(() -> player.getPageManager().openCustomPage(ref, store,
                        new EndMatchPage(playerRef, fWinningTeam)));
            }
        }
    }
    public static int getPlayersReady() {
        List<PlayerRef> playerRefs = new ArrayList<>(Universe.get().getPlayers());
        int playersReady = 0;

        for (PlayerRef ref : playerRefs) {
            if (RefactorTool.getPlayerStats(ref) == null) { // null = en lobby, disponible
                playersReady++;
            }
        }

        return playersReady;
    }
    public static int getPlayersReadyInOneMinute() {
        List<PlayerRef> playerRefs = new ArrayList<>(Universe.get().getPlayers());
        int playersReadyInOneMinute = 0;

        for (int i = 0; i < playerRefs.size(); i++) {
            PlayerStats playerStats = RefactorTool.getPlayerStats(playerRefs.get(i));

            if(playerStats != null) {
                int remainingTime = playerStats.getCurrentMatch().getTimer();
                if (remainingTime <= 60) playersReadyInOneMinute += 1;
            }
        }

        return playersReadyInOneMinute;
    }
    // ============================================ //
    public static void launchSound(PlayerRef playerRef, String sound) {
        assert playerRef.getWorldUuid() != null;

        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        assert world != null;

        Player player = getPlayer(playerRef);
        launchSound(world, player, sound);
    }
    public static void launchSound(World world, Player player, String sound) {
        Ref<EntityStore> playerRef = player.getReference();
        Store<EntityStore> store = world.getEntityStore().getStore();

        world.execute(() -> {
            assert playerRef != null;
            TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
            if (transform == null) return;

            int index = switch (sound) {
                case "clic" -> SoundEvent.getAssetMap().getIndex("SFX_Clic");
                case "fail" -> SoundEvent.getAssetMap().getIndex("SFX_Fail");
                default -> -1;
            };

            if (index == -1) return;

            SoundUtil.playSoundEvent3dToPlayer(playerRef, index, SoundCategory.SFX, transform.getPosition(), store);
        });
    }

    public static void clearRuntimeState() {
        playerStatsList.clear();
        playerStatsByUuid.clear();
        slots.clear();
    }
}
