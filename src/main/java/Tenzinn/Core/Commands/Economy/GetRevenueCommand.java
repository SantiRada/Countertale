package Tenzinn.Core.Commands.Economy;

import Tenzinn.Core.Shop.RevenuesConfig;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import com.hypixel.hytale.server.core.entity.entities.Player;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.ArrayList;

public class GetRevenueCommand extends CommandBase {


    public GetRevenueCommand(@NonNullDecl String name, @NonNullDecl String description) { super(name, description); }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        Player player = commandContext.senderAs(Player.class);

        player.sendMessage(Message.raw("----------------------------"));
        player.sendMessage(Message.raw("Revenues list"));

        ArrayList<String> allData = RevenuesConfig.getList();

        for (int i = 0; i < allData.size(); i++) { player.sendMessage(Message.raw(allData.get(i))); }

        player.sendMessage(Message.raw("----------------------------"));
    }
}