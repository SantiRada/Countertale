package Tenzinn.Core.Commands.Economy;

import Tenzinn.Core.Objects.PlayerStats;
import Tenzinn.Core.Localization.Lang;
import Tenzinn.Core.Tools.RefactorTool;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class GiveRevenueCommand extends CommandBase {

    private final OptionalArg<String> target;
    private final OptionalArg<Integer> amount;

    public GiveRevenueCommand(@NonNullDecl String name, @NonNullDecl String description) {
        super(name, description);

        amount = withOptionalArg("amount", "Enter the amount give money to this player --amount=[INTEGER]", ArgTypes.INTEGER);
        target = withOptionalArg("player", "Enter the Username to Player target --player=Username", ArgTypes.STRING);
    }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        PlayerRef senderRef = commandContext.senderAs(PlayerRef.class);
        String username;
        if (target.get(commandContext) != null) { username = target.get(commandContext); }
        else { username = senderRef.getUsername(); }

        PlayerRef playerRef = Universe.get().getPlayerByUsername(username, NameMatching.EXACT);
        PlayerStats playerStats = RefactorTool.getPlayerStats(playerRef);
        assert playerStats != null;

        playerStats.giveMoney(amount.get(commandContext));

        senderRef.sendMessage(Lang.msg("economy.give.sent", "player", username));
        playerRef.sendMessage(Lang.msg("economy.give.received", "amount", amount.get(commandContext)));
    }
}
