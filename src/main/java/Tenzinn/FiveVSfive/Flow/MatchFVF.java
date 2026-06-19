package Tenzinn.FiveVSfive.Flow;

import Tenzinn.Core.GameMatch;
import Tenzinn.Core.UI.GameHUD;
import Tenzinn.Core.LootManager;
import Tenzinn.Core.Shop.EconomySystem;
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
import org.joml.Vector3d;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Objects;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;

public class MatchFVF {

    private static ScheduledFuture<?> timerTask;
    private static int remainingSeconds = 150;

    private static int timePerRound = 150;
    private static int timePerPurchase = 15;

    public static int winner = -1;
    public static final int numRoundsPerWinner = 13;
    public static ArrayList<Integer> numRoundsPerTeam = new ArrayList<>();
    private static boolean inEndRound = false;
    private static boolean inEndPurchase = false;

    private static GameMatch myMatch;

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
        // Reset all per-match static state when a genuinely new match begins.
        // myMatch == match means we are resuming a round within the same match (fine).
        // myMatch != match means this is a fresh game — stale state from the previous
        // match would corrupt round counts and leave a leaked timer blocking the new one.
        if (myMatch != match) {
            stopTimer();                    // cancel any leaked timer from the previous match
            numRoundsPerTeam.clear();       // round scores must start at 0-0
            winner       = -1;
            inEndRound   = false;
            inEndPurchase = false;
        }

        myMatch = match;

        // Assign teams once per match (idempotent — ignored on subsequent rounds)
        if (myMatch.getTeamAssignments().isEmpty()) { myMatch.formTeams(); }

        startMatch();

        if (timerTask != null && !timerTask.isDone()) return;

