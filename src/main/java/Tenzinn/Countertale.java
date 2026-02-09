package Tenzinn;

import Tenzinn.Deathmatch.*;
import Tenzinn.Deathmatch.Commands.*;
import Tenzinn.Deathmatch.UI.QueueHud;
import Tenzinn.Events.PreventItemDrop;
import Tenzinn.Admin.UI.ServerStatusHud;
import Tenzinn.Admin.Commands.AdminCommands;
import Tenzinn.Admin.Commands.ServerStatusCommand;
import Tenzinn.Interactions.UseActionBookInteraction;
import Tenzinn.Deathmatch.Commands.Game.GameCommands;
import Tenzinn.Admin.Commands.HideServerStatusCommand;

import com.hypixel.hytale.component.Ref;
import Tenzinn.Events.DetectPlayerReady;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;

import java.util.Map;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
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
        // Interactions
        this.getCodecRegistry(Interaction.CODEC).register("use_actionbook", UseActionBookInteraction.class, UseActionBookInteraction.CODEC);

        matchManager = new MatchManager(this);

        // Admin Commands
        getCommandRegistry().registerCommand(new ServerStatusCommand("server", "Show server status", this));
        getCommandRegistry().registerCommand(new HideServerStatusCommand("hide", "Hide server status HUD", this));
        getCommandRegistry().registerCommand(new AdminCommands("admin", "View list of commands for Countertale"));

        // Deathmatch Commands
        getCommandRegistry().registerCommand(new QueueCommand("queue", "Join match queue", this));
        getCommandRegistry().registerCommand(new LeaveQueueCommand("leave", "Leave match queue", this));
        getCommandRegistry().registerCommand(new ForceStartCommand("forcestart", "Force start current match (DEBUG)", this));
        getCommandRegistry().registerCommand(new GameCommands("game", "list of command to instance manager.", this));
        getCommandRegistry().registerCommand(new BackToLobbyCommand("lobby", "Back to lobby in game", this));

        // Starter Kit
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, DetectPlayerReady::onPlayerReady);

        // Events
        this.getEntityStoreRegistry().registerSystem(new PreventItemDrop());
    }

    @Override
    protected void start() {
        serverHudUpdateTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(this::updateAllServerHuds, 1, 1, TimeUnit.SECONDS);

        @SuppressWarnings("unchecked")
        ScheduledFuture<Void> serverHudTask = (ScheduledFuture<Void>) serverHudUpdateTask;
        getTaskRegistry().registerTask(serverHudTask);

        matchCheckTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(this::checkAndStartFullMatches, 5, 5, TimeUnit.SECONDS);

        @SuppressWarnings("unchecked")
        ScheduledFuture<Void> matchTask = (ScheduledFuture<Void>) matchCheckTask;
        getTaskRegistry().registerTask(matchTask);

        getLogger().at(Level.INFO).log("Countertale plugin iniciado correctamente");
    }

    // ==================== MÉTODOS DE SERVER HUD ====================
    private void updateAllServerHuds() { activeServerHuds.values().forEach(ServerStatusHud::updateStats); }
    public void registerServerHud(String playerId, ServerStatusHud hud) { activeServerHuds.put(playerId, hud); }
    public void unregisterServerHud(String playerId) {
        ServerStatusHud hud = activeServerHuds.get(playerId);
        if (hud != null) { hud.hideStats(); }
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

        QueueHud hud = activeQueueHuds.get(playerId);
        if (hud != null) { hud.hideQueueUI(); }
        activeQueueHuds.remove(playerId);
    }
    public void hideAllQueueHuds(GameMatch match) {
        for (PlayerRef playerRef : match.getPlayers()) { hideQueueHud(playerRef); }
    }
    public void notifyMatchPlayersAndUpdateHuds(GameMatch match) {
        int playerCount = match.getPlayerCount();
        String message = String.format("[Partida] Jugadores: %d/10", playerCount);

        for (PlayerRef playerRef : match.getPlayers()) {
            playerRef.sendMessage(Message.raw(message));

            String playerId = playerRef.getUuid().toString();
            QueueHud queueHud = activeQueueHuds.get(playerId);
            if (queueHud != null) queueHud.updatePlayerCount(playerCount);
        }
    }

    // ==================== MÉTODOS DE DEATHMATCH ====================
    public MatchManager getMatchManager() { return matchManager; }
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

        notifyMatchPlayers(match, "¡Partida iniciando! Creando arena...", "yellow");

        try {
            hideAllQueueHuds(match);
            match.getInstance().teleportPlayers(match.getPlayers());
        } catch (Exception e) {
            getLogger().at(Level.SEVERE).log("=== ERROR CRÍTICO ===", e);
            match.setState(GameMatch.MatchState.WAITING);
            notifyMatchPlayers(match, "Error crítico. Contacta a un admin.", "red");
        }
    }
    private void notifyMatchPlayers(GameMatch match, String message, String color) {
        for (PlayerRef player : match.getPlayers()) {
            if (color != null && !color.isEmpty()) { player.sendMessage(Message.raw(message).color(color)); }
            else { player.sendMessage(Message.raw(message)); }
        }
    }
}