package Tenzinn.Deathmatch;

import Tenzinn.Tools.RefactorTool;
import Tenzinn.Deathmatch.Objects.WeaponStats;

import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.entity.entities.Player;

import java.awt.*;
import java.util.ArrayList;

public class LootManager {

    public static ArrayList<WeaponStats> getGameLoot(Player player) {
        PlayerRef playerRef = Universe.get().getPlayerByUsername(player.getDisplayName(), NameMatching.EXACT);
        if (playerRef == null) return null;

        ArrayList<WeaponStats> loot = RefactorTool.getLoot(playerRef);
        if (loot == null) return getStarterKit();
        if (loot.isEmpty()) return getStarterKit();

        return loot;
    }

    public static ArrayList<WeaponStats> getStarterKit() {
        ArrayList<WeaponStats> list = new ArrayList<>();

        ArrayList<String> item = new ArrayList<>();
        item.add("Weapon_Handgun");
        WeaponStats secondary = new WeaponStats("Desert Eagle", "Secondary", "Weapon", "Single", item, "DesertEagle", 7);

        ArrayList<String> item2 = new ArrayList<>();
        item2.add("Armor_Iron_Legs");
        item2.add("Armor_Iron_Hands");
        item2.add("Armor_Iron_Head");
        item2.add("Armor_Iron_Chest");
        WeaponStats shield = new WeaponStats("Kevlar", "shield", "", "", item2, "Kevlar", 1);

        list.add(secondary);
        list.add(shield);

        return list;
    }

    public static void giveLoot(Player player, ArrayList<WeaponStats> loot) {

        player.getInventory().clear();
        Inventory inv = player.getInventory();

        ItemStack bullets = new ItemStack("Weapon_Arrow_Crude", 3600);
        inv.getStorage().addItemStack(bullets);

        WeaponStats primary   = loot.stream().filter(w -> w != null && w.typeWeapon.equalsIgnoreCase("primary")).findFirst().orElse(null);
        WeaponStats secondary = loot.stream().filter(w -> w != null && w.typeWeapon.equalsIgnoreCase("secondary")).findFirst().orElse(null);
        WeaponStats shield    = loot.stream().filter(w -> w != null && w.typeWeapon.equalsIgnoreCase("shield")).findFirst().orElse(null);

        // Slot 0 - Primary
        if (primary != null) { for (String itemId : primary.giveItems) { inv.getHotbar().addItemStack(new ItemStack(itemId, 1)); } }

        // Slot 1 - Secondary
        if (secondary != null) { for (String itemId : secondary.giveItems) { inv.getHotbar().addItemStack(new ItemStack(itemId, 1)); } }

        // Slot 2 - Shield
        if (shield != null) { for (String itemId : shield.giveItems) { inv.getArmor().addItemStack(new ItemStack(itemId, 1)); } }

        ItemStack knife = new ItemStack("Weapon_Daggers_Cobalt", 1);
        inv.getHotbar().addItemStack(knife);
    }

    public static void getLobbyLoot(Player player) {
        player.getInventory().clear();

        Inventory inv = player.getInventory();
        ItemStack actionBook = new ItemStack("actions_book", 1);

        inv.getHotbar().addItemStack(actionBook);
    }
}