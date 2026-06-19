package Tenzinn.Core.Commands.Cases;

import Tenzinn.Core.Cases.CaseManager;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;
import java.util.List;

public class ClearCasesCommand extends CommandBase {

    public ClearCasesCommand(@NonNullDecl String name, @NonNullDecl String description) { super(name, description); }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        PlayerRef playerRef = commandContext.senderAs(PlayerRef.class);

        List<?> inventory = CaseManager.getInventory(playerRef.getUuid());
        int count = inventory.size();

        CaseManager.clearInventory(playerRef.getUuid());

        playerRef.sendMessage(Message.raw("[Cases] Inventory cleared (" + count + " item(s) removed).").color(Color.GREEN));
    }

    @Override
    public String getPermission() { return "countertale.cases.admin"; }
}
