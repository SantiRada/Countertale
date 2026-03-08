package Tenzinn.Admin.Commands;

import Tenzinn.Deathmatch.Commands.*;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class AdminCommands extends CommandBase {

    public AdminCommands(@NonNullDecl String name, @NonNullDecl String description) { super(name, description); }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {

        commandContext.sendMessage(Message.raw("========== ALL COMANDS SERVER =========="));
        commandContext.sendMessage(Message.raw("/admin: View list of commands for Countertale"));
        commandContext.sendMessage(Message.raw("/forcestart: Force start current match (DEBUG)"));
        commandContext.sendMessage(Message.raw("/game status: Review status of the instances"));
        commandContext.sendMessage(Message.raw("/mvp: Open Custom page of Endgame (MVP)"));
        commandContext.sendMessage(Message.raw("====================================="));
        commandContext.sendMessage(Message.raw("/loot get: Get messages with loot for this player"));
        commandContext.sendMessage(Message.raw("/loot give: Give selected loot for this player"));
        commandContext.sendMessage(Message.raw("/loot reset: Give starter kit for this player"));
        commandContext.sendMessage(Message.raw("====================================="));
        commandContext.sendMessage(Message.raw("/statue set: Set the statue's configuration according to the model you are looking at"));
        commandContext.sendMessage(Message.raw("/statue deletes: Delete all hologram statues from the world"));
        commandContext.sendMessage(Message.raw("========= AVAILABLE TO USERS ==========="));
        commandContext.sendMessage(Message.raw("/queue: Join match queue"));
        commandContext.sendMessage(Message.raw("/leave: Leave match queue"));
        commandContext.sendMessage(Message.raw("/lobby: Back to lobby in game"));
        commandContext.sendMessage(Message.raw("/shop: Open Custom page of shop"));
        commandContext.sendMessage(Message.raw("====================================="));
    }

    @Override
    public String getPermission() { return "countertale.admin"; }

    @Override
    public String getName() { return "admin"; }
}
