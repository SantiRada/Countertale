package Tenzinn;

import Tenzinn.Core.Events.*;
import Tenzinn.Core.Handle.*;
import Tenzinn.Core.GameMatch;
import Tenzinn.Core.Commands.*;
import Tenzinn.Core.Listeners.*;
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
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.world.events.AllWorldsLoadedEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        MessageListeners.load();
        RevenuesConfig.load();
        MapListeners.load();

        // Interactions
        this.getCodecRegistry(Interaction.CODEC).register(
                "use_actionbook", UseActionBookInteraction.class, UseActionBookInteraction.CODEC);

        matchManager = new MatchManager(this);

        // Admin Commands
        getCommandRegistry().registerCommand(new AdminCommands("admin", "View list of commands for Countertale"));
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
        this.getEntityStoreRegistry().registerSystem(new DeathDetector());
        this.getEntityStoreRegistry().registerSystem(new PlayerHealthTracker());
        this.getEntityStoreRegistry().registerSystem(new InvulnerabilitySystem());

        this.getEventRegistry().<String, PlayerChatEvent>registerAsyncGlobal(
                PlayerChatEvent.class,
                future -> future.thenApply(event -> {
                    TeamChatSystem.onPlayerChat(event);
                    return event;
                })
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
        if (detectFilter != null) { PacketAdapters.deregisterInbound(detectFilter); }
        if (hotbarFilter != null) { PacketAdapters.deregisterInbound(hotbarFilter); }
        matchManager.getInstancePool().shutdown();
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

    /**
     * Muestra el HUD de cola al jugador.
     * Si ya tiene un HUD activo, solo actualiza el contador de jugadores.
     *
     * @param playerRef el jugador
     * @param player    entidad del jugador
     * @param match     la partida a la que se unió
     * @param playerMaps los mapas que el jugador seleccionó (para mostrar en el HUD)
     */
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

    /**
     * Actualiza el HUD de todos los jugadores de la partida al estado "Cargando escenario...".
     * Se llama cuando la partida alcanzó 10 jugadores y está esperando que el mapa cargue.
     * El HUD no se oculta hasta que la instancia esté lista y se vaya a teletransportar.
     */
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

    // ── Arranque de partidas ──────────────────────────────────────────────────

    public MatchManager getMatchManager() { return matchManager; }

    private void checkAndStartFullMatches() {
        List<GameMatch> fullMatches = matchManager.getFullMatches();
        for (GameMatch match : fullMatches) {
            startMatch(match);
        }
    }

    /**
     * Inicia una partida completa.
     * <ol>
     *   <li>Elige el mapa más popular de los mapas elegibles (intersección de votos).</li>
     *   <li>Toma la instancia precargada del pool (o crea una en caliente si no hay).</li>
     *   <li>Teletransporta a todos los jugadores.</li>
     * </ol>
     * Si el match ya no está en estado WAITING, no hace nada (protección ante doble llamada).
     */
    public void startMatch(GameMatch match) {
        if (match.getState() != GameMatch.MatchState.WAITING) return;
        match.setState(GameMatch.MatchState.STARTING);

        try {
            // Notificar a todos los jugadores que el escenario está cargando.
            // El HUD permanece visible hasta que la instancia esté lista.
            showLoadingStateHuds(match);

            // Elegir mapa de la intersección de votos de los jugadores
            if (match.getMapId() == null) {
                List<String> eligible = new ArrayList<>(match.getEligibleMaps());
                if (eligible.isEmpty()) {
                    // Fallback defensivo: todos los mapas disponibles
                    eligible = new ArrayList<>(MapListeners.getMapNames());
                }

                String mapId = matchManager.getInstancePool()
                        .getPopularity()
                        .pickBestMap(eligible);

                if (mapId == null) mapId = eligible.get(0);
                match.setMapId(mapId);

                getLogger().at(Level.INFO).log(
                        "[Countertale] Mapa seleccionado para match: " + mapId
                        + " (candidatos: " + eligible + ")");
            }

            // Capturar la lista de jugadores antes de entrar al callback asíncrono
            final List<PlayerRef> playersSnapshot = match.getPlayers();
            final String finalMapId = match.getMapId();

            InstanceManager instance = matchManager.getInstancePool().take(finalMapId, () -> {
                // Se ejecuta cuando la instancia está lista:
                //   - inmediatamente si era precargada
                //   - diferido si fue fallback en caliente
                // Ocultar el HUD justo antes de teletransportar para que los jugadores
                // vean "Cargando escenario..." hasta el último momento.
                hideAllQueueHuds(match);
                match.getInstance().teleportPlayers(playersSnapshot);
            });
            match.setInstance(instance);

        } catch (Exception e) {
            match.setState(GameMatch.MatchState.WAITING); // revertir para reintentar
            getLogger().at(Level.SEVERE).log("[Countertale] Error al iniciar partida: " + e.getMessage());
        }
    }
}
