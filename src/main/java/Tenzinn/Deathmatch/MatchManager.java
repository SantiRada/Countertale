package Tenzinn.Deathmatch;

import Tenzinn.Countertale;
import Tenzinn.Tools.RefactorTool;
import com.hypixel.hytale.server.core.Message;
import Tenzinn.Deathmatch.Objects.PlayerStats;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.awt.*;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

public class MatchManager {

    private final List<GameMatch> activeMatches;
    private final Map<PlayerStats, GameMatch> playerMatches;

    public MatchManager() {
        this.activeMatches = new ArrayList<>();
        this.playerMatches = new ConcurrentHashMap<>();
    }
    public GameMatch addPlayerToQueue(PlayerRef playerRef, Countertale main) {

        boolean isInList = false;

        for (PlayerStats stats : playerMatches.keySet()) {
            if (stats.getPlayerRef().equals(playerRef)) {
                isInList = true;
                break;
            }
        }

        if(isInList) {
            playerRef.sendMessage(Message.raw("Ya estás en la cola, no puedes unirte a otra partida ahora.").color(Color.pink));
            return null;
        }

        Optional<GameMatch> availableMatch = activeMatches.stream()
                .filter(match -> match.getState() == GameMatch.MatchState.WAITING)
                .filter(match -> !match.isFull()).findFirst();

        GameMatch match;
        if (availableMatch.isPresent()) {
            // Join a game
            match = availableMatch.get();

            if(!match.getPlayers().contains(playerRef)) match.addPlayer(playerRef);
        } else {
            // Create new game
            match = new GameMatch();
            match.addPlayer(playerRef);
            activeMatches.add(match);

            match.setInstance(main);

            match.getInstance().preloadMap(() -> {
                match.getInstance().teleportPlayers(match.getPlayers());
            });
        }

        PlayerStats playerStats = new PlayerStats(playerRef, RefactorTool.getPlayer(playerRef), match);

        playerMatches.put(playerStats, match);
        RefactorTool.setPlayerStats(playerStats);
        return match;
    }
    public boolean removePlayerFromMatch(PlayerRef playerRef) {
        PlayerStats playerStats = RefactorTool.getPlayerStats(playerRef);

        GameMatch match = playerMatches.get(playerStats);

        if (match == null) { return false; }

        match.removePlayer(playerRef);
        playerMatches.remove(playerStats);

        if (match.getPlayers().isEmpty()) {
            CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                match.stopTimer();
                match.removeInstance();
                activeMatches.remove(match);
            });
        }

        return true;
    }
    public GameMatch getPlayerMatch(PlayerRef playerRef) {
        GameMatch match = RefactorTool.getPlayerStats(playerRef).getCurrentMatch();
        return match;
    }
    public boolean isPlayerInMatch(PlayerRef playerRef) {
        PlayerStats playerStats = RefactorTool.getPlayerStats(playerRef);

        return playerStats != null;
    }
    public List<GameMatch> getActiveMatches() { return new ArrayList<>(activeMatches); }
    public List<GameMatch> getFullMatches() { return activeMatches.stream().filter(GameMatch::isFull).filter(match -> match.getState() == GameMatch.MatchState.WAITING).toList(); }
    public String getStats() {
        int totalMatches = activeMatches.size();
        int waitingMatches = (int) activeMatches.stream().filter(m -> m.getState() == GameMatch.MatchState.WAITING).count();
        int inProgressMatches = (int) activeMatches.stream().filter(m -> m.getState() == GameMatch.MatchState.IN_PROGRESS).count();

        return String.format("Partida en espera: %d | En curso: %d | Totales: %d", waitingMatches, inProgressMatches, totalMatches);
    }
    public String getPlayers() {
        int totalPlayers = playerMatches.size();
        int waitingPlayers = 0;
        int inGamePlayers = 0;

        for (int i = 0; i < activeMatches.size(); i++) {
            if(activeMatches.get(i).getState() == GameMatch.MatchState.WAITING) { waitingPlayers += activeMatches.get(i).getPlayerCount(); }
            if(activeMatches.get(i).getState() == GameMatch.MatchState.IN_PROGRESS || activeMatches.get(i).getState() == GameMatch.MatchState.STARTING) { inGamePlayers += activeMatches.get(i).getPlayerCount(); }
        }

        return String.format("Jugadores en cola: %d | En partida: %d | Totales: %d", waitingPlayers, inGamePlayers, totalPlayers);
    }
    public String getInstances() {
        int totalInstances = 0;
        int inPrePloadInstance = 0;

        for (int i = 0; i < activeMatches.size(); i++) {
            if (activeMatches.get(i).getInstance().getMapLoaded()) { totalInstances += 1; }
            else { inPrePloadInstance += 1; }
        }

        return String.format("Preload Instance: %d | Total Instances: %d", inPrePloadInstance, totalInstances);
    }
}