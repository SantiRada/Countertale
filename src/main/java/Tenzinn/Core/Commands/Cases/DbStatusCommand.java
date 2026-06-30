package Tenzinn.Core.Commands.Cases;

import Tenzinn.Core.Storage.DatabaseManager;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;

public class DbStatusCommand extends CommandBase {

    public DbStatusCommand(@NonNullDecl String name, @NonNullDecl String description) {
        super(name, description);
    }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        PlayerRef playerRef = commandContext.senderAs(PlayerRef.class);
        playerRef.sendMessage(Message.raw("[Cases] Checking SQL status...").color(Color.YELLOW));

        DatabaseManager.debugStatus(playerRef.getUuid(), playerRef.getUsername())
                .thenAccept(status -> {
                    Color color = status.startsWith("SQL OK") ? Color.GREEN : Color.RED;
                    playerRef.sendMessage(Message.raw("[Cases] " + status).color(color));
                });
    }

    @Override
    public String getPermission() { return "countertale.cases.admin"; }
}
