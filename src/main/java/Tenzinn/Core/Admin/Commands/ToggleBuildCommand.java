package Tenzinn.Core.Admin.Commands;

import Tenzinn.Core.Events.BlockPlaceSystem;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;

public class ToggleBuildCommand extends AbstractPlayerCommand {

    public ToggleBuildCommand(@NonNullDecl String name, @NonNullDecl String description) {
        super(name, description);
    }

    @Override
    protected void execute(@NonNullDecl CommandContext ctx,
                           @NonNullDecl Store<EntityStore> store,
                           @NonNullDecl Ref<EntityStore> ref,
                           @NonNullDecl PlayerRef playerRef,
                           @NonNullDecl World world) {
        boolean nowBlocking = !BlockPlaceSystem.isBlocking();
        BlockPlaceSystem.setBlocking(nowBlocking);

        String state = nowBlocking ? "ACTIVADO (bloquea colocación)" : "DESACTIVADO (permite colocar)";
        Color  color = nowBlocking ? Color.RED : Color.GREEN;
        playerRef.sendMessage(Message.raw("[Build] Bloqueo de bloques: " + state).color(color));
    }
}
