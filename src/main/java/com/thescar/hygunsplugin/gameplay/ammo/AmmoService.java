package com.thescar.hygunsplugin.gameplay.ammo;

import com.thescar.hygunsplugin.content.ammo.AmmoDefinition;
import com.thescar.hygunsplugin.content.settings.WeaponAmmoSettings;

import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;

import javax.annotation.Nullable;
import java.util.Map;

public final class AmmoService {
	private AmmoService() {
	}

	@Nullable
	public static CombinedItemContainer getAmmoContainer(@Nullable Player player) {
		return AmmoInventoryAccess.getAmmoContainer(player);
	}

	@Nullable
	public static String resolveAvailableAmmoItemId(@Nullable ItemStack gunStack, @Nullable Player player) {
		return AmmoInventoryAccess.resolveAvailableAmmoItemId(gunStack, player);
	}

	public static int countAvailableAmmo(@Nullable ItemStack gunStack, @Nullable Player player) {
		return AmmoInventoryAccess.countAvailableAmmo(gunStack, player);
	}

	@Nullable
	public static ItemStack persistAutoSelectedAmmoForHeldItem(@Nullable Player player, @Nullable ItemStack heldStack) {
		return AmmoInventoryAccess.persistAutoSelectedAmmoForHeldItem(player, heldStack);
	}

	public static int removeAmmo(@Nullable Player player, @Nullable String ammoItemId, int quantity,
	                             @Nullable InteractionContext interactionContext) {
		return AmmoInventoryAccess.removeAmmo(player, ammoItemId, quantity, interactionContext);
	}

	public static int removeAmmo(@Nullable ItemContainer itemContainer, @Nullable String ammoItemId, int quantity) {
		return AmmoInventoryAccess.removeAmmo(itemContainer, ammoItemId, quantity);
	}

	public static int countAmmo(@Nullable ItemContainer itemContainer, @Nullable String ammoItemId) {
		return AmmoInventoryAccess.countAmmo(itemContainer, ammoItemId);
	}

	public static Map<String, AmmoDefinition> collectCompatibleAmmo(@Nullable ItemContainer itemContainer,
	                                                                @Nullable WeaponAmmoSettings weaponAmmo) {
		return AmmoInventoryAccess.collectCompatibleAmmo(itemContainer, weaponAmmo);
	}

	public static Object2IntLinkedOpenHashMap<String> collectCompatibleAmmoQuantities(@Nullable ItemContainer itemContainer,
	                                                                                  @Nullable WeaponAmmoSettings weaponAmmo) {
		return AmmoInventoryAccess.collectCompatibleAmmoQuantities(itemContainer, weaponAmmo);
	}

	@Nullable
	public static String resolvePreferredAmmoItemId(@Nullable ItemStack gunStack, @Nullable WeaponAmmoSettings weaponAmmo,
	                                                @Nullable ItemContainer itemContainer) {
		return AmmoInventoryAccess.resolvePreferredAmmoItemId(gunStack, weaponAmmo, itemContainer);
	}
}
