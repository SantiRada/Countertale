package Tenzinn;

import Tenzinn.Core.Commands.Party.PartyCommands;
import Tenzinn.Core.Events.*;
import Tenzinn.Core.Handle.*;
import Tenzinn.Core.GameMatch;
import Tenzinn.Core.Commands.*;
import Tenzinn.Core.Listeners.*;
import Tenzinn.Core.PartyManager;
import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.UI.PartyHUD;
import Tenzinn.Core.UI.GameHUD;
import Tenzinn.Core.UI.QueueHud;
import Tenzinn.Core.MatchManager;
import Tenzinn.Core.Instances.InstanceManager;
import Tenzinn.Core.Shop.RevenuesConfig;
import Tenzinn.Core.Shop.ShopData;
import Tenzinn.Core.Commands.ArmoryCommand;
import Tenzinn.Core.Commands.Cases.CaseCommands;
import Tenzinn.Core.Commands.Loot.LootCommands;
import Tenzinn.Core.Admin.Commands.ToggleBuildCommand;
import Tenzinn.Core.Admin.Commands.EndGameCommand;
import Tenzinn.Core.Listeners.ArmoryBenchListener;
import Tenzinn.Core.Listeners.ArmoryStatueListener;
import Tenzinn.Core.Listeners.ArmoryStatueUseListener;
import Tenzinn.Core.Storage.DatabaseManager;
import Tenzinn.Core.Admin.Commands.AdminCommands;
import Tenzinn.Deathmatch.Flow.MatchDeathmatch;
import Tenzinn.FiveVSfive.Systems.TeamChatSystem;
import Tenzinn.FiveVSfive.Flow.MatchFVF;
import Tenzinn.Core.Commands.Economy.RevenueCommands;
import Tenzinn.FiveVSfive.Commands.Wall.WallCommands;
import Tenzinn.FiveVSfive.Commands.Round.RoundCommands;
import Tenzinn.Core.Admin.Commands.Game.GameCommands;
import Tenzinn.Core.Admin.Commands.ForceStartCommand;
import Tenzinn.Core.Admin.Commands.Statue.StatueCommand;
import Tenzinn.Core.Interactions.UseActionBookInteraction;
import Tenzinn.Core.Interactions.UseInventoryBookInteraction;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.AllWorldsLoadedEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ConcurrentHashMap;

public class Countertale extends JavaPlugin {

    // Sistema de Partidas
    private MatchManager matchManager;
    private ScheduledFuture<?> matchCheckTask;

    private PacketFilter hotbarFilter;
    private PacketFilter detectFilter;

    // Sistema de HUD de Cola
    private final Map<String, QueueHud> activeQueueHuds = new ConcurrentHashMap<>();

    public Countertale(@Nonnull JavaPluginInit init) { super(init); }

    @Override
    protected void setup() {
        DatabaseManager.init();
        MapListeners.load();
        RevenuesConfig.load();
        MessageListeners.load();

        // Interactions
        this.getCodecRegistry(Interaction.CODEC).register("use_actionbook", UseActionBookInteraction.class, UseActionBookInteraction.CODEC);
        this.getCodecRegistry(Interaction.CODEC).register("use_inventorybook", UseInventoryBookInteraction.class, UseInventoryBookInteraction.CODEC);
        matchManager = new MatchManager(this);

        // Admin Commands
        getCommandRegistry().registerCommand(new AdminCommands("admin", "View list of commands for Countertale"));
        getCommandRegistry().registerCommand(new StatueCommand("statue", "Manage statue configurations."));
        getCommandRegistry().registerCommand(new ForceStartCommand("forcestart", "Force start current match (DEBUG)", this));
        getCommandRegistry().registerCommand(new EndGameCommand("endgame", "Set current match timer to 00:05 (DEBUG)"));
        getCommandRegistry().registerCommand(new ToggleBuildCommand("togglebuild", "Toggle block placement blocking on/off"));
        getCommandRegistry().registerCommand(new GameCommands("game", "list of command to instance manager.", this));
        getCommandRegistry().registerCommand(new ClearHUDCommand("clearhud", "Clear HUD to change instance"));
        getCommandRegistry().registerCommand(new RevenueCommands("revenue", "All content to Revenues List"));

        // Game Commands
        getCommandRegistry().registerCommand(new QueueCommand("queue", "Join match queue", this));
        getCommandRegistry().registerCommand(new LeaveQueueCommand("leave", "Leave match queue", this));
        getCommandRegistry().registerCommand(new BackToLobbyCommand("lobby", "Back to lobby in game", this));
        getCommandRegistry().registerCommand(new LootCommands("loot", "Control loot for this player"));
        getCommandRegistry().registerCommand(new CaseCommands("case", "Debug commands for the case/skin system"));
        getCommandRegistry().registerCommand(new ArmoryCommand("armory", "Open the Armory to choose skins"));
        getCommandRegistry().registerCommand(new ShopCommand("shop", "Open Custom page of shop"));
        getCommandRegistry().registerCommand(new PartyCommands("party", "All commands to custom groups"));

        // FVF Commands
        getCommandRegistry().registerCommand(new WallCommands("wall", "Manage TemporalWalls for maps"));
        getCommandRegistry().registerCommand(new RoundCommands("round", "Admin round control commands"));

        // Events
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, DetectPlayerReady::onPlayerReady);

