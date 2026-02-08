package Tenzinn.Deathmatch.Commands;

import Tenzinn.Countertale;
import Tenzinn.Deathmatch.GameMatch;

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

public class LeaveQueueCommand extends AbstractPlayerCommand {

    private final Countertale plugin;

    public LeaveQueueCommand(String name, String description, Countertale plugin) { super(name, description); this.plugin = plugin; }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {

        GameMatch match = plugin.getMatchManager().getPlayerMatch(playerRef);

        if (match == null) { commandContext.sendMessage(Message.raw("No estás en ninguna partida.").color(Color.red)); }
        else {
            if (match.getState() != GameMatch.MatchState.WAITING) {
                commandContext.sendMessage(Message.raw("Para salir de una partida en curso usa /lobby").color(Color.PINK)); return;
            }

            plugin.hideQueueHud(playerRef);
            boolean removed = plugin.getMatchManager().removePlayerFromMatch(playerRef);

            if (removed) {
                commandContext.sendMessage(Message.raw("Has salido de la cola.").color(Color.ORANGE));

                if (!match.isEmpty()) plugin.notifyMatchPlayersAndUpdateHuds(match);
            } else { commandContext.sendMessage(Message.raw("Error al salir de la partida.").color(Color.RED)); }
        }
    }

    @Override
    public String getPermission() { return "countertale.leave"; }

    @Override
    public String getName() { return "leave"; }
}