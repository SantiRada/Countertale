package Tenzinn.FiveVSfive.Flow;

import Tenzinn.Core.GameMatch;
import Tenzinn.Core.Listeners.MapListeners;
import Tenzinn.Core.LootManager;
import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.Objects.PlayerStats;
import Tenzinn.Core.GameMatch.MatchState;
import Tenzinn.Core.Listeners.MessageListeners;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import Tenzinn.FiveVSfive.UI.EndRoundPage;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.HytaleServer;
import Tenzinn.FiveVSfive.Systems.TemporalWallSystem;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;

public class MatchFVF {

    private static ScheduledFuture<?> timerTask;
    private static int remainingSeconds = 150;

    private static int timePerRound = 150;
    private static int timePerPurchase = 15;

    public static int winner = -1;
    public static int numRound = 1;
    private static boolean inEndRound = false;
    private static boolean inEndPurchase = false; // <-- Flag agregado

    private static GameMatch myMatch;

    public static void startTimerMatch(GameMatch match) {
        myMatch = match;

        if (timerTask != null && !timerTask.isDone()) return;

        if (myMatch.getState() == MatchState.IN_PROGRESS) {
            remainingSeconds = timePerRound;
            inEndRound = false; // <-- Reset al iniciar ronda

            String mapName = "Dust2";
            TemporalWallSystem.removeWalls(mapName, myMatch.getPlayers().getFirst());

            timerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
                if (remainingSeconds <= 0 && !inEndRound) { onEndRound(); }
                if (remainingSeconds <= -5) { onReloadRound(); }

                remainingSeconds--;
            }, 1, 1, TimeUnit.SECONDS);
        } else {
            remainingSeconds = timePerPurchase;
            inEndPurchase = false; // <-- Reset al iniciar compra

            for (int i = 0; i < myMatch.getPlayers().size(); i++) {
                PlayerStats playerStats = RefactorTool.getPlayerStats(myMatch.getPlayers().get(i));
                assert playerStats != null;
                playerStats.canReceivedLoot = true;
            }

            String mapName = "Dust2";
            TemporalWallSystem.buildWalls(mapName, myMatch.getPlayers().getFirst());

            timerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
                if (remainingSeconds <= 0 && !inEndPurchase) { onEndPurchase(); } // <-- Flag agregado

                remainingSeconds--;
            }, 1, 1, TimeUnit.SECONDS);
        }
    }

    // ================================================== //
    public static void onEndPurchase() {
        inEndPurchase = true; // <-- Flag activado antes de cualquier otra cosa
        stopTimer();

        for (int i = 0; i < myMatch.getPlayers().size(); i++) {
            PlayerStats playerStats = RefactorTool.getPlayerStats(myMatch.getPlayers().get(i));
            assert playerStats != null;
            playerStats.canReceivedLoot = false;

            playerStats.setFinishBuyZone();

            Ref<EntityStore> ref = playerStats.getPlayerRef().getReference();
            assert ref != null;
            Store<EntityStore> store = ref.getStore();

            playerStats.getPlayer().getPageManager().setPage(ref, store, Page.None);
        }

        String mapName = "Dust2";
        TemporalWallSystem.removeWalls(mapName, myMatch.getPlayers().getFirst());

        myMatch.setState(GameMatch.MatchState.IN_PROGRESS);
        startTimerMatch(myMatch);
    }

    public static void onReloadRound() {
        stopTimer();

        myMatch.setState(MatchState.ON_PURCHASE);
        resetPlayers(myMatch.getPlayers());

        String mapName = "Dust2";
        TemporalWallSystem.buildWalls(mapName, myMatch.getPlayers().getFirst());

        startTimerMatch(myMatch);
        numRound += 1;
    }

    public static void onEndRound() {
        inEndRound = true; // <-- Flag activado antes de cualquier otra cosa
        List<PlayerRef> players = myMatch.getPlayers();

        for (int i = 0; i < players.size(); i++) {
            Ref<EntityStore> ref = players.get(i).getReference();
            assert ref != null;
            Store<EntityStore> store = ref.getStore();
            Player player = store.getComponent(ref, Player.getComponentType());

            player.getPageManager().openCustomPage(ref, store, new EndRoundPage(players.get(i)));
        }
    }

    // ================================================== //
    public static void resetPlayers(List<PlayerRef> playerRefs) {
        for (int i = 0; i < playerRefs.size(); i++) {
            Player player = RefactorTool.getPlayer(playerRefs.get(i));
            LootManager.giveLoot(player, LootManager.getStarterKit());
            // Curar al player
        }

        teleportPlayers(playerRefs);
    }

    public static void teleportPlayers(List<PlayerRef> playerRefs) {
        ArrayList<Vector3d> spawns = RefactorTool.getSpawns("dust2", RefactorTool.getModeForPlayer(playerRefs.getFirst()));
        World newWorld = Universe.get().getWorld(playerRefs.get(0).getWorldUuid());

        for (int i = 0; i < playerRefs.size(); i++) {
            Vector3d spawnPos = spawns.get(i % spawns.size());
            Transform spawnPoint = new Transform(spawnPos.x, spawnPos.y, spawnPos.z);
            PlayerRef playerRef = playerRefs.get(i);

            try {
                UUID playerUUID = playerRef.getUuid();
                PlayerRef updatedPlayerRef = Universe.get().getPlayer(playerUUID);

                if (updatedPlayerRef == null || updatedPlayerRef.getReference() == null) continue;

                Ref<EntityStore> ref = updatedPlayerRef.getReference();
                assert updatedPlayerRef.getWorldUuid() != null;

                World currentWorld = Universe.get().getWorld(updatedPlayerRef.getWorldUuid());
                if (currentWorld == null) continue;

                updatedPlayerRef.sendMessage(Message.raw(MessageListeners.get(MessageListeners.MessageKey.CHAT_TELEPORTING_GAME)));

                currentWorld.execute(() -> {
                    try {
                        Store<EntityStore> store = currentWorld.getEntityStore().getStore();
                        Teleport teleport = Teleport.createForPlayer(newWorld, spawnPoint);
                        store.addComponent(ref, Teleport.getComponentType(), teleport);
                    } catch (Exception e) { e.printStackTrace(); }
                });

            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // ================================================== //
    public static int getTimer() { return remainingSeconds; }
    public static void stopTimer() { if (timerTask != null && !timerTask.isDone()) timerTask.cancel(false); }

    // ================================================== //
    public static int getNumberRound() { return numRound; }

    public static int validateFinishRound() {
        boolean stateTeam01 = true;
        boolean stateTeam02 = true;

        for (int i = 0; i < (myMatch.getPlayers().size() / 2); i++) {
            PlayerStats playerStats = RefactorTool.getPlayerStats(myMatch.getPlayers().get(i));
            if (playerStats.playerState == PlayerStats.PlayerState.DEFAULT) { stateTeam01 = false; break; }
        }

        for (int i = 5; i < myMatch.getPlayers().size(); i++) {
            PlayerStats playerStats = RefactorTool.getPlayerStats(myMatch.getPlayers().get(i));
            if (playerStats.playerState == PlayerStats.PlayerState.DEFAULT) { stateTeam02 = false; break; }
        }

        winner = stateTeam01 ? 1 : stateTeam02 ? 2 : 0;
        return winner;
    }

    public static int validateTeamMembership(PlayerRef playerRef) {
        List<PlayerRef> players = myMatch.getPlayers();
        int value = -1;

        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).equals(playerRef)) { value = i; break; }
        }

        if (value >= (players.size() / 2)) value = 2;
        else value = 1;

        return value;
    }
}