package Tenzinn.FiveVSfive.Flow;

import Tenzinn.Core.GameMatch;
import Tenzinn.Core.UI.GameHUD;
import Tenzinn.Core.LootManager;
import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.Objects.PlayerStats;
import Tenzinn.Core.GameMatch.MatchState;
import Tenzinn.FiveVSfive.UI.EndRoundPage;
import Tenzinn.Core.Listeners.MessageListeners;
import Tenzinn.Core.Effects.PlayerEntityEffect;
import Tenzinn.FiveVSfive.Systems.TemporalWallSystem;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;
import java.util.UUID;
import java.util.Objects;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;

public class MatchFVF {

    private static UUID activeMatchId = null;

    private static ScheduledFuture<?> timerTask;
    private static int remainingSeconds = 150;

    private static int timePerRound = 150;
    private static int timePerPurchase = 15;

    public static int winner = -1;
    public static final int numRoundsPerWinner = 4;
    public static ArrayList<Integer> numRoundsPerTeam = new ArrayList<>();
    private static boolean inEndRound = false;
    private static boolean inEndPurchase = false;

    private static GameMatch myMatch;

    private static void prepareRoundScoresFor(GameMatch match) {
        if (match == null) return;

        if (activeMatchId == null || !activeMatchId.equals(match.getMatchId())) {
            activeMatchId = match.getMatchId();
            numRoundsPerTeam.clear();
            winner = -1;
            inEndRound = false;
            inEndPurchase = false;
        }

        while (numRoundsPerTeam.size() < 2) {
            numRoundsPerTeam.add(0);
        }
    }

