package Tenzinn.Tools;

import Tenzinn.Deathmatch.PlayerStats;

import Tenzinn.Deathmatch.UI.DeathmatchHUD;
import Tenzinn.Deathmatch.UI.ScoreboardPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
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

    public static List<PlayerStats> playerStatsList = new ArrayList<>();

    private static Vector3d[] spawns = {
            new Vector3d(36, 56, 0),
            new Vector3d(16, 52, 5),
            new Vector3d(8, 52, 1),
            new Vector3d(4, 56, -3),
            new Vector3d(14, 56, -9),
            new Vector3d(30, 51, -9),
            new Vector3d(12, 52, -10),
            new Vector3d(1, 52, 1),
            new Vector3d(12, 52, 11),
            new Vector3d(14, 56, 10)
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
    public static PlayerStats getPlayerStats(Player player) {

        if (playerStatsList.size() == 0) return null;

        PlayerRef playerRef = getPlayerRef(player);

        for(PlayerStats playerStats : playerStatsList) {
            if(playerStats.getPlayerRef().equals(playerRef)) { return playerStats; }
        }

        return null;
    }
    public static List<PlayerStats> getPlayerList() { return playerStatsList; }
    // ============================================ //
    public static PlayerRef getPlayerRef(Player player) { return Universe.get().getPlayerByUsername(player.getDisplayName(), NameMatching.EXACT); }
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
}