package Tenzinn.Core.Commands.Party;

import Tenzinn.Core.PartyManager;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.entity.entities.Player;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;

public class LeavePartyCommand extends CommandBase {

    public LeavePartyCommand(@NonNullDecl String name, @NonNullDecl String description) { super(name, description);}

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        Player player = commandContext.senderAs(Player.class);
        PlayerRef playerRef = Universe.get().getPlayerByUsername(player.getDisplayName(), NameMatching.EXACT);
        if (playerRef == null) return;

        PartyManager.LeaveParty(playerRef);
    }
}