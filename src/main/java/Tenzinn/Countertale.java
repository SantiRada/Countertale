package Tenzinn;

import Tenzinn.Events.*;
import Tenzinn.Handle.*;
import Tenzinn.Deathmatch.*;
import Tenzinn.Deathmatch.Commands.*;
import Tenzinn.Deathmatch.UI.QueueHud;
import Tenzinn.Admin.UI.ServerStatusHud;
import Tenzinn.Admin.Commands.AdminCommands;
import Tenzinn.Admin.Commands.ServerStatusCommand;
import Tenzinn.Deathmatch.Commands.Loot.LootCommands;
import Tenzinn.Interactions.UseActionBookInteraction;
import Tenzinn.Deathmatch.Commands.Game.GameCommands;
import Tenzinn.Admin.Commands.HideServerStatusCommand;
import Tenzinn.Deathmatch.Commands.Statue.StatueCommand;

import Tenzinn.Listeners.*;
import Tenzinn.Tools.RefactorTool;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;

import java.util.Map;
import java.util.List;
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

    private PacketFilter hotbarFilter;
    private PacketFilter detectFilter;

    // Sistema de HUD de Cola
    private final Map<String, QueueHud> activeQueueHuds = new ConcurrentHashMap<>();

    public Countertale(@Nonnull JavaPluginInit init) { super(init); }

    @Override
    protected void setup() {
        MessageListeners.load();
        MapListeners.load();

        RefactorTool.setMap();


        // Interactions
        this.getCodecRegistry(Interaction.CODEC).register("use_actionbook", UseActionBookInteraction.class, UseActionBookInteraction.CODEC);
        matchManager = new MatchManager(this);

        // Admin Commands
        getCommandRegistry().registerCommand(new ServerStatusCommand("server", "Show server status", this));
        getCommandRegistry().registerCommand(new HideServerStatusCommand("hide", "Hide server status HUD", this));
        getCommandRegistry().registerCommand(new AdminCommands("admin", "View list of commands for Countertale"));
        getCommandRegistry().registerCommand(new StatueCommand("statue", "Manage statue configurations."));

        // Deathmatch Commands
        getCommandRegistry().registerCommand(new QueueCommand("queue", "Join match queue", this));
        getCommandRegistry().registerCommand(new LeaveQueueCommand("leave", "Leave match queue", this));
        getCommandRegistry().registerCommand(new ForceStartCommand("forcestart", "Force start current match (DEBUG)", this));
        getCommandRegistry().registerCommand(new GameCommands("game", "list of command to instance manager.", this));
        getCommandRegistry().registerCommand(new BackToLobbyCommand("lobby", "Back to lobby in game", this));
        getCommandRegistry().registerCommand(new ClearHUDCommand("clearhud", "Clear HUD to change instance"));
        getCommandRegistry().registerCommand(new ShopCommand("shop", "Open Custom page of shop"));
        getCommandRegistry().registerCommand(new MvpCommand("mvp", "Open Custom page of MVP"));

        getCommandRegistry().registerCommand(new LootCommands("loot", "Control loot for this player"));

        // Starter Kit
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, DetectPlayerReady::onPlayerReady);

        // Listeners
        this.getEntityStoreRegistry().registerSystem(new ShopStatueListener());
        this.getEntityStoreRegistry().registerSystem(new QueueStatueListener());
        this.getEntityStoreRegistry().registerSystem(StatueBlockListener.getInstance());

        // Events
        this.getEntityStoreRegistry().registerSystem(new PreventItemDrop());
        this.getEntityStoreRegistry().registerSystem(new BlockPlaceSystem());
        this.getEntityStoreRegistry().registerSystem(new DetectBlockDamage());
        this.getEntityStoreRegistry().registerSystem(new DeathDetector());
        this.getEntityStoreRegistry().registerSystem(new PlayerHealthTracker());
        this.getEntityStoreRegistry().registerSystem(new InvulnerabilitySystem());

        // Handlers
        CancelHandler handler = new CancelHandler();
        detectFilter = PacketAdapters.registerInbound(handler);

        HotbarSlotHandler hotbar = new HotbarSlotHandler();
        hotbarFilter = PacketAdapters.registerInbound(hotbar);
    }

    @Override
    protected void shutdown() {
        if (detectFilter != null) { PacketAdapters.deregisterInbound(detectFilter); }
        if (hotbarFilter != null) { PacketAdapters.deregisterInbound(hotbarFilter); }
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

        matchManager.initPool();
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
        String message = String.format("Players: %d/10", playerCount);

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

        try {
            hideAllQueueHuds(match);
            match.getInstance().teleportPlayers(match.getPlayers());
        } catch (Exception e) { match.setState(GameMatch.MatchState.WAITING); }
    }
}