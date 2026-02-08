package Tenzinn.Deathmatch.Commands.Game;

import Tenzinn.Countertale;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class DeleteGameCommand extends CommandBase {

    protected Countertale main;

    public DeleteGameCommand(@NonNullDecl String name, @NonNullDecl String description, Countertale main) { super(name, description); this.main = main; }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        Player player = commandContext.senderAs(Player.class);

        player.sendMessage(Message.raw("Eliminando instancia..."));
    }

    @Override
    public String getPermission() { return "countertale.game.delete"; }

    @Override
    public String getName() { return "delete"; }
}
