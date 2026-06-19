package Tenzinn.Core.Admin.Commands;

import Tenzinn.Core.GameMatch;
import Tenzinn.Core.Localization.Lang;
import Tenzinn.Core.Objects.PlayerStats;
import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Deathmatch.Flow.MatchDeathmatch;
import Tenzinn.FiveVSfive.Flow.MatchFVF;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class EndGameCommand extends AbstractPlayerCommand {

    private static final int DEBUG_END_SECONDS = 5;

    public EndGameCommand(String name, String description) {
        super(name, description);
    }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext,
                           @NonNullDecl Store<EntityStore> store,
                           @NonNullDecl Ref<EntityStore> ref,
                           @NonNullDecl PlayerRef playerRef,
                           @NonNullDecl World world) {
        PlayerStats stats = RefactorTool.getPlayerStats(playerRef);
        if (stats == null || stats.getCurrentMatch() == null) {
            commandContext.sendMessage(Lang.msg("admin.endgame.not-in-match"));
            return;
        }

        GameMatch match = stats.getCurrentMatch();
        boolean updated = match.getMode().equalsIgnoreCase("dm")
                ? MatchDeathmatch.forceEndMatchIn(DEBUG_END_SECONDS)
                : MatchFVF.forceEndMatchIn(DEBUG_END_SECONDS);

        commandContext.sendMessage(updated
                ? Lang.msg("admin.endgame.started", "seconds", DEBUG_END_SECONDS)
                : Lang.msg("admin.endgame.no-active-timer"));
    }

    @Override
    public String getPermission() { return "countertale.endgame"; }

    @Override
    public String getName() { return "endgame"; }
}