        // Listeners
        this.getEntityStoreRegistry().registerSystem(new ShopStatueListener());
        this.getEntityStoreRegistry().registerSystem(new ArmoryStatueListener());
        this.getEntityStoreRegistry().registerSystem(new ArmoryStatueUseListener());
        this.getEntityStoreRegistry().registerSystem(new ArmoryBenchListener());
        this.getEntityStoreRegistry().registerSystem(new QueueStatueListener());
        this.getEntityStoreRegistry().registerSystem(StatueBlockListener.getInstance());

        // Systems
        this.getEntityStoreRegistry().registerSystem(new PreventItemDrop());
        this.getEntityStoreRegistry().registerSystem(new BlockPlaceSystem());
        this.getEntityStoreRegistry().registerSystem(new DetectBlockDamage());
        this.getEntityStoreRegistry().registerSystem(new DamageStatsTracker());
        this.getEntityStoreRegistry().registerSystem(new PlayerHealthTracker());
        this.getEntityStoreRegistry().registerSystem(new InvulnerabilitySystem());
        this.getEntityStoreRegistry().registerSystem(new DeathDetector());

        this.getEventRegistry().<String, PlayerChatEvent>registerAsyncGlobal(PlayerChatEvent.class, future -> future.thenApply(event -> {
                    TeamChatSystem.onPlayerChat(event); return event; })
        );

        // Handlers
        CancelHandler handler = new CancelHandler();
        detectFilter = PacketAdapters.registerInbound(handler);

        HotbarSlotHandler hotbar = new HotbarSlotHandler();
        hotbarFilter = PacketAdapters.registerInbound(hotbar);

