package Tenzinn.Core.Commands;

import Tenzinn.OrbisOffensive;
import Tenzinn.Core.GameMatch;
import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.Listeners.MessageListeners;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;

public class LeaveQueueCommand extends AbstractPlayerCommand {

    private final OrbisOffensive plugin;

    public LeaveQueueCommand(String name, String description, OrbisOffensive plugin) { super(name, description); this.plugin = plugin; }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {

        RefactorTool.launchSound(playerRef, "fail");

        GameMatch match = plugin.getMatchManager().getPlayerMatch(playerRef);

        if (match == null) { commandContext.sendMessage(Message.raw(MessageListeners.get(MessageListeners.MessageKey.CHAT_COMMAND_LEAVE_NO_QUEUE)).color(Color.red)); }
        else {
            if (match.getState() != GameMatch.MatchState.WAITING) {
                commandContext.sendMessage(Message.raw(MessageListeners.get(MessageListeners.MessageKey.CHAT_COMMAND_LEAVE_IN_GAME)).color(Color.PINK)); return;
            }

            plugin.hideQueueHud(playerRef);
            boolean removed = plugin.getMatchManager().removePlayerFromMatch(playerRef);

            if (removed) {
                commandContext.sendMessage(Message.raw(MessageListeners.get(MessageListeners.MessageKey.CHAT_LEAVE_QUEUE)).color(Color.ORANGE));

                if (!match.isEmpty()) plugin.notifyMatchPlayersAndUpdateHuds(match);
            }
        }
    }

    @Override
    public String getPermission() { return "OrbisOffensive.leave"; }

    @Override
    public String getName() { return "leave"; }
}