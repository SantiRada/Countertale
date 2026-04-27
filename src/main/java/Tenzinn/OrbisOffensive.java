package Tenzinn;

import com.thescar.hygunsplugin.HygunsPluginMain;
import Tenzinn.Core.Commands.Party.PartyCommands;
import Tenzinn.Core.Events.*;
import Tenzinn.Core.Handle.*;
import Tenzinn.Core.GameMatch;
import Tenzinn.Core.Commands.*;
import Tenzinn.Core.Listeners.*;
import Tenzinn.Core.PartyManager;
import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.UI.PartyHUD;
import Tenzinn.Core.UI.QueueHud;
import Tenzinn.Core.MatchManager;
import Tenzinn.Core.Instances.InstanceManager;
import Tenzinn.Core.Shop.RevenuesConfig;
import Tenzinn.Core.Commands.Loot.LootCommands;
import Tenzinn.Core.Admin.Commands.AdminCommands;
import Tenzinn.FiveVSfive.Systems.TeamChatSystem;
import Tenzinn.Core.Commands.Economy.RevenueCommands;
import Tenzinn.FiveVSfive.Commands.Wall.WallCommands;
import Tenzinn.Core.Admin.Commands.Game.GameCommands;
import Tenzinn.Core.Admin.Commands.ForceStartCommand;
import Tenzinn.Core.Admin.Commands.Statue.StatueCommand;
import Tenzinn.Core.Interactions.UseActionBookInteraction;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.HytaleServer;
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

public class OrbisOffensive extends HygunsPluginMain {

    // Match system
    private MatchManager matchManager;
    private ScheduledFuture<?> matchCheckTask;

    private PacketFilter hotbarFilter;
    private PacketFilter detectFilter;

    // Queue HUD system
    private final Map<String, QueueHud> activeQueueHuds = new ConcurrentHashMap<>();

    public OrbisOffensive(@Nonnull JavaPluginInit init) { super(init); }

    @Override
    protected void setup() {
        super.setup();

        MapListeners.load();
        RevenuesConfig.load();
        MessageListeners.load();

        // Interactions
        this.getCodecRegistry(Interaction.CODEC).register("use_actionbook", UseActionBookInteraction.class, UseActionBookInteraction.CODEC);
        matchManager = new MatchManager(this);

        // Admin Commands
        getCommandRegistry().registerCommand(new AdminCommands("admin", "View list of commands for OrbisOffensive"));
        getCommandRegistry().registerCommand(new StatueCommand("statue", "Manage statue configurations."));
        getCommandRegistry().registerCommand(new ForceStartCommand("forcestart", "Force start current match (DEBUG)", this));
        getCommandRegistry().registerCommand(new GameCommands("game", "list of command to instance manager.", this));
        getCommandRegistry().registerCommand(new ClearHUDCommand("clearhud", "Clear HUD to change instance"));
        getCommandRegistry().registerCommand(new RevenueCommands("revenue", "All content to Revenues List"));

        // Game Commands
        getCommandRegistry().registerCommand(new QueueCommand("queue", "Join match queue", this));
        getCommandRegistry().registerCommand(new LeaveQueueCommand("leave", "Leave match queue", this));
        getCommandRegistry().registerCommand(new BackToLobbyCommand("lobby", "Back to lobby in game", this));
        getCommandRegistry().registerCommand(new LootCommands("loot", "Control loot for this player"));
        getCommandRegistry().registerCommand(new ShopCommand("shop", "Open Custom page of shop"));
        getCommandRegistry().registerCommand(new PartyCommands("party", "All commands to custom groups"));

        // FVF Commands
        getCommandRegistry().registerCommand(new WallCommands("wall", "Manage TemporalWalls for maps"));

        // Events
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, DetectPlayerReady::onPlayerReady);

        // Listeners
        this.getEntityStoreRegistry().registerSystem(new ShopStatueListener());
        this.getEntityStoreRegistry().registerSystem(new QueueStatueListener());
        this.getEntityStoreRegistry().registerSystem(StatueBlockListener.getInstance());

        // Systems
        this.getEntityStoreRegistry().registerSystem(new PreventItemDrop());
        this.getEntityStoreRegistry().registerSystem(new BlockPlaceSystem());
        this.getEntityStoreRegistry().registerSystem(new DetectBlockDamage());
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
            getLogger().at(Level.INFO).log("[OrbisOffensive] Universe ready, pool initialized.");
        });
    }

    @Override
    protected void shutdown() {
        if (detectFilter != null) { PacketAdapters.deregisterInbound(detectFilter); }
        if (hotbarFilter != null) { PacketAdapters.deregisterInbound(hotbarFilter); }

        matchManager.getInstancePool().shutdown();

        super.shutdown();
    }

    @Override
    protected void start() {
        super.start();

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
        player.getHudManager().setCustomHud(playerRef, queueHud);

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
            player.getHudManager().setCustomHud(playerRef, null);

            int id = PartyManager.GetPartyIdForPlayer(playerRef);
            if (id >= 0) { player.getHudManager().setCustomHud(playerRef, new PartyHUD(playerRef, PartyManager.totalParty.get(id))); }
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

                getLogger().at(Level.INFO).log("[OrbisOffensive] Selected map for match: " + mapId + " (candidates: " + eligible + ")");
            }

            final List<PlayerRef> playersSnapshot = match.getPlayers();
            final String finalMapId = match.getMapId();
            final InstanceManager[] instanceRef = new InstanceManager[1];

            instanceRef[0] = matchManager.getInstancePool().take(finalMapId, () -> {
                match.setInstance(instanceRef[0]);

                for (PlayerRef playerRef : playersSnapshot) {
                    UUID worldUuid = playerRef.getWorldUuid();
                    if (worldUuid == null) continue;

                    World world = Universe.get().getWorld(worldUuid);
                    if (world == null) continue;

                    world.execute(() -> hideQueueHud(playerRef));
                }

                match.getInstance().teleportPlayers(playersSnapshot);
            });

        } catch (Exception e) {
            match.setState(GameMatch.MatchState.WAITING);
            getLogger().at(Level.SEVERE).log("[OrbisOffensive] Error starting match: " + e.getMessage());
        }
    }
}
