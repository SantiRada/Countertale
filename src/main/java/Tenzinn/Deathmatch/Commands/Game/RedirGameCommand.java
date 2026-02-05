package Tenzinn.Deathmatch.Commands.Game;

import Tenzinn.Countertale;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class RedirGameCommand extends CommandBase {
    protected Countertale main;

    public RedirGameCommand(@NonNullDecl String name, @NonNullDecl String description, Countertale main) { super(name, description); this.main = main; }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        Player player = commandContext.senderAs(Player.class);

        player.sendMessage(Message.raw("Redirigiendo a instancia..."));
    }
}
