package Tenzinn.Core;

import Tenzinn.OrbisOffensive;
import Tenzinn.Core.Instances.MapVoteStore;
import Tenzinn.Core.Listeners.MapListeners;
import Tenzinn.Core.Listeners.MessageListeners;
import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.Objects.PlayerStats;
import Tenzinn.Core.Instances.InstancePool;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class MatchManager implements InstancePool.MatchManagerInstanceCounter {

    private final List<GameMatch>            activeMatches;
    private final Map<PlayerStats, GameMatch> playerMatches;
    private final InstancePool               instancePool;
    private final OrbisOffensive                main;

    public MatchManager(OrbisOffensive main) {
        this.main          = main;
        this.activeMatches = new ArrayList<>();
        this.playerMatches = new ConcurrentHashMap<>();
        this.instancePool  = new InstancePool(main);
        this.instancePool.setCounter(this);
    }

    public InstancePool getInstancePool() { return instancePool; }

    @Override
    public int getActiveInstanceCount() {
        return (int) activeMatches.stream()
                .filter(m -> m.getInstance() != null)
                .count();
    }

    public GameMatch addPlayerToQueue(PlayerRef playerRef, String mode, List<String> allowedMaps) {

        boolean isInList = playerMatches.keySet().stream()
                .anyMatch(s -> s.getPlayerRef().equals(playerRef));

        if (isInList) {
            playerRef.sendMessage(Message.raw(
                    MessageListeners.get(MessageListeners.MessageKey.CHAT_IN_QUEUE_X2)).color(Color.pink));
            return null;
        }

        // If the player brought no votes (direct access without ModesPage), accept all maps.
        List<String> effectiveAllowed = (allowedMaps == null || allowedMaps.isEmpty())
                ? new ArrayList<>(MapListeners.getMapNames())
                : allowedMaps;

        // Find a compatible match: same mode and at least one map in common.
        Optional<GameMatch> availableMatch = activeMatches.stream()
                .filter(m -> m.getState() == GameMatch.MatchState.WAITING)
                .filter(m -> !m.isFull())
                .filter(m -> mode.equals(m.getMode()))
                .filter(m -> !Collections.disjoint(m.getEligibleMaps(), effectiveAllowed))
                .findFirst();

        GameMatch match;

        if (availableMatch.isPresent()) {
            match = availableMatch.get();
            if (!match.getPlayers().contains(playerRef)) {
                match.addPlayer(playerRef);
            }
            // Reduce eligible maps to the intersection with the new player's maps.
            match.intersectEligibleMaps(effectiveAllowed);

            main.getLogger().at(Level.INFO).log(
                    "[MatchManager] Player joined existing match. Remaining eligible maps: "
                    + match.getEligibleMaps());
        } else {
            // New match; the instance is assigned when startMatch completes.
            match = new GameMatch();
            match.addPlayer(playerRef);
            match.setMode(mode);
            match.initEligibleMaps(effectiveAllowed);
            activeMatches.add(match);

            main.getLogger().at(Level.INFO).log(
                    "[MatchManager] New match created. Eligible maps: " + effectiveAllowed);
        }

        PlayerStats playerStats = new PlayerStats(playerRef, RefactorTool.getPlayer(playerRef), match);
        playerMatches.put(playerStats, match);
        RefactorTool.setPlayerStats(playerStats);
        return match;
    }

    public boolean removePlayerFromMatch(PlayerRef playerRef) {
        PlayerStats playerStats = RefactorTool.getPlayerStats(playerRef);

        if (playerStats == null) {
            main.getLogger().at(Level.WARNING).log(
                    "removePlayerFromMatch: PlayerStats not found for " + playerRef.getUuid());
            return false;
        }

        GameMatch match = playerMatches.get(playerStats);
        RefactorTool.setQuitPlayerStats(playerStats);

        // Clear pending player votes (in case they left before consuming all of them).
        MapVoteStore.clearVotes(playerRef);

        if (match == null) return false;

        match.removePlayer(playerRef);
        playerMatches.remove(playerStats);

        if (match.getPlayers().isEmpty()) {
            CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                match.stopTimer();

                // onMatchFinished registra popularidad, destruye la instancia y rebalancea el pool
                if (match.getMapId() != null && match.getInstance() != null) {
                    instancePool.onMatchFinished(match.getMapId(), match.getInstance());
                } else if (match.getInstance() != null) {
                    // Match that never started but already had an assigned instance.
                    match.removeInstance();
                }

                activeMatches.remove(match);
                main.getLogger().at(Level.INFO).log("[MatchManager] Empty match finished and removed.");
            });
        }

        return true;
    }

    public GameMatch getPlayerMatch(PlayerRef playerRef) {
        PlayerStats ps = RefactorTool.getPlayerStats(playerRef);
        return ps != null ? ps.getCurrentMatch() : null;
    }

    public boolean isPlayerInMatch(PlayerRef playerRef) {
        return RefactorTool.getPlayerStats(playerRef) != null;
    }

    public List<GameMatch> getActiveMatches() { return new ArrayList<>(activeMatches); }

    public List<GameMatch> getFullMatches() {
        return activeMatches.stream()
                .filter(GameMatch::isFull)
                .filter(m -> m.getState() == GameMatch.MatchState.WAITING)
                .toList();
    }

    public String getStats() {
        int total      = activeMatches.size();
        int waiting    = (int) activeMatches.stream()
                .filter(m -> m.getState() == GameMatch.MatchState.WAITING).count();
        int inProgress = (int) activeMatches.stream()
                .filter(m -> m.getState() == GameMatch.MatchState.IN_PROGRESS).count();
        return String.format("Waiting matches: %d | In progress: %d | Total: %d",
                waiting, inProgress, total);
    }

    public String getPlayers() {
        int waiting = 0, inGame = 0;
        for (GameMatch m : activeMatches) {
            if (m.getState() == GameMatch.MatchState.WAITING)
                waiting += m.getPlayerCount();
            if (m.getState() == GameMatch.MatchState.IN_PROGRESS ||
                    m.getState() == GameMatch.MatchState.STARTING)
                inGame += m.getPlayerCount();
        }
        return String.format("Players in queue: %d | In match: %d | Total: %d",
                waiting, inGame, playerMatches.size());
    }

    public String getInstances() {
        long loaded     = activeMatches.stream()
                .filter(m -> m.getInstance() != null && m.getInstance().getMapLoaded()).count();
        long preloading = activeMatches.stream()
                .filter(m -> m.getInstance() != null && !m.getInstance().getMapLoaded()).count();
        return String.format("Pool ready: %d | Pool creating: %d | In use: %d | Preloading for match: %d",
                instancePool.size(), instancePool.getBeingCreated(), loaded, preloading);
    }
}
