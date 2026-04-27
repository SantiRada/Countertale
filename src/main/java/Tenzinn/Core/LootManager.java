package Tenzinn.Core;

import Tenzinn.Core.UI.GameHUD;
import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.Objects.WeaponStats;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.thescar.hygunsplugin.support.hytale.PlayerInventoryAccess;
import com.thescar.hygunsplugin.ui.hud.HudCoordinator;

import java.util.ArrayList;

public class LootManager {

    public static ArrayList<WeaponStats> getGameLoot(Player player) {
        PlayerRef playerRef = Universe.get().getPlayerByUsername(player.getDisplayName(), NameMatching.EXACT);
        if (playerRef == null) return getStarterKit();

        ArrayList<WeaponStats> loot = RefactorTool.getLoot(playerRef);

        if (loot == null || loot.isEmpty()) {
            loot = getStarterKit();
        }

        RefactorTool.setAllLoot(playerRef, loot);
        return loot;
    }

    public static ArrayList<WeaponStats> getStarterKit() {
        ArrayList<WeaponStats> list = new ArrayList<>();

        ArrayList<String> item = new ArrayList<>();
        item.add("Weapon_DesertEagle");
        WeaponStats secondary = new WeaponStats("Desert Eagle", "Secondary", "Weapon", "Single", item, "DesertEagle", 7, 100);

        ArrayList<String> item2 = new ArrayList<>();
        item2.add("Armor_Iron_Legs");
        item2.add("Armor_Iron_Hands");
        item2.add("Armor_Iron_Head");
        item2.add("Armor_Iron_Chest");
        WeaponStats shield = new WeaponStats("Kevlar", "shield", "", "", item2, "Kevlar", 1, 100);

        list.add(secondary);
        list.add(shield);

        return list;
    }

    public static void giveLoot(Player player, ArrayList<WeaponStats> loot) {
        if (player == null) return;

        PlayerRef playerRef = Universe.get().getPlayerByUsername(player.getDisplayName(), NameMatching.EXACT);
        if (playerRef == null) return;
        if (playerRef.getWorldUuid() == null) return;

        boolean isDeathmatch = false;

        var playerStats = RefactorTool.getPlayerStats(playerRef);
        if (playerStats != null && playerStats.getCurrentMatch() != null) {
            isDeathmatch = playerStats.getCurrentMatch().getMode().equalsIgnoreCase("dm");
        }

        // Do not use 9999 here. It creates loads of ammo stacks and fills the inventory.
        // Deathmatch will be made effectively infinite by not consuming ammo on reload.
        final int baseAmmoAmount = isDeathmatch ? 64 : 300;
        final int rifleAmmoAmount = isDeathmatch ? 64 : 300;
        final int shotgunAmmoAmount = isDeathmatch ? 32 : 80;
        final int fuelTankAmount = isDeathmatch ? 32 : 100;
        final int fireBottleAmount = isDeathmatch ? 16 : 50;

        if (loot == null || loot.isEmpty()) {
            loot = getStarterKit();
        }

        RefactorTool.setAllLoot(playerRef, loot);

        final ArrayList<WeaponStats> finalLoot = new ArrayList<>(loot);

        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world == null) return;

        world.execute(() -> {
            player.getInventory().clear();
            Inventory inv = player.getInventory();

            WeaponStats primary = finalLoot.stream()
                    .filter(w -> w != null && w.typeWeapon != null && w.typeWeapon.equalsIgnoreCase("primary"))
                    .findFirst()
                    .orElse(null);

            WeaponStats secondary = finalLoot.stream()
                    .filter(w -> w != null && w.typeWeapon != null && w.typeWeapon.equalsIgnoreCase("secondary"))
                    .findFirst()
                    .orElse(null);

            WeaponStats shield = finalLoot.stream()
                    .filter(w -> w != null && w.typeWeapon != null && w.typeWeapon.equalsIgnoreCase("shield"))
                    .findFirst()
                    .orElse(null);

            WeaponStats utility = finalLoot.stream()
                    .filter(w -> w != null && w.typeWeapon != null && w.typeWeapon.equalsIgnoreCase("utility"))
                    .findFirst()
                    .orElse(null);

            // Give weapons first so ammo can never block the bought item.
            if (primary != null) {
                for (String itemId : primary.giveItems) {
                    inv.getHotbar().setItemStackForSlot((short) 0, new ItemStack(itemId, 1));
                    break;
                }
            }

            if (secondary != null) {
                for (String itemId : secondary.giveItems) {
                    inv.getHotbar().setItemStackForSlot((short) 1, new ItemStack(itemId, 1));
                    break;
                }
            }

            inv.getHotbar().setItemStackForSlot((short) 2, new ItemStack("Weapon_Daggers_Cobalt", 1));

            if (utility != null) {
                short utilitySlot = 3;

                for (String itemId : utility.giveItems) {
                    if (utilitySlot <= 8) {
                        inv.getHotbar().setItemStackForSlot(utilitySlot, new ItemStack(itemId, 1));
                        utilitySlot++;
                    } else {
                        inv.getStorage().addItemStack(new ItemStack(itemId, 1));
                    }
                }
            }

            if (shield != null) {
                for (String itemId : shield.giveItems) {
                    inv.getArmor().addItemStack(new ItemStack(itemId, 1));
                }
            }

            // Give ammo after weapons, using controlled amounts.
            inv.getStorage().addItemStack(new ItemStack("Ammo_Bullet_Base", baseAmmoAmount));
            inv.getStorage().addItemStack(new ItemStack("Ammo_Bullet_Rifle", rifleAmmoAmount));
            inv.getStorage().addItemStack(new ItemStack("Ammo_Bullet_Shotgun", shotgunAmmoAmount));
            inv.getStorage().addItemStack(new ItemStack("Ammo_Fuel_Tank", fuelTankAmount));
            inv.getStorage().addItemStack(new ItemStack("Ammo_Fuel_FireBottle", fireBottleAmount));

            Ref<EntityStore> entityRef = playerRef.getReference();
            if (entityRef != null) {
                Store<EntityStore> entityStore = entityRef.getStore();

                ComponentType<EntityStore, EntityStatMap> statMapType =
                        EntityStatsModule.get().getEntityStatMapComponentType();

                EntityStatMap statMap = entityStore.getComponent(entityRef, statMapType);
                if (statMap != null) {
                    statMap.maximizeStatValue(DefaultEntityStatTypes.getHealth());
                }
            }

            CustomUIHud customHUD = player.getHudManager().getCustomHud();
            if (customHUD instanceof GameHUD newHud) {
                newHud.setWeapons(player.getInventory().getActiveHotbarSlot() + 1);
                newHud.setShield();

                HudCoordinator.attachPlayer(playerRef, player);

                ItemStack held = PlayerInventoryAccess.getItemInHand(player);
                if (held == null) {
                    HudCoordinator.hideAmmo(playerRef);
                } else {
                    HudCoordinator.updateAmmo(playerRef, held);
                }
            }
        });
    }

    public static void getLobbyLoot(Player player) {
        player.getInventory().clear();

        Inventory inv = player.getInventory();
        ItemStack actionBook = new ItemStack("actions_book", 1);

        inv.getHotbar().addItemStack(actionBook);
    }
}
