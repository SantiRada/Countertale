package Tenzinn.Deathmatch.Content.Commands.Game;

import Tenzinn.Countertale;
import Tenzinn.Deathmatch.Global.GameMatch;

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
import java.util.List;

public class StatusGameCommand extends AbstractPlayerCommand {

    private final Countertale main;

    public StatusGameCommand(String name, String description, Countertale main) { super(name, description); this.main = main; }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {

        commandContext.sendMessage(Message.raw("========== STATEMENT OF GAMES =========="));

        commandContext.sendMessage(Message.raw(main.getMatchManager().getStats()).color(Color.ORANGE));
        commandContext.sendMessage(Message.raw(main.getMatchManager().getPlayers()).color(Color.ORANGE));
        commandContext.sendMessage(Message.raw(main.getMatchManager().getInstances()).color(Color.ORANGE));
        commandContext.sendMessage(Message.raw("-------------------------------------"));

        List<GameMatch> matches = main.getMatchManager().getActiveMatches();

        if (matches.isEmpty()) { commandContext.sendMessage(Message.raw("There are no active games.").color(Color.MAGENTA)); }
        else {
            commandContext.sendMessage(Message.raw("Active games:"));

            for (int i = 0; i < matches.size(); i++) {
                GameMatch match = matches.get(i);
                String matchId = match.getMatchId().toString().substring(0, 8);
                String state = getStateColor(match.getState()) + match.getState();
                String players = String.format("%d/10", match.getPlayerCount());

                commandContext.sendMessage(Message.raw(String.format("%d. [%s] %s - %s players", i + 1, matchId, state, players)));
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

    @Override
    public String getPermission() { return "countertale.game.status"; }

    @Override
    public String getName() { return "status"; }
}