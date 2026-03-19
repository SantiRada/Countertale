package Tenzinn.Core.Events;

import Tenzinn.Core.GameMatch;
import Tenzinn.Core.UI.GameHUD;
import Tenzinn.Core.LootManager;
import Tenzinn.Core.Shop.ShopData;
import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.Objects.PlayerStats;
import Tenzinn.Core.Objects.WeaponStats;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.protocol.packets.interface_.HudComponent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;

import java.util.Objects;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

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
        // Review active mode for this player
        assert player.getWorld() != null;
        String worldMode = player.getWorld().getName().startsWith("fvf") ? "fvf" : "dm";

        ArrayList<WeaponStats> thisLoot = LootManager.getGameLoot(player);
        assert thisLoot != null;

        // Get loot for mode
        if (worldMode.equalsIgnoreCase("dm")) { RefactorTool.setAllLoot(playerRef, thisLoot); }
        else { RefactorTool.setAllLoot(playerRef, null); }

        GameHUD newHud = new GameHUD(playerRef);
        player.getHudManager().setCustomHud(playerRef, newHud);
        PlayerStats playerStats = RefactorTool.getPlayerStats(playerRef);

        if(worldMode.equalsIgnoreCase("dm")) {
            newHud.setEffect(PlayerStats.Effects.INVULNERABILITY);

            if (playerStats != null) { playerStats.isInvulnerable = true; }
        }

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

        if (worldMode.equalsIgnoreCase("dm")) {
            if (RefactorTool.getPlayer(playerRef) != null) {
                GameMatch match = Objects.requireNonNull(RefactorTool.getPlayerStats(playerRef)).getCurrentMatch();
                if (match != null) { match.startTimer(); }
            }
        }

        LootManager.giveLoot(player, thisLoot);

        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            if (playerStats.damageReceived <= 0) {
                playerStats.setHealth((int)RefactorTool.getMaxHealth(player));
                RefactorTool.setDamageReceived(player, -1);
            }
        }, 1, TimeUnit.SECONDS);

        RefactorTool.launchSound(playerRef, "clic");
    }
}