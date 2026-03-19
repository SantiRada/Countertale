package Tenzinn.Core.Admin.Commands.Statue;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class SetStatueCollectionCommand extends AbstractCommandCollection {

    public SetStatueCollectionCommand(String name, String description) {
        super(name, description);

        addSubCommand(new SetStatueTypeCommand("queue", "Set the queue statue block."));
        addSubCommand(new SetStatueTypeCommand("shop",  "Set the shop statue block."));
    }
}