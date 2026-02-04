package Tenzinn;

import Tenzinn.Admin.UI.ServerStatusHud;
import Tenzinn.Admin.Commands.ServerStatusCommand;
import Tenzinn.Admin.Commands.HideServerStatusCommand;
import Tenzinn.Deathmatch.*;
import Tenzinn.Deathmatch.Commands.*;
import Tenzinn.Deathmatch.UI.QueueHud;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;

import java.util.Map;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ConcurrentHashMap;

public class Countertale extends JavaPlugin {

    // Sistema de HUD de Admin
    private final Map<String, ServerStatusHud> activeServerHuds = new ConcurrentHashMap<>();
    private ScheduledFuture<?> serverHudUpdateTask;

    // Sistema de Deathmatch
    private MatchManager matchManager;
    private ScheduledFuture<?> matchCheckTask;

    // Sistema de HUD de Cola
    private final Map<String, QueueHud> activeQueueHuds = new ConcurrentHashMap<>();

    public Countertale(@Nonnull JavaPluginInit init) { super(init); }

    @Override
    protected void setup() {
        matchManager = new MatchManager();

        // Admin Commands
        getCommandRegistry().registerCommand(new ServerStatusCommand("server", "Show server status", this));
        getCommandRegistry().registerCommand(new HideServerStatusCommand("hide", "Hide server status HUD", this));

        // Deathmatch Commands
        getCommandRegistry().registerCommand(new QueueCommand("queue", "Join match queue", this));
        getCommandRegistry().registerCommand(new LeaveQueueCommand("leave", "Leave match queue", this));
        getCommandRegistry().registerCommand(new MatchStatusCommand("matchstatus", "View match status", this));
        getCommandRegistry().registerCommand(new ForceStartCommand("forcestart", "Force start current match (DEBUG)", this));
    }

    @Override
    protected void start() {
        serverHudUpdateTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(this::updateAllServerHuds,1,1,TimeUnit.SECONDS);

        @SuppressWarnings("unchecked")
        ScheduledFuture<Void> serverHudTask = (ScheduledFuture<Void>) serverHudUpdateTask;
        getTaskRegistry().registerTask(serverHudTask);

        matchCheckTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(this::checkAndStartFullMatches,5,5,TimeUnit.SECONDS);

        @SuppressWarnings("unchecked")
        ScheduledFuture<Void> matchTask = (ScheduledFuture<Void>) matchCheckTask;
        getTaskRegistry().registerTask(matchTask);

        getLogger().at(Level.INFO).log("Countertale plugin iniciado correctamente");
    }

    // ==================== MÉTODOS DE SERVER HUD ====================
    private void updateAllServerHuds() {
        activeServerHuds.values().forEach(ServerStatusHud::updateStats);
    }
    public void registerServerHud(String playerId, ServerStatusHud hud) { activeServerHuds.put(playerId, hud); }
    public void unregisterServerHud(String playerId) {
        activeServerHuds.get(playerId).hideStats();

        activeServerHuds.remove(playerId);
    }
    public boolean hasActiveServerHud(String playerId) { return activeServerHuds.containsKey(playerId); }

