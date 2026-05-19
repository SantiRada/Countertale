package Tenzinn.FiveVSfive.Commands.Wall;

import Tenzinn.Core.Listeners.MapListeners;
import Tenzinn.Core.Localization.Lang;
import Tenzinn.FiveVSfive.Systems.TemporalWallSystem;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class BuildWallCommand extends AbstractAsyncCommand {

    private final OptionalArg<String> mapArg;

    public BuildWallCommand(@NonNullDecl String name, @NonNullDecl String description) {
        super(name, description);

        mapArg = withOptionalArg("map", "Map name (uses active session map if omitted)", ArgTypes.STRING);
    }

    @NullableDecl @Override
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
        PlayerRef playerRef = context.senderAs(PlayerRef.class);

        if (!MapListeners.exists(mapArg.get(context))) {
            playerRef.sendMessage(Lang.msg("wall.map-not-found").color(Color.cyan));
            return null;
        }

        TemporalWallSystem.buildWalls(mapArg.get(context), playerRef);
        playerRef.sendMessage(Lang.msg("wall.building", "map", mapArg.get(context)));

        return CompletableFuture.completedFuture(null);
    }
}
