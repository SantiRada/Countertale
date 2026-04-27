package Tenzinn.FiveVSfive.Commands.Wall;

import Tenzinn.Core.Listeners.MapListeners;
import Tenzinn.FiveVSfive.Systems.TemporalWallSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.Universe;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class BuildWallCommand extends AbstractAsyncCommand {

    private final OptionalArg<String> mapArg;

    public BuildWallCommand(@NonNullDecl String name, @NonNullDecl String description) {
        super(name, description);

        mapArg   = withOptionalArg("map",   "Map name (uses active session map if omitted)", ArgTypes.STRING);
    }

    @NullableDecl @Override
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
        Player player = context.senderAs(Player.class);

        if (!MapListeners.exists(mapArg.get(context))) {
            player.sendMessage(Message.raw("Selected map was not found").color(Color.cyan));
            return null;
        }

        TemporalWallSystem.buildWalls(mapArg.get(context), Objects.requireNonNull(Universe.get().getPlayerByUsername(player.getDisplayName(), NameMatching.EXACT)));
        player.sendMessage(Message.raw("Building TemporalWalls to " + mapArg.get(context)));

        return CompletableFuture.completedFuture(null);
    }
}
