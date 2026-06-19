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

public class DropCaseCommand extends CommandBase {

    private final OptionalArg<Integer> amountArg;

    public DropCaseCommand(@NonNullDecl String name, @NonNullDecl String description) {
        super(name, description);
        amountArg = withOptionalArg("amount", "Number of cases to add (default 1, max 100).", ArgTypes.INTEGER);
    }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        PlayerRef playerRef = commandContext.senderAs(PlayerRef.class);

        Integer raw = amountArg.get(commandContext);
        int amount = (raw != null) ? Math.max(1, Math.min(raw, 100)) : 1;

        for (int i = 0; i < amount; i++) {
            CaseManager.addCase(playerRef.getUuid());
        }

        int total = CaseManager.getCaseCount(playerRef.getUuid());
        playerRef.sendMessage(Message.raw(
                "[Cases] Added " + amount + " case" + (amount == 1 ? "" : "s") + ". Total: " + total
        ).color(Color.GREEN));
    }

    @Override
    public String getPermission() { return "countertale.cases.admin"; }
}
