package Tenzinn.Core.Admin.Commands.Game;

import Tenzinn.OrbisOffensive;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class GameCommands extends AbstractCommandCollection {

    public GameCommands(@NonNullDecl String name, @NonNullDecl String description, OrbisOffensive main) {
        super(name, description);

        addSubCommand(new StatusGameCommand("status", "Review status of the instances", main));
    }

    @Override
    public String getPermission() { return "orbisoffensive.game"; }

    @Override
    public String getName() { return "game"; }
}