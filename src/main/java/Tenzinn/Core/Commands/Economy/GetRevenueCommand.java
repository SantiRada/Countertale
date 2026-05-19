package Tenzinn.Core.Commands.Economy;

import Tenzinn.Core.Shop.RevenuesConfig;
import Tenzinn.Core.Localization.Lang;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.ArrayList;

public class GetRevenueCommand extends CommandBase {


    public GetRevenueCommand(@NonNullDecl String name, @NonNullDecl String description) { super(name, description); }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        PlayerRef playerRef = commandContext.senderAs(PlayerRef.class);

        playerRef.sendMessage(Message.raw("----------------------------"));
        playerRef.sendMessage(Lang.msg("economy.revenue.list-title"));

        ArrayList<String> allData = RevenuesConfig.getList();

        for (int i = 0; i < allData.size(); i++) { playerRef.sendMessage(Message.raw(allData.get(i))); }

        playerRef.sendMessage(Message.raw("----------------------------"));
    }
}
