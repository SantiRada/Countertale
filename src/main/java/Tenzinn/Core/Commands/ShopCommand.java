package Tenzinn.Core.Commands;

import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.UI.ShopPage;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;

public class ShopCommand extends AbstractPlayerCommand {

    public ShopCommand(String name, String description) { super(name, description); }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext,
                           @NonNullDecl Store<EntityStore> store,
                           @NonNullDecl Ref<EntityStore> ref,
                           @NonNullDecl PlayerRef playerRef,
                           @NonNullDecl World world) {
        Player player = commandContext.senderAs(Player.class);
        if (player == null) return;

        var stats = RefactorTool.getPlayerStats(playerRef);
        if (stats == null || stats.getCurrentMatch() == null) {
            playerRef.sendMessage(com.hypixel.hytale.server.core.Message.raw("You must be in a match to open the shop.").color(Color.orange));
            return;
        }

        player.getPageManager().openCustomPage(ref, store, new ShopPage(playerRef, RefactorTool.getModeForPlayer(playerRef)));
    }

    @Override
    public String getPermission() { return "OrbisOffensive.shop"; }

    @Override
    public String getName() { return "shop"; }
}