    // ==================== MÉTODOS DE QUEUE HUD ====================
    public void showQueueHud(PlayerRef playerRef, Player player, GameMatch match) {
        String playerId = playerRef.getUuid().toString();

        QueueHud existingHud = activeQueueHuds.get(playerId);
        if (existingHud != null) { existingHud.updatePlayerCount(match.getPlayerCount()); return; }

        // Crear nuevo HUD
        QueueHud queueHud = new QueueHud(playerRef);
        player.getHudManager().setCustomHud(playerRef, queueHud);

        queueHud.updatePlayerCount(match.getPlayerCount());

        activeQueueHuds.put(playerId, queueHud);
    }
    public void hideQueueHud(PlayerRef playerRef) {
        String playerId = playerRef.getUuid().toString();

        activeQueueHuds.get(playerId).hideQueueUI();
        activeQueueHuds.remove(playerId);
    }
    public void hideAllQueueHuds(GameMatch match) {
        for (PlayerRef playerRef : match.getPlayers()) {
            String playerId = playerRef.getUuid().toString();
            QueueHud queueHud = activeQueueHuds.remove(playerId);

            if (queueHud != null) queueHud.hideQueueUI();
        }
    }
    public void notifyMatchPlayersAndUpdateHuds(GameMatch match) {
        int playerCount = match.getPlayerCount();
        String message = String.format("§7[Partida] Jugadores: %d/10", playerCount);

        for (PlayerRef playerRef : match.getPlayers()) {
            playerRef.sendMessage(Message.raw(message));

            String playerId = playerRef.getUuid().toString();
            QueueHud queueHud = activeQueueHuds.get(playerId);
            if (queueHud != null) queueHud.updatePlayerCount(playerCount);
        }
    }

    // ==================== MÉTODOS DE DEATHMATCH ====================
    public MatchManager getMatchManager() {
        return matchManager;
    }
    private void checkAndStartFullMatches() {
        List<GameMatch> fullMatches = matchManager.getFullMatches();

        for (GameMatch match : fullMatches) {
            hideAllQueueHuds(match);
            startMatch(match);
        }
    }
    public void startMatch(GameMatch match) {
        if (match.getState() != GameMatch.MatchState.WAITING) return;
        match.setState(GameMatch.MatchState.STARTING);

        Map<String, World> worldsMap = Universe.get().getWorlds();
        World mainWorld = worldsMap.isEmpty() ? null : worldsMap.values().iterator().next();

        if (mainWorld == null) {
            getLogger().at(Level.SEVERE).log("No se pudo obtener el mundo principal");
            match.setState(GameMatch.MatchState.WAITING);
            return;
        }

        notifyMatchPlayers(match, "§e¡Partida iniciando! Creando arena...");

        CompletableFuture.runAsync(() -> {
            try {
                int matchOffset = match.hashCode() % 10000;
                Vector3d spawnLocation = new Vector3d(10000 + matchOffset, 100, 10000);

                match.setMatchWorld(mainWorld);
                match.setState(GameMatch.MatchState.IN_PROGRESS);

                teleportPlayersToMatch(match, spawnLocation, mainWorld);

                notifyMatchPlayers(match, "§a¡Partida iniciada! ¡Buena suerte!");

                getLogger().at(Level.INFO).log("Partida iniciada: " + match.getMatchId().toString());

            } catch (Exception e) {
                getLogger().at(Level.SEVERE).log("Error al iniciar partida: " + e.getMessage());
                match.setState(GameMatch.MatchState.WAITING);
                notifyMatchPlayers(match, "§cError al iniciar la partida. Reintentando...");
            }
        });
    }
    private void teleportPlayersToMatch(GameMatch match, Vector3d spawnLocation, World world) {
        List<PlayerRef> players = match.getPlayers();

        for (int i = 0; i < players.size(); i++) {
            PlayerRef playerRef = players.get(i);

            double angle = (2 * Math.PI / players.size()) * i;
            double radius = 10.0;

            Vector3d playerSpawn = new Vector3d(spawnLocation.x + Math.cos(angle) * radius,spawnLocation.y,spawnLocation.z + Math.sin(angle) * radius);

            teleportPlayer(playerRef, playerSpawn, world);
        }
    }
    private void teleportPlayer(PlayerRef playerRef, Vector3d position, World world) {
        world.execute(() -> {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null) return;

            Store<EntityStore> store = ref.getStore();

            Teleport teleport = Teleport.createForPlayer(world,position,new Vector3f(0, 0, 0));

            store.addComponent(ref, Teleport.getComponentType(), teleport);
        });
    }
    private void notifyMatchPlayers(GameMatch match, String message) { for (PlayerRef player : match.getPlayers()) { player.sendMessage(Message.raw(message)); } }
}