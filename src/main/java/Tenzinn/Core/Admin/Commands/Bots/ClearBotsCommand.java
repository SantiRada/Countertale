package Tenzinn.Core.Admin.Commands.Bots;

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

public class ClearBotsCommand extends AbstractPlayerCommand {

    public ClearBotsCommand(@NonNullDecl String name, @NonNullDecl String description) {
        super(name, description);
    }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext,
                           @NonNullDecl Store<EntityStore> store,
                           @NonNullDecl Ref<EntityStore> ref,
                           @NonNullDecl PlayerRef playerRef,
                           @NonNullDecl World world) {
        DeathmatchBotManager.removeAllBots();
        commandContext.sendMessage(Message.raw("Cleared all active Deathmatch bots."));
    }

    @Override
    public String getPermission() { return "orbisoffensive.bots.clear"; }

    @Override
    public String getName() { return "clear"; }
}
