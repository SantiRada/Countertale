package Tenzinn.Tools;

import Tenzinn.Deathmatch.UI.DeathmatchHUD;
import Tenzinn.Deathmatch.UI.ScoreboardPage;
import Tenzinn.Deathmatch.Objects.PlayerStats;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;

import java.awt.*;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class RefactorTool {

    public enum TypeData { SCORE, DEATH, KILL }
    public static List<PlayerStats> playerStatsList = new ArrayList<>();

    private static Vector3d[] spawns = {
            new Vector3d(-27, 107, -10),
            new Vector3d(-28, 107, 10),
            new Vector3d(-10, 110, -13),
            new Vector3d(-31, 112, -23),
            new Vector3d(-20, 110, -43),
            new Vector3d(18, 106, -36),
            new Vector3d(14, 110, -15),
            new Vector3d(40, 110, -13),
            new Vector3d(0, 111, 32),
            new Vector3d(-33, 110, 28)
    };

    public static void setPlayerStats (PlayerStats playerStats) { playerStatsList.add(playerStats); }
    public static void setQuitPlayerStats (PlayerStats playerStats) { playerStatsList.remove(playerStats); }
    // ============================================ //
    public static PlayerStats getPlayerStats(PlayerRef playerRef) {

        if (playerStatsList.size() == 0) return null;

        for(PlayerStats playerStats : playerStatsList) {
            if(playerStats.getPlayerRef().equals(playerRef)) { return playerStats; }
        }

        return null;
    }
    public static List<PlayerStats> getPlayerList() { return playerStatsList; }
    // ============================================ //
    public static Player getPlayer(PlayerRef playerRef) {
        Ref<EntityStore> ref = playerRef.getReference();
        Store<EntityStore> store = ref.getStore();

        Player player = store.getComponent(ref, Player.getComponentType());

        return player;
    }
    public static CustomUIHud getCustomHud (PlayerRef playerRef) {
        Player player = getPlayer(playerRef);

        return player.getHudManager().getCustomHud();
    }
    // ============================================ //
    public static void Respawn(PlayerRef playerRef) {
        World currentWorld = Universe.get().getWorld(playerRef.getWorldUuid());

        if (currentWorld == null) { return; }

        Random random = new Random();
        int randomPosition = random.nextInt(10);
        Vector3d spawnPos = spawns[randomPosition];

        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            currentWorld.execute(() -> {
                try {
                    Ref<EntityStore> ref = playerRef.getReference();
                    Store<EntityStore> store = ref.getStore();

                    Vector3f rotation = new Vector3f();
                    Vector3f targetPosition = new Vector3f((float) spawnPos.getX(), (float) spawnPos.getY(), (float) spawnPos.getZ());

                    Teleport teleport = new Teleport(targetPosition.toVector3d(), rotation);
                    store.putComponent(ref, Teleport.getComponentType(), teleport);
                }  catch (Exception e) { e.printStackTrace(); }
            });
        }, 150, TimeUnit.MILLISECONDS);
    }
    public static void setChangesInUI() {
        for (PlayerStats playerStats : playerStatsList) {
            if(playerStats.getPlayer().getHudManager().getCustomHud() == null) continue;

            Object testHud = playerStats.getPlayer().getHudManager().getCustomHud();
            if(testHud instanceof DeathmatchHUD) {

                DeathmatchHUD deathmatchHUD = (DeathmatchHUD)testHud;
                deathmatchHUD.setData();

            } else if(testHud instanceof ScoreboardPage) {

                ScoreboardPage scoreboard = (ScoreboardPage)testHud;
                scoreboard.setData();

            }
            else { playerStats.getPlayer().sendMessage(Message.raw("No se encuentra la UI del Player").color(Color.yellow)); }
        }
    }
    public static void setChangesInSlots (int value, PlayerRef playerRef) {
        Player player = getPlayer(playerRef);
        if(player == null) return;

        Object testHud = player.getHudManager().getCustomHud();

        if(testHud == null) return;

        if(testHud instanceof DeathmatchHUD) {
            DeathmatchHUD currentHUD = (DeathmatchHUD) testHud;

            if(value > 3) return;

            currentHUD.setWeapons(value);
        }
    }
    public static void setDataScore(Player player, TypeData typeData, float value) {
        PlayerRef playerRef = Universe.get().getPlayerByUsername(player.getDisplayName(), NameMatching.EXACT);

        PlayerStats playerStats = getPlayerStats(playerRef);

        switch (typeData) {
            case TypeData.SCORE:
                assert playerStats != null;
                int finalValue = (int)value;
                playerStats.setScore(finalValue);
                break;
            case TypeData.DEATH:
                playerStats.setDeaths();
                break;
            case TypeData.KILL:
                playerStats.setKills();
                break;
        }
    }
}