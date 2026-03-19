package Tenzinn.Core.Commands.Loot;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class LootCommands extends AbstractCommandCollection {

    public LootCommands(@NonNullDecl String name, @NonNullDecl String description) {
        super(name, description);

        addSubCommand(new GetLootCommand("get", "Get messages with loot for this player"));
        addSubCommand(new GiveLootCommand("give", "Give selected loot for this player"));
        addSubCommand(new ResetLootCommand("reset", "Give starter kit for this player"));
    }

    @Override
    public String getPermission() { return "countertale.loot"; }

    @Override
    public String getName() { return "loot"; }
}