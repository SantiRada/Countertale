package Tenzinn.Deathmatch;

import Tenzinn.Countertale;
import Tenzinn.Listeners.MessageListeners;
import Tenzinn.Tools.RefactorTool;
import Tenzinn.Deathmatch.Objects.PlayerStats;
import com.hypixel.hytale.server.core.Message;
import Tenzinn.Deathmatch.Instances.InstancePool;
import Tenzinn.Deathmatch.Instances.InstanceManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.awt.*;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class MatchManager implements InstancePool.MatchManagerInstanceCounter {

    private final List<GameMatch> activeMatches;
    private final Map<PlayerStats, GameMatch> playerMatches;
    private final InstancePool instancePool;
    private Countertale main;

    public MatchManager(Countertale main) {
        this.main = main;
        this.activeMatches  = new ArrayList<>();
        this.playerMatches  = new ConcurrentHashMap<>();

        this.instancePool = new InstancePool(main);
        this.instancePool.setCounter(this);
    }

    public void initPool() { instancePool.refill(); }

    @Override
    public int getActiveInstanceCount() { return (int) activeMatches.stream().filter(m -> m.getInstance() != null).count(); }

    public GameMatch addPlayerToQueue(PlayerRef playerRef) {

        boolean isInList = playerMatches.keySet().stream().anyMatch(s -> s.getPlayerRef().equals(playerRef));

        if (isInList) { playerRef.sendMessage(Message.raw(MessageListeners.get(MessageListeners.MessageKey.CHAT_IN_QUEUE_X2)).color(Color.pink)); return null; }

        // Buscar partida disponible o crear una nueva
        Optional<GameMatch> availableMatch = activeMatches.stream()
                .filter(m -> m.getState() == GameMatch.MatchState.WAITING)
                .filter(m -> !m.isFull()).findFirst();

        GameMatch match;

        if (availableMatch.isPresent()) {
            // --- Unirse a partida existente ---
            match = availableMatch.get();
            if (!match.getPlayers().contains(playerRef)) match.addPlayer(playerRef);

        } else {
            // --- Crear nueva partida ---
            match = new GameMatch();
            match.addPlayer(playerRef);
            activeMatches.add(match);

            InstanceManager pooledInstance = instancePool.take();

            if (pooledInstance != null) {
                match.setInstance(pooledInstance);
                main.getLogger().at(java.util.logging.Level.INFO).log("[MatchManager] ✓ Instancia tomada del pool, lista para usar.");
            } else {
                main.getLogger().at(java.util.logging.Level.WARNING).log("[MatchManager] Pool vacío, creando instancia on-demand.");

                match.setInstance(main);
            }
        }

        PlayerStats playerStats = new PlayerStats(playerRef, RefactorTool.getPlayer(playerRef), match);
        playerMatches.put(playerStats, match);
        RefactorTool.setPlayerStats(playerStats);
        return match;
    }

    public boolean removePlayerFromMatch(PlayerRef playerRef) {
        PlayerStats playerStats = RefactorTool.getPlayerStats(playerRef);

        if (playerStats == null) {
            main.getLogger().at(Level.WARNING).log("removePlayerFromMatch: PlayerStats no encontrado para " + playerRef.getUuid());
            return false;
        }

        GameMatch match = playerMatches.get(playerStats);
        RefactorTool.setQuitPlayerStats(playerStats);

        if (match == null) return false;

        match.removePlayer(playerRef);
        playerMatches.remove(playerStats);

        if (match.getPlayers().isEmpty()) {
            CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                match.stopTimer();
                match.removeInstance();
                activeMatches.remove(match);

                instancePool.refill();
            });
        }

        return true;
    }

    public GameMatch getPlayerMatch(PlayerRef playerRef) {
        return RefactorTool.getPlayerStats(playerRef).getCurrentMatch();
    }

    public boolean isPlayerInMatch(PlayerRef playerRef) {
        return RefactorTool.getPlayerStats(playerRef) != null;
    }

    public List<GameMatch> getActiveMatches()  { return new ArrayList<>(activeMatches); }

    public List<GameMatch> getFullMatches() {
        return activeMatches.stream()
                .filter(GameMatch::isFull)
                .filter(m -> m.getState() == GameMatch.MatchState.WAITING)
                .toList();
    }

    public String getStats() {
        int total      = activeMatches.size();
        int waiting    = (int) activeMatches.stream().filter(m -> m.getState() == GameMatch.MatchState.WAITING).count();
        int inProgress = (int) activeMatches.stream().filter(m -> m.getState() == GameMatch.MatchState.IN_PROGRESS).count();
        return String.format("Partida en espera: %d | En curso: %d | Totales: %d", waiting, inProgress, total);
    }

    public String getPlayers() {
        int waiting  = 0, inGame = 0;
        for (GameMatch m : activeMatches) {
            if (m.getState() == GameMatch.MatchState.WAITING)       waiting += m.getPlayerCount();
            if (m.getState() == GameMatch.MatchState.IN_PROGRESS ||
                    m.getState() == GameMatch.MatchState.STARTING)      inGame  += m.getPlayerCount();
        }
        return String.format("Jugadores en cola: %d | En partida: %d | Totales: %d",
                waiting, inGame, playerMatches.size());
    }

    public String getInstances() {
        long loaded    = activeMatches.stream().filter(m -> m.getInstance() != null && m.getInstance().getMapLoaded()).count();
        long preloading = activeMatches.stream().filter(m -> m.getInstance() != null && !m.getInstance().getMapLoaded()).count();
        return String.format("Pool listo: %d | Pool en creación: %d | En use: %d | Precargando para match: %d",
                instancePool.size(), instancePool.getBeingCreated(), loaded, preloading);
    }
}