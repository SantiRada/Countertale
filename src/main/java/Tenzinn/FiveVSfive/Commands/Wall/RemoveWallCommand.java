package Tenzinn.FiveVSfive.Commands.Wall;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.Message;

import Tenzinn.Core.Listeners.MapListeners;
import Tenzinn.Core.Listeners.MapListeners.TemporalWall;
import com.hypixel.hytale.server.core.entity.entities.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class RemoveWallCommand extends AbstractAsyncCommand {

    private final OptionalArg<String> mapArg;
    private final OptionalArg<String> idArg;

    public RemoveWallCommand(String name, String description) {
        super(name, description);

        idArg    = withOptionalArg("id", "Wall index (from /wall get)", ArgTypes.STRING);
        mapArg   = withOptionalArg("map", "Map name", ArgTypes.STRING);
    }

    @Override @Nullable
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
        Player player = context.senderAs(Player.class);

        int id = Integer.parseInt(idArg.get(context));
        String mapName = mapArg.get(context);

        List<TemporalWall> walls = MapListeners.getWalls(mapName);
        if (walls.isEmpty() && !MapListeners.exists(mapName)) {
            player.sendMessage(Message.raw("[Wall:removed] Map not found: " + mapName));
            return CompletableFuture.completedFuture(null);
        }

        if (id < 0 || id >= walls.size()) {
            player.sendMessage(Message.raw("[Wall:removed] Index " + id + " out of range."));
            return CompletableFuture.completedFuture(null);
        }

        MapListeners.removeWall(mapName, id);
        player.sendMessage(Message.raw("[Wall:removed] Wall [" + id + "] in '" + mapName + "'"));

        return CompletableFuture.completedFuture(null);
    }
}