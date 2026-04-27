package Tenzinn.Core.Commands.Party;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class PartyCommands extends AbstractCommandCollection {

    public PartyCommands(@NonNullDecl String name, @NonNullDecl String description) {
        super(name, description);

        addSubCommand(new CreatePartyCommand("create", "Creates a party UI on the left of your screen showing the party leader and members."));
        addSubCommand(new InvitePartyCommand("invite", "Invites the player to the party."));
        addSubCommand(new JoinPartyCommand("join", "Joins the party the player has been invited to."));
        addSubCommand(new DeclinePartyCommand("decline", "Sends a message to the party that invited that player, that the invite was declined."));
        addSubCommand(new LeavePartyCommand("leave", "Sends a message to all players in that party that you have left, and removes you from the party."));
        addSubCommand(new OrderPartyCommand("order", "Order of leader group to initial queue."));
        addSubCommand(new ThrowPartyCommand("throw", "Throw player to party with --user=[STRING]."));
    }

    @Override
    public String getPermission() { return "OrbisOffensive.party"; }

    @Override
    public String getName() { return "party"; }
}