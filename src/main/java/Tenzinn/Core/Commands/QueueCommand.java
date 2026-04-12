package Tenzinn.Core.Commands;

import Tenzinn.Countertale;
import Tenzinn.Core.UI.ModesPage;
import Tenzinn.Core.GameMatch;
import Tenzinn.Core.Listeners.MessageListeners;
import Tenzinn.Core.Tools.RefactorTool;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;

public class QueueCommand extends AbstractPlayerCommand {

    private final Countertale plugin;

    private final OptionalArg<String> mode;

    public QueueCommand(String name, String description, Countertale plugin) {
        super(name, description);
        this.plugin = plugin;

        mode = withOptionalArg("mode", "Select mode for add to queue", ArgTypes.STRING);
    }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {
        RefactorTool.launchSound(playerRef, "clic");

        if(mode.get(commandContext).equalsIgnoreCase("null") || mode.get(commandContext).isBlank() || mode.get(commandContext).isEmpty()) {
            Player player = commandContext.senderAs(Player.class);
            player.getPageManager().openCustomPage(ref, store, new ModesPage(playerRef));
            return;
        }

        if (plugin.getMatchManager().isPlayerInMatch(playerRef)) {
            GameMatch currentMatch = plugin.getMatchManager().getPlayerMatch(playerRef);
            commandContext.sendMessage(Message.raw(String.format(MessageListeners.get(MessageListeners.MessageKey.CHAT_ALREADY_IN_GAME) + " (%d/10 players). State: %s",currentMatch.getPlayerCount(),currentMatch.getState())).color(Color.ORANGE));
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        GameMatch match = plugin.getMatchManager().addPlayerToQueue(playerRef, mode.get(commandContext));
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