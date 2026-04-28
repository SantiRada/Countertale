package Tenzinn.Core.Admin.Commands.Bots;

import Tenzinn.Core.GameMatch;
import Tenzinn.Core.Objects.PlayerStats;
import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Deathmatch.Bots.DeathmatchBotManager;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class RespawnBotsCommand extends AbstractPlayerCommand {

    public RespawnBotsCommand(@NonNullDecl String name, @NonNullDecl String description) {
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
            commandContext.sendMessage(Message.raw("You are not in an active match."));
            return;
        }

        GameMatch match = stats.getCurrentMatch();
        if (!"dm".equalsIgnoreCase(match.getMode())) {
            commandContext.sendMessage(Message.raw("Bots respawn is Deathmatch-only."));
            return;
        }
        if (match.getMapId() == null || match.getMapId().isBlank()) {
            commandContext.sendMessage(Message.raw("Current Deathmatch has no map ID."));
            return;
        }
        if (match.getState() == GameMatch.MatchState.FINISHED) {
            commandContext.sendMessage(Message.raw("Current Deathmatch is already finished."));
            return;
        }

        boolean ok = DeathmatchBotManager.respawnFillBots(match, world);
        if (!ok) {
            commandContext.sendMessage(Message.raw("Unable to respawn Deathmatch bots for this match."));
            return;
        }

        commandContext.sendMessage(Message.raw("Deathmatch bots respawn requested for this match."));
    }

    @Override
    public String getPermission() { return "orbisoffensive.bots.respawn"; }

    @Override
    public String getName() { return "respawn"; }
}
