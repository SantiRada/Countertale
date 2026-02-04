package Tenzinn.Deathmatch.Commands;

import Tenzinn.Countertale;
import Tenzinn.Deathmatch.GameMatch;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.Message;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.List;

public class MatchStatusCommand extends AbstractPlayerCommand {

    private final Countertale plugin;

    public MatchStatusCommand(String name, String description, Countertale plugin) { super(name, description); this.plugin = plugin; }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {

        commandContext.sendMessage(Message.raw("========== ESTADO DE PARTIDAS =========="));

        commandContext.sendMessage(Message.raw(plugin.getMatchManager().getStats()));
        commandContext.sendMessage(Message.raw(""));

        List<GameMatch> matches = plugin.getMatchManager().getActiveMatches();

        if (matches.isEmpty()) { commandContext.sendMessage(Message.raw("No hay partidas activas.")); }
        else {
            commandContext.sendMessage(Message.raw("Partidas activas:"));

            for (int i = 0; i < matches.size(); i++) {
                GameMatch match = matches.get(i);
                String matchId = match.getMatchId().toString().substring(0, 8);
                String state = getStateColor(match.getState()) + match.getState();
                String players = String.format("%d/10", match.getPlayerCount());

                commandContext.sendMessage(Message.raw(String.format("%d. [%s] %s - %s jugadores", i + 1, matchId, state, players)));
            }
        }

        commandContext.sendMessage(Message.raw("====================================="));
    }
    private String getStateColor(GameMatch.MatchState state) {
        return switch (state) {
            case WAITING -> "W-";
            case STARTING -> "S-";
            case IN_PROGRESS -> "P-";
            case FINISHED -> "F-";
        };
    }
}