    public static void startMatch() {
        List<PlayerRef> playerRefs = myMatch.getPlayers();

        World currentWorld = Universe.get().getWorld(Objects.requireNonNull(myMatch.getPlayers().getFirst().getWorldUuid()));
        assert currentWorld != null;

        currentWorld.execute(() -> {
            HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                currentWorld.execute(() -> {
                    for (int i = 0; i < playerRefs.size(); i++) {
                        Ref<EntityStore> ref = playerRefs.get(i).getReference();
                        if (ref == null) continue;

                        Store<EntityStore> store = ref.getStore();
                        Player player = store.getComponent(ref, Player.getComponentType());
                        if (player == null) continue;

                        if (myMatch.getState() == MatchState.ON_PURCHASE || myMatch.getState() == MatchState.STARTING) {
                            PlayerEntityEffect.applyEffect(player, "NotMove", store);
                        }
                    }
                });
            }, 300, TimeUnit.MILLISECONDS);
        });
    }
    // ================================================== //
    public static void startTimerMatch(GameMatch match) {
        prepareRoundScoresFor(match);
        myMatch = match;
        startMatch();

        if (timerTask != null && !timerTask.isDone()) return;

        if (myMatch.getState() == MatchState.IN_PROGRESS) {
            remainingSeconds = timePerRound;
            inEndRound = false;

            String mapName = myMatch.getMapId();
            TemporalWallSystem.removeWalls(mapName, myMatch.getPlayers().getFirst());

            timerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
                if (remainingSeconds <= 0 && !inEndRound) { onEndRound(); }
                if (remainingSeconds <= -5) { onReloadRound(); }

                remainingSeconds--;
            }, 1, 1, TimeUnit.SECONDS);
        } else {
            remainingSeconds = timePerPurchase;
            inEndPurchase = false;

            World currentWorld = Universe.get().getWorld(Objects.requireNonNull(myMatch.getPlayers().getFirst().getWorldUuid()));
            assert currentWorld != null;
            currentWorld.execute(() -> {
                for (int i = 0; i < myMatch.getPlayers().size(); i++) {
                    PlayerStats playerStats = RefactorTool.getPlayerStats(myMatch.getPlayers().get(i));
                    assert playerStats != null;
                    playerStats.canReceivedLoot = true;
                }
            });

            String mapName = myMatch.getMapId();
            TemporalWallSystem.buildWalls(mapName, myMatch.getPlayers().getFirst());

            timerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
                if (remainingSeconds <= 0 && !inEndPurchase) { onEndPurchase(); }
                remainingSeconds--;
            }, 1, 1, TimeUnit.SECONDS);
        }
    }
    // ================================================== //
    public static void onEndPurchase() {
        inEndPurchase = true;
        stopTimer();

        if (myMatch != null) {
            World currentWorld = Universe.get().getWorld(Objects.requireNonNull(myMatch.getPlayers().getFirst().getWorldUuid()));
            assert currentWorld != null;

            // FIX: setState and player setup on the world thread, but startTimerMatch
            // called OUTSIDE execute() with a short delay to avoid nested execute() calls
            // to ensure setState is processed before starting the timer.
            currentWorld.execute(() -> {
                for (int i = 0; i < myMatch.getPlayers().size(); i++) {
                    PlayerStats playerStats = RefactorTool.getPlayerStats(myMatch.getPlayers().get(i));
                    assert playerStats != null;
                    playerStats.canReceivedLoot = false;

                    playerStats.setFinishBuyZone();

                    PlayerRef liveRef = Universe.get().getPlayer(playerStats.getPlayerRef().getUuid());
                    if (liveRef == null || liveRef.getReference() == null) continue;

                    Ref<EntityStore> ref = liveRef.getReference();
                    Store<EntityStore> store = ref.getStore();

                    Player player = store.getComponent(ref, Player.getComponentType());
                    if (player == null) continue;

                    player.getPageManager().setPage(ref, store, Page.None);

                    PlayerEntityEffect.clearAllEffects(player, store);

                    String effect = validateTeamMembership(liveRef) == 1 ? "Ally" : "Enemy";
                    PlayerEntityEffect.applyEffect(player, effect, store);
                }

                String mapName = myMatch.getMapId();
                TemporalWallSystem.removeWalls(mapName, myMatch.getPlayers().getFirst());

                myMatch.setState(GameMatch.MatchState.IN_PROGRESS);
            });

            // FIX: startTimerMatch outside execute() with a minimal delay so
            // the previous setState is applied when the new timer starts.
            HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                startTimerMatch(myMatch);
            }, 100, TimeUnit.MILLISECONDS);
        }
    }
    public static void onReloadRound() {
        stopTimer();

        // FIX: setState before execute() - correct, no changes needed here.
        myMatch.setState(MatchState.ON_PURCHASE);

        World currentWorld = Universe.get().getWorld(Objects.requireNonNull(myMatch.getPlayers().getFirst().getWorldUuid()));
        currentWorld.execute(() -> {
            resetPlayers(myMatch.getPlayers());

            String mapName = myMatch.getMapId();
            TemporalWallSystem.buildWalls(mapName, myMatch.getPlayers().getFirst());

            List<PlayerRef> allPlayers = myMatch.getPlayers();
            for (int i = 0; i < allPlayers.size(); i++) {
                Ref<EntityStore> ref = allPlayers.get(i).getReference();
                assert ref != null;
                Store<EntityStore> store = ref.getStore();
                Player player = store.getComponent(ref, Player.getComponentType());
                player.getPageManager().setPage(ref, store, Page.None);
            }
        });

        // FIX: startTimerMatch outside execute() with a minimal delay for consistency
        // and so resetPlayers/buildWalls are queued before starting the timer.
        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            startTimerMatch(myMatch);
        }, 100, TimeUnit.MILLISECONDS);
    }
    public static void onEndRound() {
        inEndRound = true;
        List<PlayerRef> players = myMatch.getPlayers();

        World currentWorld = Universe.get().getWorld(Objects.requireNonNull(players.getFirst().getWorldUuid()));
        assert currentWorld != null;
        currentWorld.execute(() -> {
            for (int i = 0; i < players.size(); i++) {
                Ref<EntityStore> ref = players.get(i).getReference();
                assert ref != null;
                Store<EntityStore> store = ref.getStore();
                Player player = store.getComponent(ref, Player.getComponentType());
                player.getPageManager().openCustomPage(ref, store, new EndRoundPage(players.get(i)));
            }
        });
    }
    // ================================================== //
    public static void resetPlayers(List<PlayerRef> playerRefs) {
        for (int i = 0; i < playerRefs.size(); i++) {
            Player player = RefactorTool.getPlayer(playerRefs.get(i));
            LootManager.giveLoot(player, LootManager.getStarterKit());

            Ref<EntityStore> ref = playerRefs.get(i).getReference();
            assert ref != null;
            Store<EntityStore> store = ref.getStore();
            EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
            if (statMap != null) { statMap.maximizeStatValue(DefaultEntityStatTypes.getHealth()); }
        }

        teleportPlayers(playerRefs);
    }
    public static void teleportPlayers(List<PlayerRef> playerRefs) {
        ArrayList<Vector3d> spawns = RefactorTool.getSpawns(myMatch.getMapId(), RefactorTool.getModeForPlayer(playerRefs.getFirst()));
        if (spawns.isEmpty()) return;

        assert playerRefs.getFirst().getWorldUuid() != null;
        World newWorld = Universe.get().getWorld(playerRefs.getFirst().getWorldUuid());

        for (int i = 0; i < playerRefs.size(); i++) {
            Vector3d spawnPos = spawns.get(i % spawns.size());
            Transform spawnPoint = new Transform(spawnPos.x + 0.5f, spawnPos.y, spawnPos.z + 0.5f);
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
    public static List<PlayerRef> getPlayers() { return myMatch != null ? myMatch.getPlayers() : new ArrayList<>(); }
    // ================================================== //
    public static int getTimer() { return remainingSeconds; }
    public static void stopTimer() {
        if (timerTask != null && !timerTask.isDone()) {
            timerTask.cancel(false);
        }

        timerTask = null;
    }
    // ================================================== //
    public static int getNumberRound(int team) {
        if(numRoundsPerTeam.isEmpty()) {
            numRoundsPerTeam.add(0);
            numRoundsPerTeam.add(0);

            return 0;
        }

        return numRoundsPerTeam.get(team - 1);
    }
    public static int validateFinishRound() {
        boolean stateTeam01 = true;
        boolean stateTeam02 = true;

        int halfAmount;
        if (myMatch.getPlayers().size() % 2 == 0) { halfAmount = myMatch.getPlayers().size() / 2; }
        else { halfAmount = (myMatch.getPlayers().size() - 1) / 2; }

        for (int i = 0; i < halfAmount; i++) {
            PlayerStats playerStats = RefactorTool.getPlayerStats(myMatch.getPlayers().get(i));
            if (playerStats.playerState == PlayerStats.PlayerState.DEFAULT) { stateTeam01 = false; break; }
        }

        for (int i = halfAmount; i < myMatch.getPlayers().size(); i++) {
            PlayerStats playerStats = RefactorTool.getPlayerStats(myMatch.getPlayers().get(i));
            if (playerStats.playerState == PlayerStats.PlayerState.DEFAULT) { stateTeam02 = false; break; }
        }

        winner = stateTeam01 ? 1 : stateTeam02 ? 2 : 0;
        if (winner > 0) finishRound();

        return winner;
    }
    public static int validateTeamMembership(PlayerRef playerRef) {
        List<PlayerRef> players = myMatch.getPlayers();
        int value = -1;

        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getUuid().equals(playerRef.getUuid())) {
                value = i;
                break;
            }
        }

        int halfAmount;
        if (myMatch.getPlayers().size() % 2 == 0) { halfAmount = myMatch.getPlayers().size() / 2; }
        else { halfAmount = (myMatch.getPlayers().size() - 1) / 2; }

        if (value == -1) return -1;
        if (value >= halfAmount) return 2;

        return 1;
    }
    // ================================================== //
    private static void finishRound () {
        prepareRoundScoresFor(myMatch);

        if (winner < 1 || winner > 2) return;

        onEndRound();

        numRoundsPerTeam.set(winner - 1, numRoundsPerTeam.get(winner - 1) + 1);

        boolean matchFinished =
                numRoundsPerTeam.get(0) >= numRoundsPerWinner
                        || numRoundsPerTeam.get(1) >= numRoundsPerWinner;

        if (!matchFinished) {
            HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                if (myMatch != null && myMatch.getState() != GameMatch.MatchState.FINISHED) {
                    onReloadRound();
                }
            }, 5, TimeUnit.SECONDS);
        }

        List<PlayerRef> players = myMatch.getPlayers();

        for (int i = 0; i < players.size(); i++) {
            Player player = RefactorTool.getPlayer(players.get(i));
            GameHUD customHUD = (GameHUD) player.getHudManager().getCustomHud();
            assert customHUD != null;
            customHUD.setRounds(numRoundsPerTeam.getFirst(), numRoundsPerTeam.getLast());

            if (numRoundsPerTeam.getFirst() >= numRoundsPerWinner || numRoundsPerTeam.get(1) >= numRoundsPerWinner) {
                assert player.getReference() != null;
                PlayerEntityEffect.clearAllEffects(player, player.getReference().getStore());
            }
        }

        if (matchFinished) {
            RefactorTool.finishGame(myMatch.getPlayers(), myMatch);
        }
    }
}
