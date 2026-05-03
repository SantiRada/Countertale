package Tenzinn.Core.Commands;

import Tenzinn.Countertale;
import Tenzinn.Core.GameMatch;
import Tenzinn.Core.Instances.MapVoteStore;
import Tenzinn.Core.Listeners.MessageListeners;
import Tenzinn.Core.Localization.Lang;
import Tenzinn.Core.Objects.PartyObject;
import Tenzinn.Core.PartyManager;
import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.UI.ModesPage;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class QueueCommand extends AbstractPlayerCommand {

    private final Countertale plugin;
    private final OptionalArg<String> mode;

    public QueueCommand(String name, String description, Countertale plugin) {
        super(name, description);
        this.plugin = plugin;
        mode = withOptionalArg("mode", "Select mode for add to queue", ArgTypes.STRING);
    }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store,
                           @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef,
                           @NonNullDecl World world) {
        RefactorTool.launchSound(playerRef, "clic");
        String modeArg = mode.get(commandContext);

        if (modeArg.equalsIgnoreCase("null") || modeArg.isBlank() || modeArg.isEmpty()) {
            Player player = commandContext.senderAs(Player.class);
            player.getPageManager().openCustomPage(ref, store, new ModesPage(playerRef));
            return;
        }

        int partyIdx = PartyManager.GetPartyIdForPlayer(playerRef);
        if (partyIdx >= 0) {
            PartyObject party = PartyManager.totalParty.get(partyIdx);
            boolean isLeader = party.players.getFirst().equals(playerRef);

            if (!isLeader) {
                playerRef.sendMessage(Lang.msg("party.only-leader-queue").color(Color.orange));
                return;
            }

            for (PlayerRef member : party.players) {
                if (plugin.getMatchManager().isPlayerInMatch(member)) {
                    playerRef.sendMessage(Lang.msg("party.member-already-in-match", "player", member.getUsername())
                            .color(Color.red));
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
                member.sendMessage(MessageListeners.message(MessageListeners.MessageKey.CHAT_ADDED_QUEUE)
                        .param("match", matchShortId)
                        .param("players", match.getPlayerCount())
                        .color(Color.orange));
                plugin.showQueueHud(member, memberPlayer, match, votes);
            }

            plugin.notifyMatchPlayersAndUpdateHuds(match);

            if (match.isFull()) {
                playerRef.sendMessage(MessageListeners.message(MessageListeners.MessageKey.CHAT_STARTING_GAME)
                        .color(Color.green));
                plugin.hideAllQueueHuds(match);
                plugin.startMatch(match);
            }
            return;
        }

        if (plugin.getMatchManager().isPlayerInMatch(playerRef)) {
            GameMatch currentMatch = plugin.getMatchManager().getPlayerMatch(playerRef);
            commandContext.sendMessage(MessageListeners.message(MessageListeners.MessageKey.CHAT_ALREADY_IN_GAME)
                    .param("players", currentMatch.getPlayerCount())
                    .param("state", currentMatch.getState().name())
                    .color(Color.ORANGE));
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        List<String> votes = MapVoteStore.getVotes(playerRef);
        MapVoteStore.clearVotes(playerRef);

        GameMatch match = plugin.getMatchManager().addPlayerToQueue(playerRef, modeArg, votes);
        if (match == null) return;

        player.sendMessage(MessageListeners.message(MessageListeners.MessageKey.CHAT_ADDED_QUEUE)
                .param("match", match.getMatchId().toString().substring(0, 8))
                .param("players", match.getPlayerCount())
                .color(Color.orange));

        plugin.showQueueHud(playerRef, player, match, votes);
        plugin.notifyMatchPlayersAndUpdateHuds(match);

        if (match.isFull()) {
            player.sendMessage(MessageListeners.message(MessageListeners.MessageKey.CHAT_STARTING_GAME)
                    .color(Color.green));
            plugin.hideAllQueueHuds(match);
            plugin.startMatch(match);
        }
    }

    @Override
    public String getPermission() { return "countertale.queue"; }

    @Override
    public String getName() { return "queue"; }
}
