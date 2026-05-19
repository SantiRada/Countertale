package Tenzinn.Core.Commands.Party;

import Tenzinn.Core.PartyManager;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;

public class CreatePartyCommand extends CommandBase {

    public CreatePartyCommand(@NonNullDecl String name, @NonNullDecl String description) { super(name, description); }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        PlayerRef playerRef = commandContext.senderAs(PlayerRef.class);

        PartyManager.CreateParty(playerRef);
    }
}