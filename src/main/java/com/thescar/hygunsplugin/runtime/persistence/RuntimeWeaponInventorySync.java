package com.thescar.hygunsplugin.runtime.persistence;

import com.thescar.hygunsplugin.content.ammo.AmmoContentApi;
import com.thescar.hygunsplugin.content.ammo.AmmoDefinition;
import com.thescar.hygunsplugin.content.settings.GunSettings;
import com.thescar.hygunsplugin.content.settings.WeaponAmmoSettings;
import com.thescar.hygunsplugin.content.weapon.WeaponContentApi;
import com.thescar.hygunsplugin.gameplay.ammo.AmmoService;
import com.thescar.hygunsplugin.runtime.api.RuntimeWeaponStateAccess;
import com.thescar.hygunsplugin.runtime.components.AmmoDataComponent;
import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeEcs;
import com.thescar.hygunsplugin.runtime.item.core.RuntimeItemIdentity;
import com.thescar.hygunsplugin.support.hytale.PlayerInventoryAccess;
import com.thescar.hygunsplugin.support.hytale.SignatureStatsGuard;
import com.thescar.hygunsplugin.support.text.ValueUtils;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class RuntimeWeaponInventorySync {
	private RuntimeWeaponInventorySync() {
	}

	public static void ensureTrackedInventory(@Nullable Player player) {
		if (player == null) {
			return;
		}

		ItemRuntimeEcs.rememberWorldThread(player.getWorld());
		ensureTrackedInventoryInternal(player);
	}

	public static void ensureTrackedInventory(@Nullable World world, @Nullable Player player) {
		if (player == null || world == null) {
			return;
		}

		ItemRuntimeEcs.rememberWorldThread(world);
		ensureTrackedInventoryInternal(player);
	}

	private static void ensureTrackedInventoryInternal(@Nonnull Player player) {
		ensureTrackedSection(player, PlayerInventoryAccess.getHotbar(player));
		ensureTrackedSection(player, PlayerInventoryAccess.getUtility(player));
		ensureTrackedSection(player, PlayerInventoryAccess.getStorage(player));
		ensureTrackedSection(player, PlayerInventoryAccess.getBackpack(player));
	}

	private static void ensureTrackedSection(@Nullable Player player, @Nullable ItemContainer container) {
		if (player == null || container == null) {
			return;
		}

		short capacity = container.getCapacity();
		for (short slot = 0; slot < capacity; slot++) {
			ItemStack stack = container.getItemStack(slot);
			GunSettings settings;
			if (stack == null || stack.isEmpty() || (settings = WeaponContentApi.getSettings(stack.getItemId())) == null) {
				continue;
			}

			RuntimeItemIdentity.Assignment assignment = ItemRuntimeEcs.ensureTracked(stack);
			RuntimeWeaponStateAccess.AmmoState ammoState = RuntimeWeaponStateAccess.ensureAmmoForConfiguredGun(
				assignment.stack(),
				settings
			);
			boolean needsInitialAmmoSetup = com.thescar.hygunsplugin.runtime.api.WeaponRuntimeApi
				.readPersistedAmmo(assignment.stack()) == null;
			if (!needsInitialAmmoSetup) {
				if (assignment.created()) {
					container.replaceItemStackInSlot(slot, stack, assignment.stack());
					SignatureStatsGuard.preventQueuedReset(player.getReference());
				}

				continue;
			}

			ItemStack updated = initializeNewWeaponAmmo(player, container, slot, stack, ammoState, settings);
			if (updated == null && assignment.created()) {
				container.replaceItemStackInSlot(slot, stack, assignment.stack());
				SignatureStatsGuard.preventQueuedReset(player.getReference());
			}
		}
	}

	@Nullable
	private static ItemStack initializeNewWeaponAmmo(@Nullable Player player, @Nullable ItemContainer container, short slot,
	                                                 @Nullable ItemStack previousStack, @Nullable RuntimeWeaponStateAccess.AmmoState ammoState, @Nullable GunSettings settings) {
		if (player == null || container == null || previousStack == null || ammoState == null || settings == null) {
			return null;
		}

		WeaponAmmoSettings weaponAmmo = settings.ammo();
		if (weaponAmmo == null) {
			return null;
		}

		AmmoDataComponent ammo = ammoState.ammo();
		CombinedItemContainer combined = AmmoService.getAmmoContainer(player);
		if (combined == null) {
			return null;
		}

		String ammoItemId = resolveInitialAmmoItemId(weaponAmmo, combined);
		if (ammoItemId != null) {
			int available = AmmoService.countAmmo(combined, ammoItemId);
			int loadRequest = Math.min(ammo.maxAmmo(), Math.max(0, available));
			int loaded = AmmoService.removeAmmo(combined, ammoItemId, loadRequest);
			if (loaded > 0) {
				ammo.setSelectedAmmoItemId(ammoItemId);
				ammo.setLoadedAmmoItemId(ammoItemId);
				ammo.setLoadedAmmoIcon(resolveAmmoIcon(ammoItemId));
				ammo.setAmmo(loaded);
			}
		}

		return RuntimeWeaponMetadataCommit.commitContainerAmmo(
			player.getReference(), container, slot, previousStack, ammoState.stack(),
			ammo
		);
	}

	@Nullable
	private static String resolveInitialAmmoItemId(@Nullable WeaponAmmoSettings weaponAmmo, @Nullable ItemContainer itemContainer) {
		if (weaponAmmo == null || itemContainer == null) {
			return null;
		}

		String exactItemId = ValueUtils.Checks.nonBlankOrNull(weaponAmmo.itemId());
		if (exactItemId != null) {
			return AmmoService.countAmmo(itemContainer, exactItemId) > 0
			       ? exactItemId
			       : null;
		}

		return AmmoContentApi.resolveDefaultAmmoItemId(AmmoService.collectCompatibleAmmo(itemContainer, weaponAmmo));
	}

	@Nullable
	private static String resolveAmmoIcon(@Nullable String ammoItemId) {
		AmmoDefinition ammo = AmmoContentApi.getAmmo(ammoItemId);
		if (ammo == null || ammo.settings() == null) {
			return null;
		}

		return ValueUtils.Checks.nonBlankOrNull(ammo.settings().icon());
	}
}
