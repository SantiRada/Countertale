package Tenzinn.Deathmatch.Commands.Game;

import Tenzinn.Countertale;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class GameCommands extends AbstractCommandCollection {

    public GameCommands(@NonNullDecl String name, @NonNullDecl String description, Countertale main) {
        super(name, description);

        addSubCommand(new CreateGameCommand("create", "Create a new instance to game", main));
        addSubCommand(new StatusGameCommand("status", "Review status of the instances", main));
        addSubCommand(new RedirGameCommand("redir", "Redir a exist instance", main));
        addSubCommand(new DeleteGameCommand("delete", "Delete a exist instace", main));
    }

    @Override
    public String getPermission() { return "countertale.game"; }

    @Override
    public String getName() { return "game"; }
}