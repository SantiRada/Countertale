package Tenzinn.Deathmatch.Commands.Game;

import Tenzinn.Countertale;
import Tenzinn.Deathmatch.UI.DeathmatchHUD;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.protocol.packets.interface_.HudComponent;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class TestHudCommand extends AbstractPlayerCommand {

    private final Countertale main;

    public TestHudCommand(String name, String description, Countertale plugin) { super(name, description); this.main = plugin; }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {
        Player player = commandContext.senderAs(Player.class);

        DeathmatchHUD deathmatchHUD = new DeathmatchHUD(playerRef);
        player.getHudManager().setCustomHud(playerRef, deathmatchHUD);
    }

    @Override
    public String getPermission() { return "countertale.testhud"; }

    @Override
    public String getName() { return "testhud"; }
}