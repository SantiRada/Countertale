package Tenzinn.Core.Events;

import Tenzinn.Core.Cases.CaseManager;
import Tenzinn.Core.GameMatch;
import Tenzinn.Core.Storage.DatabaseManager;
import Tenzinn.Core.UI.GameHUD;
import Tenzinn.Core.LootManager;
import Tenzinn.Core.Shop.ShopData;
import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.Objects.PlayerStats;
import Tenzinn.Core.Objects.WeaponStats;
import Tenzinn.Core.Effects.PlayerEntityEffect;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.protocol.packets.interface_.HudComponent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Objects;
import java.util.ArrayList;
import java.util.UUID;
import Tenzinn.Core.Cases.ArmorySkinAssets;

public class DetectPlayerReady {

    public static void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        Ref<EntityStore> entityRef = Objects.requireNonNull(player.getReference());
        UUIDComponent uuidComp = entityRef.getStore().getComponent(entityRef, UUIDComponent.getComponentType());
        PlayerRef playerRef = (uuidComp != null) ? Universe.get().getPlayer(uuidComp.getUuid()) : null;

        assert playerRef != null;
        PlayerEntityEffect.clearAllEffects(player, entityRef.getStore());

        ShopData.loadContent();
        assert player.getWorld() != null;

        if (Objects.equals(Universe.get().getDefaultWorld(), Universe.get().getWorld(player.getWorld().getName()))) {
            player.getHudManager().showHudComponents(playerRef, HudComponent.Chat);
            player.getHudManager().showHudComponents(playerRef, HudComponent.Hotbar);
            player.getHudManager().showHudComponents(playerRef, HudComponent.Health);
            player.getHudManager().showHudComponents(playerRef, HudComponent.Compass);
            player.getHudManager().showHudComponents(playerRef, HudComponent.Stamina);
            player.getHudManager().showHudComponents(playerRef, HudComponent.Reticle);
            player.getHudManager().showHudComponents(playerRef, HudComponent.InputBindings);

            UUID uuid = playerRef.getUuid();
            DatabaseManager.registerPlayer(uuid, playerRef.getUsername()).thenRun(() -> {
                CaseManager.loadInventoryFromDb(uuid);
                CaseManager.loadCasesFromDb(uuid);
                CaseManager.loadSelectedSkinsFromDb(uuid);
            });

            ArmorySkinAssets.announceMissingIconWarnings(playerRef);
            LootManager.getLobbyLoot(player);
        }
        else {
            openGameHud(playerRef, player);
            RefactorTool.Respawn(playerRef);
        }
    }

    public static void openGameHud(PlayerRef playerRef, Player player) {
        assert player.getWorld() != null;
        String worldMode = player.getWorld().getName().startsWith("fvf") ? "fvf" : "dm";

        ArrayList<WeaponStats> thisLoot = LootManager.getGameLoot(player);
        assert thisLoot != null;

        if (worldMode.equalsIgnoreCase("dm")) { RefactorTool.setAllLoot(playerRef, thisLoot); }
        else {
            RefactorTool.setAllLoot(playerRef, LootManager.getStarterKit());
            thisLoot = LootManager.getStarterKit();
        }

        GameHUD newHud = new GameHUD(playerRef);
        player.getHudManager().addCustomHud(playerRef, newHud);

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
        RefactorTool.launchSound(playerRef, "clic");
    }
}
