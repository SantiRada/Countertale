package Tenzinn.Deathmatch;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class MatchManager {

    private final List<GameMatch> activeMatches;
    private final Map<String, GameMatch> playerMatches;

    public MatchManager() { this.activeMatches = new ArrayList<>(); this.playerMatches = new ConcurrentHashMap<>(); }
    public GameMatch addPlayerToQueue(PlayerRef playerRef) {
        String playerId = playerRef.getUuid().toString();

        if (playerMatches.containsKey(playerId)) { return playerMatches.get(playerId); }

        Optional<GameMatch> availableMatch = activeMatches.stream().filter(match -> match.getState() == GameMatch.MatchState.WAITING).filter(match -> !match.isFull()).findFirst();

        GameMatch match;
        if (availableMatch.isPresent()) {
            // Join a game
            match = availableMatch.get();
            match.addPlayer(playerRef);
        } else {
            // Create new game
            match = new GameMatch();
            match.addPlayer(playerRef);
            activeMatches.add(match);
        }

        playerMatches.put(playerId, match);
        return match;
    }
    public boolean removePlayerFromMatch(PlayerRef playerRef) {
        String playerId = playerRef.getUuid().toString();
        GameMatch match = playerMatches.remove(playerId);

        if (match == null) return false;

        match.removePlayer(playerRef);

        if (match.isEmpty() && match.getState() == GameMatch.MatchState.WAITING) activeMatches.remove(match);

        return true;
    }
    public GameMatch getPlayerMatch(PlayerRef playerRef) {
        String playerId = playerRef.getUuid().toString();
        return playerMatches.get(playerId);
    }
    public boolean isPlayerInMatch(PlayerRef playerRef) {
        String playerId = playerRef.getUuid().toString();
        return playerMatches.containsKey(playerId);
    }
    public List<GameMatch> getActiveMatches() { return new ArrayList<>(activeMatches); }
    public List<GameMatch> getFullMatches() { return activeMatches.stream().filter(GameMatch::isFull).filter(match -> match.getState() == GameMatch.MatchState.WAITING).toList(); }
    public String getStats() {
        int totalMatches = activeMatches.size();
        int waitingMatches = (int) activeMatches.stream().filter(m -> m.getState() == GameMatch.MatchState.WAITING).count();
        int inProgressMatches = (int) activeMatches.stream().filter(m -> m.getState() == GameMatch.MatchState.IN_PROGRESS).count();
        int totalPlayers = playerMatches.size();

        return String.format("Partidas totales: %d | Esperando: %d | En curso: %d | Jugadores totales: %d", totalMatches, waitingMatches, inProgressMatches, totalPlayers);
    }
}