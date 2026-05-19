package Tenzinn.FiveVSfive.Commands.Wall;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;

import Tenzinn.Core.Listeners.MapListeners;
import Tenzinn.Core.Listeners.MapListeners.TemporalWall;
import Tenzinn.Core.Localization.Lang;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public final class GetWallCommand extends AbstractAsyncCommand {

    private final OptionalArg<String> mapArg;

    public GetWallCommand(String name, String description) {
        super(name, description);

        mapArg = withOptionalArg("map", "Map name (uses active session map if omitted)", ArgTypes.STRING);
    }

    @Override @Nullable
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
        PlayerRef player = context.senderAs(PlayerRef.class);

        String mapName = mapArg.provided(context) ? mapArg.get(context) : WallSessionState.currentMap();

        if (mapName == null || mapName.isBlank()) {
            player.sendMessage(Lang.msg("wall.map-required"));
            return CompletableFuture.completedFuture(null);
        }

        if (!MapListeners.exists(mapName)) {
            player.sendMessage(Lang.msg("wall.map-not-found-named", "map", mapName));
            return CompletableFuture.completedFuture(null);
        }

        List<TemporalWall> walls = MapListeners.getWalls(mapName);

        if (walls.isEmpty()) {
            player.sendMessage(Lang.msg("wall.none-defined", "map", mapName));
            return CompletableFuture.completedFuture(null);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[Wall:get] TemporalWalls for '").append(mapName)
                .append("' (").append(walls.size()).append("):");

        for (int i = 0; i < walls.size(); i++) {
            TemporalWall w = walls.get(i);
            sb.append(String.format("\n [%d] from=(%.0f, %.0f, %.0f)  to=(%.0f, %.0f, %.0f)",
                    i, w.fromX, w.fromY, w.fromZ, w.toX, w.toY, w.toZ));
        }

        player.sendMessage(Message.raw(sb.toString()));
        return CompletableFuture.completedFuture(null);
    }
}
