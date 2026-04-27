package Tenzinn.Core.Commands.Party;

import Tenzinn.Core.PartyManager;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;

public class InvitePartyCommand extends CommandBase {

    private final OptionalArg<String> username;

    public InvitePartyCommand(@NonNullDecl String name, @NonNullDecl String description) {
        super(name, description);

        username = withOptionalArg("user", "Enter the player username to send invite the party --user=[STRING]", ArgTypes.STRING);
    }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        Player player = commandContext.senderAs(Player.class);
        int id = PartyManager.GetPartyIdForPlayer(Universe.get().getPlayerByUsername(player.getDisplayName(),NameMatching.EXACT));
        if (id < 0) return;

        PlayerRef playerRef = Universe.get().getPlayerByUsername(username.get(commandContext), NameMatching.EXACT);
        if(playerRef == null) {
            player.sendMessage(Message.raw("User '" + username.get(commandContext) + "' was not found").color(Color.orange));
            return;
        }

        PartyManager.InviteToParty(id, playerRef);
    }

    @Override
    public String getPermission() { return "OrbisOffensive.party.invite"; }

    @Override
    public String getName() { return "party.invite"; }
}
