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

public class QueueCommand extends AbstractPlayerCommand {

    private final Countertale plugin;

    public QueueCommand(String name, String description, Countertale plugin) { super(name, description); this.plugin = plugin; }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {

        if (plugin.getMatchManager().isPlayerInMatch(playerRef)) {
            GameMatch currentMatch = plugin.getMatchManager().getPlayerMatch(playerRef);
            commandContext.sendMessage(Message.raw(String.format("Ya estás en una partida <color:orange>(%d/10 jugadores)</color>. Estado: <color:orange>%s</color>",currentMatch.getPlayerCount(),currentMatch.getState())));
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());

        if (player == null) { commandContext.sendMessage(Message.raw("<color:red>Error: No se pudo obtener el componente del jugador.</color>")); return; }

        GameMatch match = plugin.getMatchManager().addPlayerToQueue(playerRef);
        player.sendMessage(Message.raw(String.format("<color:orange>Añadido a la cola!</color> Partida %s <color:orange>(%d/10 jugadores)</color>",match.getMatchId().toString().substring(0, 8),match.getPlayerCount())));

        plugin.showQueueHud(playerRef, player, match);
        plugin.notifyMatchPlayersAndUpdateHuds(match);

        if (match.isFull()) {
            player.sendMessage(Message.raw("<color:green>¡Partida completa! Iniciando...</color>"));

            plugin.hideAllQueueHuds(match);
            plugin.startMatch(match);
        }
    }
}