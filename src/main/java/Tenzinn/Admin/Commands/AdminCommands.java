package Tenzinn.Admin.Commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class AdminCommands extends CommandBase {

    public AdminCommands(@NonNullDecl String name, @NonNullDecl String description) { super(name, description); }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {

        commandContext.sendMessage(Message.raw("========== ALL COMANDS SERVER =========="));

        commandContext.sendMessage(Message.raw("/server: Show server status"));
        commandContext.sendMessage(Message.raw("/hide: Hide server status HUD"));
        commandContext.sendMessage(Message.raw("-------------------------------------"));
        commandContext.sendMessage(Message.raw("/queue: Join match queue"));
        commandContext.sendMessage(Message.raw("/leave: Leave match queue"));
        commandContext.sendMessage(Message.raw("/forcestart: Force start current match (DEBUG)"));
        commandContext.sendMessage(Message.raw("/lobby: Back to lobby in game"));
        commandContext.sendMessage(Message.raw("-------------------------------------"));
        commandContext.sendMessage(Message.raw("/game create: Create a new instance to game"));
        commandContext.sendMessage(Message.raw("/game status: Review status of the instances"));
        commandContext.sendMessage(Message.raw("/game redir: Redirection a exist instance with --code"));
        commandContext.sendMessage(Message.raw("/game delete: Delete a exist instance with --code"));

        commandContext.sendMessage(Message.raw("====================================="));
    }

    @Override
    public String getPermission() { return "countertale.admin"; }

    @Override
    public String getName() { return "admin"; }
}
