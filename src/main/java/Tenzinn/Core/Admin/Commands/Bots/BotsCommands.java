package Tenzinn.Core.Admin.Commands.Bots;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class BotsCommands extends AbstractCommandCollection {

    public BotsCommands(@NonNullDecl String name, @NonNullDecl String description) {
        super(name, description);

        addSubCommand(new StatusBotsCommand("status", "Show Deathmatch bot runtime status."));
        addSubCommand(new ReloadBotsCommand("reload", "Reload bots.json safely."));
        addSubCommand(new ClearBotsCommand("clear", "Remove all active Deathmatch bots."));
        addSubCommand(new RespawnBotsCommand("respawn", "Respawn filler bots in your current Deathmatch."));
        addSubCommand(new EnableBotsCommand("enable", "Enable Deathmatch bots."));
        addSubCommand(new DisableBotsCommand("disable", "Disable Deathmatch bots and clear active bots."));
    }

    @Override
    public String getPermission() { return "orbisoffensive.bots"; }

    @Override
    public String getName() { return "bots"; }
}
