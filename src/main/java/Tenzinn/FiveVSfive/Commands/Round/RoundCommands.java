package Tenzinn.FiveVSfive.Commands.Round;

import Tenzinn.Core.Localization.Lang;
import Tenzinn.FiveVSfive.Flow.MatchFVF;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;

public class RoundCommands extends AbstractCommandCollection {

    public RoundCommands(@NonNullDecl String name, @NonNullDecl String description) {
        super(name, description);

        addSubCommand(new AddRoundCommand("t1", "Give 1 round win to team 1", 1));
        addSubCommand(new AddRoundCommand("t2", "Give 1 round win to team 2", 2));
        addSubCommand(new WinMatchCommand("w1", "Make team 1 win the match", 1));
        addSubCommand(new WinMatchCommand("w2", "Make team 2 win the match", 2));
    }

    @Override
    public String getPermission() { return "countertale.round"; }

    @Override
    public String getName() { return "round"; }

    // ── Subcommands ───────────────────────────────────────────────────────────

    private static class AddRoundCommand extends AbstractPlayerCommand {
        private final int team;

        AddRoundCommand(String name, String description, int team) {
            super(name, description);
            this.team = team;
        }

        @Override
        protected void execute(@NonNullDecl CommandContext ctx, @NonNullDecl Store<EntityStore> store,
                               @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef,
                               @NonNullDecl World world) {
            if (!MatchFVF.isMatchActive()) {
                playerRef.sendMessage(Lang.msg("round.no-active-match").color(Color.red));
                return;
            }
            MatchFVF.forceAddRound(team);
            playerRef.sendMessage(Lang.msg("round.given", "team", team).color(Color.green));
        }

        @Override
        public String getPermission() { return "countertale.round"; }
    }

    private static class WinMatchCommand extends AbstractPlayerCommand {
        private final int team;

        WinMatchCommand(String name, String description, int team) {
            super(name, description);
            this.team = team;
        }

        @Override
        protected void execute(@NonNullDecl CommandContext ctx, @NonNullDecl Store<EntityStore> store,
                               @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef,
                               @NonNullDecl World world) {
            if (!MatchFVF.isMatchActive()) {
                playerRef.sendMessage(Lang.msg("round.no-active-match").color(Color.red));
                return;
            }
            MatchFVF.forceWinMatch(team);
            playerRef.sendMessage(Lang.msg("round.match-won", "team", team).color(Color.green));
        }

        @Override
        public String getPermission() { return "countertale.round"; }
    }
}
