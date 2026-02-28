package Tenzinn.Deathmatch.Commands;

import Tenzinn.Countertale;
import Tenzinn.Deathmatch.GameMatch;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import Tenzinn.Listeners.MessageListeners;
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
            commandContext.sendMessage(Message.raw(String.format(MessageListeners.get(MessageListeners.MessageKey.CHAT_ALREADY_IN_GAME) + " (%d/10 players). State: %s",currentMatch.getPlayerCount(),currentMatch.getState())).color(Color.ORANGE));
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        GameMatch match = plugin.getMatchManager().addPlayerToQueue(playerRef);
        player.sendMessage(Message.raw(String.format(MessageListeners.get(MessageListeners.MessageKey.CHAT_ADDED_QUEUE) + " [%s] (%d/10 players)",match.getMatchId().toString().substring(0, 8),match.getPlayerCount())).color(Color.orange));

        plugin.showQueueHud(playerRef, player, match);
        plugin.notifyMatchPlayersAndUpdateHuds(match);

        if (match.isFull()) {
            player.sendMessage(Message.raw(MessageListeners.get(MessageListeners.MessageKey.CHAT_STARTING_GAME)).color(Color.green));

            plugin.hideAllQueueHuds(match);
            plugin.startMatch(match);
        }
    }

    @Override
    public String getPermission() { return "countertale.queue"; }

    @Override
    public String getName() { return "queue"; }
}