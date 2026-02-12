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

        player.sendMessage(Message.raw("Name World: " + player.getWorld().getName()));

        if(player.getWorld().getName().equals("default")) {
            Object hud = RefactorTool.getCustomHud(playerRef);
            if(hud != null){
                if(hud instanceof DeathmatchHUD){
                    DeathmatchHUD newHud = (DeathmatchHUD) hud;
                    newHud.clearHUD();
                }
            }

            getLobbyLoot(player);
        }
        else {
            openGameHud(playerRef, player);
            getGameLoot(player);

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

        GameMatch match = Objects.requireNonNull(RefactorTool.getPlayerStats(playerRef)).getCurrentMatch();
        if (match != null) match.startTimer();
    }
    public static void getLobbyLoot(Player player) {
        player.getInventory().clear();

        Inventory inv = player.getInventory();
        ItemStack actionBook = new ItemStack("actions_book", 1);

        inv.getHotbar().addItemStack(actionBook);
    }
    public static void getGameLoot(Player player) {
        World playerWorld = player.getWorld();
        if (playerWorld == null) { return; }

        // Get-Item
        player.getInventory().clear();
        Inventory inv = player.getInventory();

        ItemStack gun = new ItemStack("Weapon_Handgun", 1);
        ItemStack knife = new ItemStack("Weapon_Daggers_Cobalt", 1);
        ItemStack bullets = new ItemStack("Weapon_Arrow_Crude", 3600);

        inv.getHotbar().addItemStack(gun);
        inv.getHotbar().addItemStack(knife);
        inv.getStorage().addItemStack(bullets);

        inv.setActiveSlot(0, (byte) 0);
    }
}