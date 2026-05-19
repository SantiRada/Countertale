package Tenzinn.Core.Commands.Cases;

import Tenzinn.Core.Cases.CaseManager;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;

public class SetCaseChanceCommand extends CommandBase {

    private final OptionalArg<Integer> valueArg;

    public SetCaseChanceCommand(@NonNullDecl String name, @NonNullDecl String description) {
        super(name, description);
        valueArg = withOptionalArg("value", "Drop chance 0-100 (e.g. --value=100 for guaranteed drop)", ArgTypes.INTEGER);
    }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        PlayerRef playerRef = commandContext.senderAs(PlayerRef.class);

        Integer raw = valueArg.get(commandContext);
        if (raw == null) {
            playerRef.sendMessage(Message.raw(
                    "[Cases] Current drop chance: " + String.format("%.0f%%", CaseManager.getDropChance() * 100)
                    + "  |  Usage: /case chance --value=<0-100>"
            ).color(Color.YELLOW));
            return;
        }

        float chance = raw / 100f;
        CaseManager.setDropChance(chance);

        playerRef.sendMessage(Message.raw(
                "[Cases] Drop chance set to " + String.format("%.0f%%", CaseManager.getDropChance() * 100)
        ).color(Color.GREEN));
    }
}
