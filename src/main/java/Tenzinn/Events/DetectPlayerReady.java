package Tenzinn.Events;

import Tenzinn.Tools.RefactorTool;
import Tenzinn.Deathmatch.GameMatch;
import Tenzinn.Deathmatch.LootManager;
import Tenzinn.Deathmatch.Shop.ShopData;
import Tenzinn.Deathmatch.UI.DeathmatchHUD;
import Tenzinn.Deathmatch.Objects.WeaponStats;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
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

        ShopData.loadContent();

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

            LootManager.getLobbyLoot(player);
        }
        else {
            openGameHud(playerRef, player);

            assert playerRef != null;
            RefactorTool.Respawn(playerRef);
        }
    }
    public static void openGameHud(PlayerRef playerRef, Player player) {
        ArrayList<WeaponStats> thisLoot = LootManager.getGameLoot(player);

        // TENGO QUE SEGUIR VIENDO PORQUE EL LOOT NO CARGA CUANDO LO ELEGIS DESDE EL LOBBY
        if(thisLoot != null) {
            player.sendMessage(Message.raw("Se encontró el loot").color(Color.red));
            if(!thisLoot.isEmpty()) {
                player.sendMessage(Message.raw("Kit (" + thisLoot.size() + ")"));
                int i = 1;
                for(WeaponStats item : thisLoot) {
                    player.sendMessage(Message.raw("Slot " + i + ": " + item.nameWeapon));
                    i += 1;
                }
            } else {
                player.sendMessage(Message.raw("El Loot está vacío").color(Color.red));
            }
        } else {
            player.sendMessage(Message.raw("No se encuentra el Loot").color(Color.red));
        }

        if (thisLoot != null) {
            RefactorTool.setAllLoot(playerRef, thisLoot);
            LootManager.giveLoot(player, thisLoot);
        }

        DeathmatchHUD newHud = new DeathmatchHUD(playerRef);
        player.getHudManager().setCustomHud(playerRef, newHud);

        player.getHudManager().hideHudComponents(playerRef, HudComponent.Hotbar);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.Health);
        player.getHudManager().hideHudComponents(playerRef, HudComponent.Stamina);

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

        if (RefactorTool.getPlayer(playerRef) != null) {
            GameMatch match = Objects.requireNonNull(RefactorTool.getPlayerStats(playerRef)).getCurrentMatch();
            if (match != null) match.startTimer();
        }

        if (thisLoot != null) {
            newHud.setShield(playerRef);
            newHud.setWeapons(0);
        }
    }
}