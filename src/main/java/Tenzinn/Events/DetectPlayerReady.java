package Tenzinn.Events;

import Tenzinn.Deathmatch.GameMatch;
import Tenzinn.Deathmatch.UI.DeathmatchHUD;

import Tenzinn.Tools.RefactorTool;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.protocol.packets.interface_.HudComponent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;

import java.awt.*;
import java.util.Objects;

public class DetectPlayerReady {

    public static void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        PlayerRef playerRef = Universe.get().getPlayerByUsername(player.getDisplayName(), NameMatching.EXACT);

        assert player.getWorld() != null;
        player.sendMessage(Message.raw("Name World: " + player.getWorld().getName()));

        System.out.println("[[[[[[[[[ El usuario " + player.getDisplayName() + " ingresó al mundo " + player.getWorld().getName() + " ]]]]]]]]]");

        if (Universe.get().getDefaultWorld().equals(Universe.get().getWorld(player.getWorld().getName()))) { getLobbyLoot(player, playerRef); }
        else {
            openGameHud(playerRef, player);
            getGameLoot(player);

            assert playerRef != null;
            RefactorTool.Respawn(playerRef);
        }
    }
    public static void openGameHud(PlayerRef playerRef, Player player) {
        DeathmatchHUD newHud = new DeathmatchHUD(playerRef);
        player.getHudManager().setCustomHud(playerRef, newHud);

        player.getHudManager().hideHudComponents(playerRef, HudComponent.Mana);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.Sleep);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.Oxygen);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.Compass);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.Reticle);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.Requests);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.PortalPanel);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.Speedometer);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.StatusIcons);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.InputBindings);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.BuilderToolsLegend);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.UtilitySlotSelector);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.BlockVariantSelector);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.BuilderToolsMaterialSlotSelector);

        player.getHudManager().hideHudComponents(playerRef, HudComponent.Health);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.Stamina);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.Hotbar);

        GameMatch match = Objects.requireNonNull(RefactorTool.getPlayerStats(playerRef)).getCurrentMatch();
        if (match != null) match.startTimer();
    }
    public static void getLobbyLoot(Player player, PlayerRef playerRef) {
        player.getInventory().clear();

        player.getHudManager().showHudComponents(playerRef, HudComponent.Chat);
        player.getHudManager().showHudComponents(playerRef, HudComponent.Hotbar);
        player.getHudManager().showHudComponents(playerRef, HudComponent.Health);
        player.getHudManager().showHudComponents(playerRef, HudComponent.Compass);
        player.getHudManager().showHudComponents(playerRef, HudComponent.Stamina);
        player.getHudManager().showHudComponents(playerRef, HudComponent.Reticle);
        player.getHudManager().showHudComponents(playerRef, HudComponent.InputBindings);

        Inventory inv = player.getInventory();
        ItemStack actionBook = new ItemStack("actions_book", 1);

        inv.getHotbar().addItemStack(actionBook);
    }
    public static void getGameLoot(Player player) {
        World playerWorld = player.getWorld();
        if (playerWorld == null) { return; }

        player.getInventory().clear();
        Inventory inv = player.getInventory();

        ItemStack rifle = new ItemStack("Weapon_Assault_Rifle", 1);
        ItemStack gun = new ItemStack("Weapon_Handgun", 1);
        ItemStack knife = new ItemStack("Weapon_Daggers_Cobalt", 1);
        ItemStack bullets = new ItemStack("Weapon_Arrow_Crude", 3600);

        inv.getHotbar().addItemStack(rifle);
        inv.getHotbar().addItemStack(gun);
        inv.getHotbar().addItemStack(knife);
        inv.getStorage().addItemStack(bullets);
    }
}