        if (myMatch.getState() == MatchState.IN_PROGRESS) {
            remainingSeconds = timePerRound;
            inEndRound = false;
            winner = -1;

            String mapName = "Dust2";
            TemporalWallSystem.removeWalls(mapName, myMatch.getPlayers().getFirst());

            timerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
                if (remainingSeconds <= 0 && !inEndRound) { onTimeExpired(); }
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

            String mapName = "Dust2";
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

            // FIX: setState y setup de players en el world thread, pero startTimerMatch
            // se llama FUERA del execute() con un pequeño delay para evitar execute() anidados
            // y para garantizar que el setState ya fue procesado antes de arrancar el timer.
            currentWorld.execute(() -> {
                for (int i = 0; i < myMatch.getPlayers().size(); i++) {
                    PlayerStats playerStats = RefactorTool.getPlayerStats(myMatch.getPlayers().get(i));
                    assert playerStats != null;
                    playerStats.canReceivedLoot = false;

                    playerStats.setFinishBuyZone();

                    Ref<EntityStore> ref = playerStats.getPlayerRef().getReference();
                    assert ref != null;
                    Store<EntityStore> store = ref.getStore();

                    playerStats.getPlayer().getPageManager().setPage(ref, store, Page.None);

                    PlayerEntityEffect.clearAllEffects(playerStats.getPlayer(), store);

                    String effect = validateTeamMembership(myMatch.getPlayers().get(i)) == 1 ? "Ally" : "Enemy";
                    PlayerEntityEffect.applyEffect(playerStats.getPlayer(), effect, store);
                }

                String mapName = "Dust2";
                TemporalWallSystem.removeWalls(mapName, myMatch.getPlayers().getFirst());

                myMatch.setState(GameMatch.MatchState.IN_PROGRESS);
            });

            // FIX: startTimerMatch fuera del execute() con delay mínimo para que
            // el setState anterior ya esté aplicado cuando arranque el nuevo timer.
            HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                startTimerMatch(myMatch);
            }, 100, TimeUnit.MILLISECONDS);
        }
    }
    public static void onReloadRound() {
        stopTimer();
        giveRoundMoney();

        myMatch.setState(MatchState.ON_PURCHASE);

        World currentWorld = Universe.get().getWorld(Objects.requireNonNull(myMatch.getPlayers().getFirst().getWorldUuid()));
        currentWorld.execute(() -> {
            resetPlayers(myMatch.getPlayers());

            String mapName = "Dust2";
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

        // FIX: startTimerMatch fuera del execute() con delay mínimo para consistencia
        // y para que el resetPlayers/buildWalls ya estén encolados antes de arrancar el timer.
        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            startTimerMatch(myMatch);
        }, 100, TimeUnit.MILLISECONDS);
    }
    public static void onEndRound() {
        inEndRound = true;
        stopTimer();

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

        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> onReloadRound(), 3, TimeUnit.SECONDS);
    }
    /** Called when the round timer reaches 0. Determines winner by alive-player rules and closes the round. */
    public static void onTimeExpired() {
        inEndRound = true;
        stopTimer();

        boolean team1HasAlive = false;
        boolean team2HasAlive = false;

        for (PlayerRef playerRef : myMatch.getPlayers()) {
            PlayerStats ps = RefactorTool.getPlayerStats(playerRef);
            if (ps == null) continue;
            int team = validateTeamMembership(playerRef);
            if (ps.playerState == PlayerStats.PlayerState.DEFAULT) {
                if (team == 1) team1HasAlive = true;
                else if (team == 2) team2HasAlive = true;
            }
        }

        // Rule 1: only one team alive → that team wins
        // Rule 2: both alive (or both dead) → team 1 wins
        winner = (!team1HasAlive && team2HasAlive) ? 2 : 1;

        finishRound();
    }
    // ================================================== //
    public static void resetPlayers(List<PlayerRef> playerRefs) {
        for (int i = 0; i < playerRefs.size(); i++) {
            PlayerRef playerRef = playerRefs.get(i);
            Ref<EntityStore> ref = playerRef.getReference();
            assert ref != null;
            Store<EntityStore> store = ref.getStore();

            PlayerStats ps = RefactorTool.getPlayerStats(playerRef);
            if (ps != null && ps.playerState == PlayerStats.PlayerState.SPECTATOR) {
                ps.playerState = PlayerStats.PlayerState.DEFAULT;
                DeathComponent.respawn(store, ref);
            }

            Player player = RefactorTool.getPlayer(playerRef);
            LootManager.giveLoot(player, LootManager.getStarterKit());

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

        Map<UUID, Integer> teams = myMatch.getTeamAssignments();
        int team1Count = 0;
        int team2Count = 0;

        for (PlayerRef playerRef : playerRefs) {
            int team = teams.getOrDefault(playerRef.getUuid(), 1);
            // Team 1 → spawns [0..4], Team 2 → spawns [5..9]
            int spawnIndex = (team == 1) ? team1Count++ : 5 + team2Count++;
            Vector3d spawnPos = spawns.get(spawnIndex % spawns.size());
            Vector3d spawnPoint = new Vector3d(spawnPos.x + 0.5f, spawnPos.y, spawnPos.z + 0.5f);

            try {
                UUID playerUUID = playerRef.getUuid();
                PlayerRef updatedPlayerRef = Universe.get().getPlayer(playerUUID);

                if (updatedPlayerRef == null || updatedPlayerRef.getReference() == null) continue;

                Ref<EntityStore> ref = updatedPlayerRef.getReference();
                assert updatedPlayerRef.getWorldUuid() != null;

                World currentWorld = Universe.get().getWorld(updatedPlayerRef.getWorldUuid());
                if (currentWorld == null) continue;

                updatedPlayerRef.sendMessage(MessageListeners.message(MessageListeners.MessageKey.CHAT_TELEPORTING_GAME));

                currentWorld.execute(() -> {
                    try {
                        Store<EntityStore> store = currentWorld.getEntityStore().getStore();
                        Teleport teleport = Teleport.createForPlayer(newWorld, spawnPoint, Rotation3f.ZERO);
                        store.addComponent(ref, Teleport.getComponentType(), teleport);
                    } catch (Exception e) { e.printStackTrace(); }
                });

            } catch (Exception e) { e.printStackTrace(); }
        }
    }
    public static List<PlayerRef> getPlayers() { return myMatch != null ? myMatch.getPlayers() : new ArrayList<>(); }
    public static boolean isMatchActive()       { return myMatch != null; }
    // ── Admin round control ────────────────────────────────────────────────────
    /** Adds one round win to {@code team} (1 or 2) and resolves the round normally. */
    public static void forceAddRound(int team) {
        if (myMatch == null) return;
        getNumberRound(1); // initialises numRoundsPerTeam if empty
        winner = team;
        finishRound();
    }
    /** Sets {@code team} (1 or 2) as the match winner immediately by granting the required rounds. */
    public static void forceWinMatch(int team) {
        if (myMatch == null) return;
        getNumberRound(1); // initialises numRoundsPerTeam if empty
        numRoundsPerTeam.set(team - 1, numRoundsPerWinner - 1); // finishRound will add the last one
        winner = team;
        finishRound();
    }
    // ================================================== //
    public static int getTimer() { return remainingSeconds; }
    public static void stopTimer() { if (timerTask != null && !timerTask.isDone()) timerTask.cancel(false); }

    public static boolean forceEndMatchIn(int seconds) {
        if (myMatch == null) return false;

        getNumberRound(1);
        numRoundsPerTeam.set(0, numRoundsPerWinner - 1);
        numRoundsPerTeam.set(1, numRoundsPerWinner - 1);

        if (myMatch.getState() != MatchState.IN_PROGRESS || timerTask == null || timerTask.isDone()) {
            myMatch.setState(MatchState.IN_PROGRESS);
            stopTimer();
            timerTask = null;
            startTimerMatch(myMatch);
        }

        inEndRound = false;
        remainingSeconds = Math.max(0, seconds);
        return true;
    }

    public static void clearRuntimeState() {
        stopTimer();
        timerTask = null;
        remainingSeconds = 150;
        winner = -1;
        numRoundsPerTeam.clear();
        inEndRound = false;
        inEndPurchase = false;
        myMatch = null;
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

        for (PlayerRef playerRef : myMatch.getPlayers()) {
            PlayerStats playerStats = RefactorTool.getPlayerStats(playerRef);
            if (playerStats == null) continue;
            int team = validateTeamMembership(playerRef);
            if (team == 1 && playerStats.playerState == PlayerStats.PlayerState.DEFAULT) { stateTeam01 = false; }
            else if (team == 2 && playerStats.playerState == PlayerStats.PlayerState.DEFAULT) { stateTeam02 = false; }
        }

        winner = stateTeam01 ? 1 : stateTeam02 ? 2 : 0;
        if (winner > 0) finishRound();

        return winner;
    }
    public static int validateTeamMembership(PlayerRef playerRef) {
        Map<UUID, Integer> assignments = myMatch.getTeamAssignments();
        Integer team = assignments.get(playerRef.getUuid());
        return team != null ? team : -1;
    }
    // ================================================== //
    private static void giveRoundMoney() {
        if (myMatch == null || winner <= 0) return;
        int bonus = EconomySystem.getRevenue(EconomySystem.Revenue.ROUND);
        for (PlayerRef playerRef : myMatch.getPlayers()) {
            PlayerStats ps = RefactorTool.getPlayerStats(playerRef);
            if (ps == null) continue;
            if (validateTeamMembership(playerRef) == winner) {
                ps.giveMoney(bonus);
            }
        }
    }
    private static void finishRound() {
        numRoundsPerTeam.set(winner - 1, numRoundsPerTeam.get(winner - 1) + 1);

        boolean matchOver = numRoundsPerTeam.getFirst() >= numRoundsPerWinner
                         || numRoundsPerTeam.get(1) >= numRoundsPerWinner;

        List<PlayerRef> players = myMatch.getPlayers();
        for (int i = 0; i < players.size(); i++) {
            PlayerRef playerRef = players.get(i);
            Player player = RefactorTool.getPlayer(playerRef);
            if (player == null) continue;

            if (player.getHudManager().getCustomHud(playerRef.getUuid().toString()) instanceof GameHUD customHUD) {
                customHUD.setRounds(numRoundsPerTeam.getFirst(), numRoundsPerTeam.getLast());
            }

            if (matchOver) {
                assert player.getReference() != null;
                PlayerEntityEffect.clearAllEffects(player, player.getReference().getStore());
            }
        }

        if (matchOver) {
            RefactorTool.finishGame(myMatch.getPlayers(), myMatch);
        } else {
            onEndRound();
        }
    }
}
