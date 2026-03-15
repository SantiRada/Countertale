package Tenzinn.Deathmatch.Content.Commands.Statue;

import Tenzinn.Core.Listeners.StatueBlockListener;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.UUID;

public class SetStatueTypeCommand extends AbstractPlayerCommand {

    private final String statueType;

    public SetStatueTypeCommand(@NonNullDecl String name, @NonNullDecl String description) {
        super(name, description);
        this.statueType = name;
    }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext,
                           @NonNullDecl Store<EntityStore> store,
                           @NonNullDecl Ref<EntityStore> ref,
                           @NonNullDecl PlayerRef playerRef,
                           @NonNullDecl World world) {

        UUID playerId = playerRef.getUuid();
        StatueBlockListener.getInstance().activateFor(playerId, playerRef, world, statueType);
        playerRef.sendMessage(
                Message.raw("Golpeá un bloque para asignarlo como [" + statueType.toUpperCase() + "].").color(Color.cyan)
        );
    }
}