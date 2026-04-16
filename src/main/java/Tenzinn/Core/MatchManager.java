package Tenzinn.Core;

import Tenzinn.Countertale;
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
    private final Countertale                main;

    public MatchManager(Countertale main) {
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

        // Si el jugador no trajo votos (acceso directo sin ModesPage), acepta todos los mapas
        List<String> effectiveAllowed = (allowedMaps == null || allowedMaps.isEmpty())
                ? new ArrayList<>(MapListeners.getMapNames())
                : allowedMaps;

        // Buscar partida compatible: mismo modo Y al menos un mapa en común
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
            // Reducir los mapas elegibles a la intersección con los del nuevo jugador
            match.intersectEligibleMaps(effectiveAllowed);

            main.getLogger().at(Level.INFO).log(
                    "[MatchManager] Jugador unido a match existente. Mapas elegibles restantes: "
                    + match.getEligibleMaps());
        } else {
            // Nueva partida; la instancia se asigna cuando se complete en startMatch
            match = new GameMatch();
            match.addPlayer(playerRef);
            match.setMode(mode);
            match.initEligibleMaps(effectiveAllowed);
            activeMatches.add(match);

            main.getLogger().at(Level.INFO).log(
                    "[MatchManager] Nueva partida creada. Mapas elegibles: " + effectiveAllowed);
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
                    "removePlayerFromMatch: PlayerStats no encontrado para " + playerRef.getUuid());
            return false;
        }

        GameMatch match = playerMatches.get(playerStats);
        RefactorTool.setQuitPlayerStats(playerStats);

        // Limpiar votos pendientes del jugador (por si salió sin consumirlos del todo)
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
                    // Partida que nunca empezó pero ya tenía instancia asignada
                    match.removeInstance();
                }

                activeMatches.remove(match);
                main.getLogger().at(Level.INFO).log("[MatchManager] Match vacío finalizado y removido.");
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
        return String.format("Partida en espera: %d | En curso: %d | Totales: %d",
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
        return String.format("Jugadores en cola: %d | En partida: %d | Totales: %d",
                waiting, inGame, playerMatches.size());
    }

    public String getInstances() {
        long loaded     = activeMatches.stream()
                .filter(m -> m.getInstance() != null && m.getInstance().getMapLoaded()).count();
        long preloading = activeMatches.stream()
                .filter(m -> m.getInstance() != null && !m.getInstance().getMapLoaded()).count();
        return String.format("Pool listo: %d | Pool en creación: %d | En uso: %d | Precargando para match: %d",
                instancePool.size(), instancePool.getBeingCreated(), loaded, preloading);
    }
}
