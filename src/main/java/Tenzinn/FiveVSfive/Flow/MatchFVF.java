package Tenzinn.FiveVSfive.Flow;

import Tenzinn.Core.GameMatch;
import Tenzinn.Core.LootManager;
import Tenzinn.Core.Objects.PlayerStats;
import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.GameMatch.MatchState;
import Tenzinn.Core.Listeners.MessageListeners;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
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

    private static GameMatch myMatch;

    public static void startTimerMatch (GameMatch match) {
        myMatch = match;

        if (timerTask != null && !timerTask.isDone()) return;

        if(myMatch.getState() == MatchState.IN_PROGRESS) {
            remainingSeconds = timePerRound;

            timerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
                // Lo que pasa cuando termina la ronda
                if (remainingSeconds <= 0) { onEndRound(); }
                if (remainingSeconds <= -5) { onReloadRound(); }

                remainingSeconds--;
            }, 1, 1, TimeUnit.SECONDS);
        }
        else {
            remainingSeconds = timePerPurchase;

            for(int i = 0; i < myMatch.getPlayers().size(); i++) {
                PlayerStats playerStats = RefactorTool.getPlayerStats(myMatch.getPlayers().get(i));
                assert playerStats != null;
                playerStats.canReceivedLoot = true;
            }

            timerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
                // Lo que pasa cuando termina la fase de compra
                if (remainingSeconds <= 0) { onEndPurchase(); }

                remainingSeconds--;
            }, 1, 1, TimeUnit.SECONDS);
        }
    }
    // ================================================== //
    public static void onEndPurchase() {
        stopTimer();

        for(int i = 0; i < myMatch.getPlayers().size(); i++) {
            PlayerStats playerStats = RefactorTool.getPlayerStats(myMatch.getPlayers().get(i));
            assert playerStats != null;
            playerStats.canReceivedLoot = false;

            playerStats.setFinishBuyZone();

            Ref<EntityStore> ref = playerStats.getPlayerRef().getReference();
            Store<EntityStore> store = ref.getStore();

            playerStats.getPlayer().getPageManager().setPage(ref, store, Page.None);
        }

        myMatch.setState(GameMatch.MatchState.IN_PROGRESS);
        startTimerMatch(myMatch);
    }
    public static void onReloadRound() {
        stopTimer();

        myMatch.setState(MatchState.ON_PURCHASE);

        resetPlayers(myMatch.getPlayers());

        // Activar barreras de periodo de compra

        startTimerMatch(myMatch);
    }
    public static void onEndRound() {
        // Activar contenido en la UI de finalización de ronda
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
    public static int getTimer () { return remainingSeconds; }
    public static void stopTimer() { if (timerTask != null && !timerTask.isDone()) timerTask.cancel(false); }
    // ================================================== //
    public static boolean validateFinishRound () {
        boolean stateTeam01 = true;
        boolean stateTeam02 = true;

        // Saber si quedan jugadores del EQUIPO 1 vivos
        for (int i = 0; i < (myMatch.getPlayers().size() / 2); i++) {
            PlayerStats playerStats = RefactorTool.getPlayerStats(myMatch.getPlayers().get(i));

            if (playerStats.playerState == PlayerStats.PlayerState.DEFAULT) { stateTeam01 = false; break; }
        }

        // Saber si quedan jugadores del EQUIPO 2 vivos
        for (int i = 5; i < myMatch.getPlayers().size(); i++) {
            PlayerStats playerStats = RefactorTool.getPlayerStats(myMatch.getPlayers().get(i));
            if (playerStats.playerState == PlayerStats.PlayerState.DEFAULT) { stateTeam02 = false; break; }
        }

        return stateTeam01 || stateTeam02;
    }
}