package Tenzinn.Core.Commands.Economy;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class RevenueCommands extends AbstractCommandCollection {

    public RevenueCommands(@NonNullDecl String name, @NonNullDecl String description) {
        super(name, description);

        addSubCommand(new SetRevenueCommand("set", "Set type revenue in one line"));
        addSubCommand(new GetRevenueCommand("get", "Get list revenues"));
        addSubCommand(new GiveRevenueCommand("give", "Give money to any player"));
    }
}