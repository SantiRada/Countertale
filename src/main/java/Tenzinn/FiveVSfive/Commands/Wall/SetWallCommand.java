package Tenzinn.FiveVSfive.Commands.Wall;

import Tenzinn.Core.Listeners.MapListeners;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;

import com.hypixel.hytale.server.core.entity.entities.Player;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class SetWallCommand extends AbstractAsyncCommand {

    private final OptionalArg<String> mapArg;

    private final OptionalArg<String> startArg;
    private final OptionalArg<String> fromArg;

    private final OptionalArg<String> endArg;
    private final OptionalArg<String> toArg;

    public SetWallCommand(@NonNullDecl String name, @NonNullDecl String description) {
        super(name, description);

        mapArg   = withOptionalArg("map",   "Map name (uses active session map if omitted)", ArgTypes.STRING);

        startArg = withOptionalArg("start", "Start coordinate [x,y,z]", ArgTypes.STRING);
        fromArg  = withOptionalArg("from",  "Start coordinate [x,y,z] (alias for --start)", ArgTypes.STRING);

        endArg   = withOptionalArg("end",   "End coordinate [x,y,z]",   ArgTypes.STRING);
        toArg    = withOptionalArg("to",    "End coordinate [x,y,z] (alias for --end)",   ArgTypes.STRING);
    }

    @NullableDecl @Override
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
        Player player = context.senderAs(Player.class);
        double[] from = new double[3];
        double[] to = new double[3];

        if (startArg.get(context) != null || fromArg.get(context) != null) {
            String base = startArg.get(context) != null ? startArg.get(context) : fromArg.get(context);
            String[] xyz = base.split("\\.");

            for (int i = 0; i < xyz.length; i++) {
                if (xyz[i].isEmpty() || !xyz[i].matches("[0-9,-]+")) {
                    player.sendMessage(Message.raw("Invalid start points").color(Color.cyan));
                    break;
                }

                from[i] = Double.parseDouble(xyz[i]);
            }
        }
        if (endArg.get(context) != null || toArg.get(context) != null) {
            String base = endArg.get(context) != null ? endArg.get(context) : toArg.get(context);
            String[] xyz = base.split("\\.");

            for (int i = 0; i < xyz.length; i++) {
                if (xyz[i].isEmpty() || !xyz[i].matches("[0-9,-]+")) {
                    player.sendMessage(Message.raw("Invalid end points").color(Color.cyan));
                    break;
                }

                to[i] = Double.parseDouble(xyz[i]);
            }
        }

        if(startArg.get(context) == null && endArg.get(context) == null && fromArg.get(context) == null && toArg.get(context) == null) {
            player.sendMessage(Message.raw("Values (--start or --from) and (--end or --to) are required"));
            return null;
        }

        if(mapArg.get(context) != null) {
            if (!MapListeners.exists(mapArg.get(context))) {
                player.sendMessage(Message.raw("The map you are trying to edit does not exist or has a different name").color(Color.cyan));
                return null;
            }
        }
        else {
            player.sendMessage(Message.raw("You must define a map to save TemporalWall").color(Color.cyan));
            return null;
        }

        MapListeners.TemporalWall newTemporalWall = new MapListeners.TemporalWall(from[0], from[1], from[2], to[0], to[1], to[2]);
        MapListeners.addWall(mapArg.get(context), newTemporalWall);
        player.sendMessage(Message.raw("[Wall:Set] new Wall in '" + mapArg.get(context) + "'"));

        return CompletableFuture.completedFuture(null);
    }
    static double[] parseCoords(String raw) {
        if (raw == null) return null;
        String clean = raw.replaceAll("[\\[\\]\\s]", "");
        String[] parts = clean.split(",");
        if (parts.length != 3) return null;
        try {
            return new double[]{
                    Double.parseDouble(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2])
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String getPermission() { return "OrbisOffensive.Wall.set"; }

    @Override
    public String getName() { return "wall.set"; }
}
