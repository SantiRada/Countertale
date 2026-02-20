package Tenzinn.Events;

import Tenzinn.Deathmatch.LootManager;
import Tenzinn.Tools.RefactorTool;
import Tenzinn.Deathmatch.GameMatch;
import Tenzinn.Deathmatch.UI.DeathmatchHUD;
import Tenzinn.Deathmatch.Objects.WeaponStats;

import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.protocol.packets.interface_.HudComponent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;

import java.awt.*;
import java.util.Objects;
import java.util.ArrayList;

public class DetectPlayerReady {

    public static void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        PlayerRef playerRef = Universe.get().getPlayerByUsername(player.getDisplayName(), NameMatching.EXACT);

        assert player.getWorld() != null;

        if (Objects.equals(Universe.get().getDefaultWorld(), Universe.get().getWorld(player.getWorld().getName()))) {
            assert playerRef != null;
            player.getHudManager().showHudComponents(playerRef, HudComponent.Chat);
            player.getHudManager().showHudComponents(playerRef, HudComponent.Hotbar);
            player.getHudManager().showHudComponents(playerRef, HudComponent.Health);
            player.getHudManager().showHudComponents(playerRef, HudComponent.Compass);
            player.getHudManager().showHudComponents(playerRef, HudComponent.Stamina);
            player.getHudManager().showHudComponents(playerRef, HudComponent.Reticle);
            player.getHudManager().showHudComponents(playerRef, HudComponent.InputBindings);

            getLobbyLoot(player);
        }
        else {
            openGameHud(playerRef, player);

            assert playerRef != null;
            RefactorTool.Respawn(playerRef);
        }
    }
    public static void openGameHud(PlayerRef playerRef, Player player) {
        ArrayList<WeaponStats> thisLoot = LootManager.getGameLoot(player);

        if (thisLoot != null) {
            RefactorTool.setAllLoot(playerRef, thisLoot);
            LootManager.giveLoot(player, thisLoot);
        }

        DeathmatchHUD newHud = new DeathmatchHUD(playerRef);
        player.getHudManager().setCustomHud(playerRef, newHud);

        if (thisLoot != null) {
            newHud.setShield(playerRef);
            newHud.setWeapons(2);
        }

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

        player.getHudManager().hideHudComponents(playerRef, HudComponent.Hotbar);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.Health);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.Stamina);

        GameMatch match = Objects.requireNonNull(RefactorTool.getPlayerStats(playerRef)).getCurrentMatch();
        if (match != null) match.startTimer();
    }
    public static void getLobbyLoot(Player player) {
        player.getInventory().clear();

        Inventory inv = player.getInventory();
        ItemStack actionBook = new ItemStack("actions_book", 1);

        inv.getHotbar().addItemStack(actionBook);
    }
}