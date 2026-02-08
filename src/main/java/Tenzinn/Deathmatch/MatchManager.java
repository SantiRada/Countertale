package Tenzinn.Deathmatch;

import Tenzinn.Countertale;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class MatchManager {

    private final List<GameMatch> activeMatches;
    private final Map<String, GameMatch> playerMatches;
    private static Map<String, GameMatch> staticMatches;
    private final Countertale main;

    public MatchManager(Countertale main) {
        this.main = main;
        this.activeMatches = new ArrayList<>();
        this.playerMatches = new ConcurrentHashMap<>();
        this.staticMatches = new ConcurrentHashMap<>();
    }
    public GameMatch addPlayerToQueue(PlayerRef playerRef, Countertale main) {
        String playerId = playerRef.getUuid().toString();

        if (playerMatches.containsKey(playerId)) { return playerMatches.get(playerId); }

        Optional<GameMatch> availableMatch = activeMatches.stream().filter(match -> match.getState() == GameMatch.MatchState.WAITING).filter(match -> !match.isFull()).findFirst();

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

            match.getInstance().preloadMap();
        }

        playerMatches.put(playerId, match);
        staticMatches.put(playerId, match);
        return match;
    }
    public boolean removePlayerFromMatch(PlayerRef playerRef) {
        String playerId = playerRef.getUuid().toString();

        main.getLogger().at(Level.INFO).log("=== INICIANDO REMOCIÓN DE JUGADOR ===");
        main.getLogger().at(Level.INFO).log("Player ID: " + playerId);
        main.getLogger().at(Level.INFO).log("Jugadores en playerMatches antes: " + playerMatches.size());
        main.getLogger().at(Level.INFO).log("Keys en playerMatches: " + playerMatches.keySet());

        GameMatch match = playerMatches.get(playerId);

        if (match == null) {
            main.getLogger().at(Level.WARNING).log("❌ Match es NULL para jugador: " + playerId);
            main.getLogger().at(Level.WARNING).log("El jugador no está en ninguna partida registrada");
            return false;
        }

        main.getLogger().at(Level.INFO).log("✅ Match encontrado: " + match.getMatchId());
        main.getLogger().at(Level.INFO).log("Jugadores en la match antes de remover: " + match.getPlayers().size());

        boolean removed = match.removePlayer(playerRef);
        main.getLogger().at(Level.INFO).log("Jugador removido de la lista: " + removed);

        playerMatches.remove(playerId);
        staticMatches.remove(playerId);

        main.getLogger().at(Level.INFO).log("Jugadores restantes en la match: " + match.getPlayers().size());
        main.getLogger().at(Level.INFO).log("Jugadores en playerMatches después: " + playerMatches.size());

        if (match.getPlayers().isEmpty()) {
            main.getLogger().at(Level.INFO).log("🗑️ Match vacía, programando eliminación de instancia en 2 segundos...");

            CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                main.getLogger().at(Level.INFO).log("⏰ Tiempo cumplido, eliminando instancia...");
                match.removeInstance();
                activeMatches.remove(match);
                main.getLogger().at(Level.INFO).log("✅ Instancia y match eliminadas!");
                main.getLogger().at(Level.INFO).log("Matches activas restantes: " + activeMatches.size());
            });
        } else { main.getLogger().at(Level.INFO).log("⚠️ Match aún tiene jugadores, no se eliminará"); }

        main.getLogger().at(Level.INFO).log("=== FIN REMOCIÓN DE JUGADOR ===");
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
    public static boolean verifyPlayerInMatch(PlayerRef playerRef) {
        String playerId = playerRef.getUuid().toString();
        return staticMatches.containsKey(playerId);
    }
}