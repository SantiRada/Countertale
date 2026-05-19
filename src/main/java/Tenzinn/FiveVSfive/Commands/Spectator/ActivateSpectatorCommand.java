package Tenzinn.FiveVSfive.Commands.Spectator;

import Tenzinn.Core.Tools.RefactorTool;

import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class ActivateSpectatorCommand extends CommandBase {

    private final OptionalArg<String> target;

    public ActivateSpectatorCommand(@NonNullDecl String name, @NonNullDecl String description) {
        super(name, description);

        target = withOptionalArg("player", "Enter the Username to Player target --player=Username", ArgTypes.STRING);
    }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        PlayerRef sender = commandContext.senderAs(PlayerRef.class);
        if (sender == null) return;

        if (target.get(commandContext) != null) {
            PlayerRef playerRef = Universe.get().getPlayerByUsername(target.get(commandContext), NameMatching.EXACT);
            assert playerRef != null;
        }
    }
}