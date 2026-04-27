package Tenzinn.Core.Commands;

import Tenzinn.Core.Objects.PartyObject;
import Tenzinn.Core.PartyManager;
import Tenzinn.OrbisOffensive;
import Tenzinn.Core.UI.ModesPage;
import Tenzinn.Core.GameMatch;
import Tenzinn.Core.Instances.MapVoteStore;
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
import java.util.ArrayList;
import java.util.List;

public class QueueCommand extends AbstractPlayerCommand {

    private final OrbisOffensive plugin;
    private final OptionalArg<String> mode;

    public QueueCommand(String name, String description, OrbisOffensive plugin) {
        super(name, description);
        this.plugin = plugin;
        mode = withOptionalArg("mode", "Select mode for add to queue", ArgTypes.STRING);
    }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref,
                           @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {
        RefactorTool.launchSound(playerRef, "clic");
        String modeArg = mode.get(commandContext);

        // No mode -> open the mode/map selector (ModesPage)
        if (modeArg.equalsIgnoreCase("null") || modeArg.isBlank() || modeArg.isEmpty()) {
            Player player = commandContext.senderAs(Player.class);
            player.getPageManager().openCustomPage(ref, store, new ModesPage(playerRef));
            return;
        }

        if (plugin.getMatchManager().isPlayerInMatch(playerRef)) {
            GameMatch currentMatch = plugin.getMatchManager().getPlayerMatch(playerRef);
            commandContext.sendMessage(Message.raw(String.format(
                    MessageListeners.get(MessageListeners.MessageKey.CHAT_ALREADY_IN_GAME) + " (%d/10 players). State: %s",
                    currentMatch.getPlayerCount(), currentMatch.getState())).color(Color.ORANGE));
            return;
        }

        // Party flow
        int partyIdx = PartyManager.GetPartyIdForPlayer(playerRef);
        if (partyIdx >= 0) {
            PartyObject party = PartyManager.totalParty.get(partyIdx);
            boolean isLeader = party.players.getFirst().equals(playerRef);

            if (!isLeader) {
                playerRef.sendMessage(Message.raw("Only the party leader can join the queue. Use /party order to request it.").color(Color.orange));
                return;
            }

            // Verify that no member is already in the queue or game
            for (PlayerRef member : party.players) {
                if (plugin.getMatchManager().isPlayerInMatch(member)) {
                    playerRef.sendMessage(Message.raw(member.getUsername() + " is already in the queue or in a match. The group cannot join.").color(Color.red));
                    return;
                }
            }

            List<PlayerRef> partyMembers = new ArrayList<>(party.players);
            List<String> votes = MapVoteStore.getVotes(playerRef);
            MapVoteStore.clearVotes(playerRef);

            GameMatch match = plugin.getMatchManager().addGroupToQueue(partyMembers, modeArg, votes);
            if (match == null) return;

            String matchShortId = match.getMatchId().toString().substring(0, 8);
            for (PlayerRef member : partyMembers) {
                Player memberPlayer = RefactorTool.getPlayer(member);
                if (memberPlayer == null) continue;
                member.sendMessage(Message.raw(String.format(
                                MessageListeners.get(MessageListeners.MessageKey.CHAT_ADDED_QUEUE)
                                        + " [%s] (%d/10 players)", matchShortId, match.getPlayerCount()))
                        .color(Color.orange));
                plugin.showQueueHud(member, memberPlayer, match, votes);
            }

            plugin.notifyMatchPlayersAndUpdateHuds(match);

            if (match.isFull()) {
                playerRef.sendMessage(Message.raw(
                        MessageListeners.get(MessageListeners.MessageKey.CHAT_STARTING_GAME)).color(Color.green));
                plugin.hideAllQueueHuds(match);
                plugin.startMatch(match);
            }
            return;
        }

        // Single Flow

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        // Read votes saved by ModesPage (or all maps when accessed directly)
        List<String> votes = MapVoteStore.getVotes(playerRef);
        MapVoteStore.clearVotes(playerRef);

        GameMatch match = plugin.getMatchManager().addPlayerToQueue(playerRef, modeArg, votes);
        if (match == null) return;

        player.sendMessage(Message.raw(String.format(MessageListeners.get(MessageListeners.MessageKey.CHAT_ADDED_QUEUE)
                + " [%s] (%d/10 players)", match.getMatchId().toString().substring(0, 8), match.getPlayerCount())).color(Color.orange));

        // Show QueueHud with the player's selected maps
        plugin.showQueueHud(playerRef, player, match, votes);
        plugin.notifyMatchPlayersAndUpdateHuds(match);

        if (match.isFull()) {
            player.sendMessage(Message.raw(
                    MessageListeners.get(MessageListeners.MessageKey.CHAT_STARTING_GAME)).color(Color.green));
            plugin.hideAllQueueHuds(match);
            plugin.startMatch(match);
        }
    }

    @Override
    public String getPermission() { return "orbisoffensive.queue"; }

    @Override
    public String getName() { return "queue"; }
}
