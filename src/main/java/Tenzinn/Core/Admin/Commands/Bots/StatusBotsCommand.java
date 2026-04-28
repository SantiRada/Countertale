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

public class StatusBotsCommand extends AbstractPlayerCommand {

    public StatusBotsCommand(@NonNullDecl String name, @NonNullDecl String description) {
        super(name, description);
    }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext,
                           @NonNullDecl Store<EntityStore> store,
                           @NonNullDecl Ref<EntityStore> ref,
                           @NonNullDecl PlayerRef playerRef,
                           @NonNullDecl World world) {
        commandContext.sendMessage(Message.raw("=== Deathmatch Bots ==="));
        commandContext.sendMessage(Message.raw("Enabled: " + DeathmatchBotManager.isBotsEnabled()));
        commandContext.sendMessage(Message.raw("Ticker running: " + DeathmatchBotManager.isTickerRunning()));
        commandContext.sendMessage(Message.raw("fillDeathmatchTo: " + DeathmatchBotManager.getConfiguredFillDeathmatchTo()));
        commandContext.sendMessage(Message.raw("Projectile mode: " + DeathmatchBotManager.isBotProjectileModeEnabled()));
        commandContext.sendMessage(Message.raw("Direct fallback: " + DeathmatchBotManager.isBotDirectDamageFallbackEnabled()));
        commandContext.sendMessage(Message.raw("Yaw offset (deg): " + DeathmatchBotManager.getBotYawOffsetDegrees()));
        commandContext.sendMessage(Message.raw("Active bots (all matches): " + DeathmatchBotManager.getTotalBotCountAllMatches()));
        commandContext.sendMessage(Message.raw("Alive bots (all matches): " + DeathmatchBotManager.getAliveBotCountAllMatches()));
        commandContext.sendMessage(Message.raw("Config: " + DeathmatchBotManager.getConfigPath()));

        PlayerStats stats = RefactorTool.getPlayerStats(playerRef);
        if (stats == null || stats.getCurrentMatch() == null) {
            commandContext.sendMessage(Message.raw("You are not in an active match."));
            return;
        }

        GameMatch match = stats.getCurrentMatch();
        if (!"dm".equalsIgnoreCase(match.getMode())) {
            commandContext.sendMessage(Message.raw("Current match mode is not Deathmatch."));
            return;
        }

        commandContext.sendMessage(Message.raw("Current match: " + match.getMatchId()));
        commandContext.sendMessage(Message.raw("Match bots total/alive: "
                + DeathmatchBotManager.getTotalBotCount(match)
                + "/"
                + DeathmatchBotManager.getAliveBotCount(match)));
    }

    @Override
    public String getPermission() { return "orbisoffensive.bots.status"; }

    @Override
    public String getName() { return "status"; }
}
