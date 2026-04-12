package Tenzinn.FiveVSfive.Commands.Wall;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class WallCommands extends AbstractCommandCollection {

    public WallCommands(@NonNullDecl String name, @NonNullDecl String description) {
        super(name, description);

        addSubCommand(new SetWallCommand("set", "Add a new TemporalWall to a map"));
        addSubCommand(new GetWallCommand("get", "List all TemporalWalls of a map"));
        addSubCommand(new BuildWallCommand("build", "Build TemporalWalls of a map"));
        addSubCommand(new RemoveWallCommand("remove", "Remove an existing TemporalWall by index"));
    }
}