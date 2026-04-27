package Tenzinn.Core.Commands.Economy;

import Tenzinn.Core.Shop.RevenuesConfig;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;
import java.util.*;

public class SetRevenueCommand extends AbstractPlayerCommand {

    private final OptionalArg<String> type;
    private final OptionalArg<Integer> value;

    private String typeAlias;
    private int valueAlias;

    public SetRevenueCommand(String name, String description) {
        super(name, description);

        type = withOptionalArg("type", "Select type revenue in CAPITALIZE: --type=KILL...", ArgTypes.STRING);
        value = withOptionalArg("value", "Enter a new value for the selected revenue type",  ArgTypes.INTEGER);
    }

    @Override
    protected void execute(@NonNullDecl CommandContext context, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {
        if (!type.provided(context)) {
            playerRef.sendMessage(Message.raw("Not is optional --type and --value").color(Color.RED));
            return;
        }

        typeAlias = type.get(context);
        valueAlias = value.get(context);

        applyGlobal(playerRef);
    }

    private void applyGlobal(PlayerRef sender) {
        RevenuesConfig.updateValue(typeAlias, valueAlias);
        sender.sendMessage(Message.raw("Type | " + typeAlias + " | correctly modified").color(Color.GREEN));
    }

    @Override
    public String getPermission() { return "OrbisOffensive.revenue.set"; }

    @Override
    public String getName() { return "revenue.set"; }
}