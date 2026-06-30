package Tenzinn.Core;

import Tenzinn.Core.UI.GameHUD;
import Tenzinn.Core.Cases.ArmorySkinAssets;
import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.Objects.WeaponStats;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;

import java.util.ArrayList;

public class LootManager {

    private static final WeaponStats STARTER_SECONDARY = new WeaponStats(
            "Desert Eagle",
            "Secondary",
            "Weapon",
            "Single",
            new ArrayList<>(java.util.List.of("Weapon_Handgun")),
            "DesertEagle",
            7,
            100
    );
    private static final WeaponStats STARTER_SHIELD = new WeaponStats(
            "Kevlar",
            "shield",
            "",
            "",
            new ArrayList<>(java.util.List.of(
                    "Armor_Iron_Legs",
                    "Armor_Iron_Hands",
                    "Armor_Iron_Head",
                    "Armor_Iron_Chest"
            )),
            "Kevlar",
            1,
            100
    );

    public static ArrayList<WeaponStats> getGameLoot(Player player) {
        PlayerRef playerRef = getPlayerRef(player);
        if (playerRef == null) return null;

        ArrayList<WeaponStats> loot = RefactorTool.getLoot(playerRef);
        if (loot == null) return getStarterKit();
        if (loot.isEmpty()) return getStarterKit();

        return loot;
    }

    public static ArrayList<WeaponStats> getStarterKit() {
        ArrayList<WeaponStats> list = new ArrayList<>();

        list.add(cloneWeapon(STARTER_SECONDARY));
        list.add(cloneWeapon(STARTER_SHIELD));

        return list;
    }

    public static void giveLoot(Player player, ArrayList<WeaponStats> loot) {
        PlayerRef playerRef = getPlayerRef(player);
        if (playerRef == null) return;
        if (playerRef.getWorldUuid() == null) return;

        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world == null) return;

        world.execute(() -> {
            Ref<EntityStore> ref = playerRef.getReference();
            Store<EntityStore> store = ref.getStore();
            InventoryComponent.Hotbar hotbar = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
            InventoryComponent.Storage storage = store.getComponent(ref, InventoryComponent.Storage.getComponentType());
            InventoryComponent.Armor armor = store.getComponent(ref, InventoryComponent.Armor.getComponentType());

            ItemContainer hotbarItems = hotbar != null ? hotbar.getInventory() : null;
            ItemContainer storageItems = storage != null ? storage.getInventory() : null;
            ItemContainer armorItems = armor != null ? armor.getInventory() : null;

            if (hotbarItems != null) hotbarItems.clear();
            if (storageItems != null) storageItems.clear();
            if (armorItems != null) armorItems.clear();

            ItemStack bullets = new ItemStack("Weapon_Arrow_Crude", 3600);
            if (storageItems != null) storageItems.addItemStack(bullets);

            WeaponStats primary = null;
            WeaponStats secondary = null;
            WeaponStats shield = null;

            for (WeaponStats weapon : loot) {
                if (weapon == null || weapon.typeWeapon == null) continue;
                if (primary == null && weapon.typeWeapon.equalsIgnoreCase("primary")) primary = weapon;
                else if (secondary == null && weapon.typeWeapon.equalsIgnoreCase("secondary")) secondary = weapon;
                else if (shield == null && weapon.typeWeapon.equalsIgnoreCase("shield")) shield = weapon;
            }

            if (primary != null)
                for (String itemId : primary.giveItems)
                    if (hotbarItems != null) hotbarItems.addItemStack(new ItemStack(resolveSkinnedItemId(playerRef, primary, itemId), 1));

            if (secondary != null)
                for (String itemId : secondary.giveItems)
                    if (hotbarItems != null) hotbarItems.setItemStackForSlot((short) 1, new ItemStack(resolveSkinnedItemId(playerRef, secondary, itemId), 1));

            if (shield != null)
                for (String itemId : shield.giveItems)
                    if (armorItems != null) armorItems.addItemStack(new ItemStack(itemId, 1));

            if (hotbarItems != null) hotbarItems.setItemStackForSlot((short) 2, new ItemStack("Weapon_Daggers_Cobalt", 1));

            // Stats y HUD en el mismo execute, sin schedule separado
            ComponentType<EntityStore, EntityStatMap> statMapType =
                    EntityStatsModule.get().getEntityStatMapComponentType();
            EntityStatMap statMap = store.getComponent(ref, statMapType);
            if (statMap != null)
                statMap.maximizeStatValue(DefaultEntityStatTypes.getHealth());

            CustomUIHud customHUD = player.getHudManager().getCustomHud(playerRef.getUuid().toString());
            if (customHUD instanceof GameHUD newHud) {
                int activeSlot = hotbar != null ? hotbar.getActiveSlot() : 0;
                newHud.setWeapons(activeSlot + 1);
                newHud.setShield();
            }
        });
    }

    public static void getLobbyLoot(Player player) {
        Ref<EntityStore> ref = player.getReference();
        if (ref == null) return;

        Store<EntityStore> store = ref.getStore();
        InventoryComponent.Hotbar hotbar = store.getComponent(ref, InventoryComponent.Hotbar.getComponentType());
        InventoryComponent.Storage storage = store.getComponent(ref, InventoryComponent.Storage.getComponentType());
        InventoryComponent.Armor armor = store.getComponent(ref, InventoryComponent.Armor.getComponentType());

        ItemContainer hotbarItems = hotbar != null ? hotbar.getInventory() : null;
        if (hotbarItems == null) return;

        hotbarItems.clear();
        if (storage != null) storage.getInventory().clear();
        if (armor != null) armor.getInventory().clear();

        hotbarItems.setItemStackForSlot((short) 0, new ItemStack("actions_book", 1));
        hotbarItems.setItemStackForSlot((short) 1, new ItemStack("inventory_book", 1));
    }

    private static WeaponStats cloneWeapon(WeaponStats weapon) {
        return new WeaponStats(
                weapon.nameWeapon,
                weapon.typeWeapon,
                weapon.crossType,
                weapon.firemode,
                weapon.giveItems,
                weapon.image,
                weapon.pos,
                weapon.pricing
        );
    }

    private static String resolveSkinnedItemId(PlayerRef playerRef, WeaponStats weapon, String baseItemId) {
        String weaponId = ArmorySkinAssets.weaponIdFromBaseItem(baseItemId, weapon.image);
        return ArmorySkinAssets.resolveItemId(playerRef.getUuid(), baseItemId, weaponId);
    }

    private static PlayerRef getPlayerRef(Player player) {
        Ref<EntityStore> ref = player.getReference();
        if (ref == null) return null;

        Store<EntityStore> store = ref.getStore();
        UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
        return uuidComponent != null ? Universe.get().getPlayer(uuidComponent.getUuid()) : null;
    }
}
