package Tenzinn.Deathmatch.Commands;

import Tenzinn.Countertale;
import Tenzinn.Deathmatch.GameMatch;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;

public class QueueCommand extends AbstractPlayerCommand {

    private final Countertale plugin;

    public QueueCommand(String name, String description, Countertale plugin) { super(name, description); this.plugin = plugin; }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {

        if (plugin.getMatchManager().isPlayerInMatch(playerRef)) {
            GameMatch currentMatch = plugin.getMatchManager().getPlayerMatch(playerRef);
            commandContext.sendMessage(Message.raw(String.format("Ya estás en una partida (%d/10 jugadores). Estado: %s",currentMatch.getPlayerCount(),currentMatch.getState())).color(Color.ORANGE));
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());

        if (player == null) { commandContext.sendMessage(Message.raw("Error: No se pudo obtener el componente del jugador.").color(Color.RED)); return; }

        GameMatch match = plugin.getMatchManager().addPlayerToQueue(playerRef, plugin);
        player.sendMessage(Message.raw(String.format("Añadido a la cola! Partida %s (%d/10 jugadores)",match.getMatchId().toString().substring(0, 8),match.getPlayerCount())).color(Color.orange));

        plugin.showQueueHud(playerRef, player, match);
        plugin.notifyMatchPlayersAndUpdateHuds(match);

        if (match.isFull()) {
            player.sendMessage(Message.raw("¡Partida completa! Iniciando...").color(Color.green));

            plugin.hideAllQueueHuds(match);
            plugin.startMatch(match);
        }
    }

    @Override
    public String getPermission() { return "countertale.queue"; }

    @Override
    public String getName() { return "queue"; }
}