        this.getEventRegistry().registerGlobal(AllWorldsLoadedEvent.class, event -> {
            matchManager.getInstancePool().markReady();
            getLogger().at(Level.INFO).log("[Countertale] Universe listo, pool inicializado.");
        });
    }

    @Override
    protected void shutdown() {
        DatabaseManager.close();
        if (matchCheckTask != null && !matchCheckTask.isDone()) { matchCheckTask.cancel(false); }
        if (detectFilter != null) { PacketAdapters.deregisterInbound(detectFilter); }
        if (hotbarFilter != null) { PacketAdapters.deregisterInbound(hotbarFilter); }
        matchManager.getInstancePool().shutdown();
        GameHUD.clearRuntimeState();
        QueueHud.clearRuntimeState();
        PlayerHealthTracker.clearRuntimeState();
        MatchDeathmatch.clearRuntimeState();
        MatchFVF.clearRuntimeState();
        PartyManager.clearRuntimeState();
        ShopData.clearRuntimeState();
        RefactorTool.clearRuntimeState();
    }

    @Override
    protected void start() {
        matchCheckTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(
                this::checkAndStartFullMatches, 5, 5, TimeUnit.SECONDS);

        @SuppressWarnings("unchecked")
        ScheduledFuture<Void> matchTask = (ScheduledFuture<Void>) matchCheckTask;
        getTaskRegistry().registerTask(matchTask);
    }
    // ── Queue HUD ─────────────────────────────────────────────────────────────
    public void showQueueHud(PlayerRef playerRef, Player player, GameMatch match, List<String> playerMaps) {
        String playerId = playerRef.getUuid().toString();

        QueueHud existingHud = activeQueueHuds.get(playerId);
        if (existingHud != null) {
            existingHud.updatePlayerCount(match.getPlayerCount());
            return;
        }

        QueueHud queueHud = new QueueHud(playerRef);
        player.getHudManager().addCustomHud(playerRef, queueHud);

        queueHud.updatePlayerCount(match.getPlayerCount());
        queueHud.setMapsInfo(playerMaps);

        int partyIndex = PartyManager.GetPartyIdForPlayer(playerRef);
        if (partyIndex >= 0) { queueHud.setDataParty(PartyManager.totalParty.get(partyIndex)); }

        activeQueueHuds.put(playerId, queueHud);
    }
    public void hideQueueHud(PlayerRef playerRef) {
        String playerId = playerRef.getUuid().toString();
        QueueHud hud = activeQueueHuds.get(playerId);
        if (hud != null) {
            hud.stopUpdating();

            Player player = RefactorTool.getPlayer(playerRef);
            player.getHudManager().removeCustomHud(playerRef, playerId);

            int id = PartyManager.GetPartyIdForPlayer(playerRef);
            if (id >= 0) { player.getHudManager().addCustomHud(playerRef, new PartyHUD(playerRef, PartyManager.totalParty.get(id))); }
        }
        activeQueueHuds.remove(playerId);
    }
    public void hideAllQueueHuds(GameMatch match) {
        for (PlayerRef playerRef : match.getPlayers()) { hideQueueHud(playerRef); }
    }
    public void showLoadingStateHuds(GameMatch match) {
        for (PlayerRef playerRef : match.getPlayers()) {
            String playerId = playerRef.getUuid().toString();
            QueueHud hud = activeQueueHuds.get(playerId);
            if (hud != null) hud.showLoadingMap();
        }
    }
    public void notifyMatchPlayersAndUpdateHuds(GameMatch match) {
        int playerCount = match.getPlayerCount();
        String message  = String.format("Players: %d/10", playerCount);

        for (PlayerRef playerRef : match.getPlayers()) {
            playerRef.sendMessage(Message.raw(message));

            String playerId = playerRef.getUuid().toString();
            QueueHud queueHud = activeQueueHuds.get(playerId);
            if (queueHud != null) queueHud.updatePlayerCount(playerCount);
        }
    }
    public MatchManager getMatchManager() { return matchManager; }
    private void checkAndStartFullMatches() {
        List<GameMatch> fullMatches = matchManager.getFullMatches();
        for (GameMatch match : fullMatches) {
            startMatch(match);
        }
    }
    public void startMatch(GameMatch match) {
        if (match.getState() != GameMatch.MatchState.WAITING) return;
        match.setState(GameMatch.MatchState.STARTING);

        try {
            showLoadingStateHuds(match);

            if (match.getMapId() == null) {
                List<String> eligible = new ArrayList<>(match.getEligibleMaps()).stream().map(String::toLowerCase).collect(java.util.stream.Collectors.toList());
                if (eligible.isEmpty()) { eligible = new ArrayList<>(MapListeners.getMapNames()); }

                String mapId = matchManager.getInstancePool().getPopularity().pickBestMap(eligible);
                if (mapId == null) mapId = eligible.getFirst();
                match.setMapId(mapId);

                getLogger().at(Level.INFO).log("[Countertale] Mapa seleccionado para match: " + mapId + " (candidatos: " + eligible + ")");
            }

            final List<PlayerRef> playersSnapshot = match.getPlayers();
            final String finalMapId = match.getMapId();

            matchManager.getInstancePool().take(finalMapId, instance -> {
                try {
                    if (instance == null) {
                        match.setState(GameMatch.MatchState.WAITING);
                        getLogger().at(Level.SEVERE).log("[Countertale] Error al iniciar partida: instancia nula para mapa " + finalMapId);
                        return;
                    }

                    match.setInstance(instance);

                    for (PlayerRef playerRef : playersSnapshot) {
                        UUID worldUuid = playerRef.getWorldUuid();
                        if (worldUuid == null) continue;

                        World world = Universe.get().getWorld(worldUuid);
                        if (world == null) continue;

                        world.execute(() -> hideQueueHud(playerRef));
                    }

                    instance.teleportPlayers(playersSnapshot);
                } catch (Exception callbackError) {
                    match.setState(GameMatch.MatchState.WAITING);
                    getLogger().at(Level.SEVERE).log("[Countertale] Error al preparar instancia de partida: " + callbackError.getMessage());
                }
            });

        } catch (Exception e) {
            match.setState(GameMatch.MatchState.WAITING);
            getLogger().at(Level.SEVERE).log("[Countertale] Error al iniciar partida: " + e.getMessage());
        }
    }
}
