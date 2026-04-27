package Tenzinn.Core.Admin.Commands.Statue;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class StatueCommand extends AbstractCommandCollection {

    public StatueCommand(String name, String description) {
        super(name, description);

        addSubCommand(new SetStatueCollectionCommand("set", "Set the statue's configuration according to the model you are looking at."));
        addSubCommand(new DeleteStatuesCommand("deletes", "Delete all hologram statues from the world."));
    }

    @Override
    public String getPermission() { return "OrbisOffensive.statue"; }

    @Override
    public String getName() { return "statue"; }
}
