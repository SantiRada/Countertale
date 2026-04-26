package com.thescar.hygunsplugin.ui.pages;

import com.thescar.hygunsplugin.content.ammo.AmmoDefinition;
import com.thescar.hygunsplugin.content.registry.AmmoRegistry;
import com.thescar.hygunsplugin.content.registry.GunRegistry;
import com.thescar.hygunsplugin.content.settings.GunSettings;
import com.thescar.hygunsplugin.content.settings.WeaponAmmoSettings;
import com.thescar.hygunsplugin.gameplay.ammo.AmmoInventoryAccess;
import com.thescar.hygunsplugin.runtime.api.RuntimeWeaponStateAccess;
import com.thescar.hygunsplugin.runtime.components.AmmoDataComponent;
import com.thescar.hygunsplugin.runtime.persistence.RuntimeWeaponPersistence;
import com.thescar.hygunsplugin.support.hytale.SignatureStatsGuard;
import com.thescar.hygunsplugin.ui.hud.HudCoordinator;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceInteraction;
import com.hypixel.hytale.server.core.inventory.ItemContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class SelectAmmoInteraction extends ChoiceInteraction {
	private final ItemContext heldItemContext;
	private final String ammoItemId;

	public SelectAmmoInteraction(ItemContext heldItemContext, String ammoItemId) {
		this.heldItemContext = heldItemContext;
		this.ammoItemId = ammoItemId;
	}

	@Override
	public void run(Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef) {
		Player player = store.getComponent(ref, Player.getComponentType());
		if (player == null) {
			return;
		}

		ItemStack heldItem = this.heldItemContext.getContainer().getItemStack(this.heldItemContext.getSlot());
		if (heldItem == null || heldItem.isEmpty()) {
			return;
		}

		GunSettings gunSettings = GunRegistry.getSettings(heldItem.getItemId());
		WeaponAmmoSettings weaponAmmo = gunSettings != null
		                                ? gunSettings.ammo()
		                                : null;
		RuntimeWeaponStateAccess.AmmoState runtimeAmmo = RuntimeWeaponStateAccess.ensureAmmoForReloadApply(
			heldItem,
			resolveMaxAmmo(weaponAmmo)
		);
		ItemStack updated = runtimeAmmo.stack();
		AmmoDataComponent ammo = runtimeAmmo.ammo();
		int loadedAmmoCount = ammo.ammo();
		int maxAmmo = ammo.maxAmmo();
		String previousLoadedAmmoItemId = ammo.loadedAmmoItemId();
		CombinedItemContainer combined = AmmoInventoryAccess.getAmmoContainer(player);
		if (combined == null) {
			return;
		}

		ammo.setSelectedAmmoItemId(this.ammoItemId);
		ammo.markDirty();
		boolean switchingLoadedAmmo = previousLoadedAmmoItemId != null && !previousLoadedAmmoItemId.equalsIgnoreCase(this.ammoItemId)
			&& loadedAmmoCount > 0;
		if (switchingLoadedAmmo || loadedAmmoCount <= 0) {
			AmmoDefinition selectedAmmo = AmmoRegistry.getAmmo(this.ammoItemId);
			String selectedAmmoIcon = selectedAmmo != null && selectedAmmo.settings() != null
			                          ? selectedAmmo.settings().icon()
			                          : null;
			int retainedAmmoCount = switchingLoadedAmmo
			                        ? 0
			                        : loadedAmmoCount;
			int need = Math.max(0, maxAmmo - retainedAmmoCount);
			if (need > 0) {
				int available = AmmoInventoryAccess.countAmmo(combined, this.ammoItemId);
				int ammoToLoad = Math.min(need, Math.max(0, available));
				int removed = ammoToLoad > 0
				              ? AmmoInventoryAccess.removeAmmo(combined, this.ammoItemId, ammoToLoad)
				              : 0;
				if (removed > 0) {
					if (switchingLoadedAmmo) {
						SimpleItemContainer.addOrDropItemStack(
							store, ref, combined,
							new ItemStack(previousLoadedAmmoItemId).withQuantity(loadedAmmoCount)
						);
					}

					ammo.setAmmo(retainedAmmoCount + removed);
					ammo.setLoadedAmmoItemId(this.ammoItemId);
					ammo.setLoadedAmmoIcon(selectedAmmoIcon);
					ammo.markDirty();
				} else if (switchingLoadedAmmo) {
					ammo.setLoadedAmmoItemId(previousLoadedAmmoItemId);
				}

			} else {
				if (switchingLoadedAmmo) {
					SimpleItemContainer.addOrDropItemStack(
						store, ref, combined,
						new ItemStack(previousLoadedAmmoItemId).withQuantity(loadedAmmoCount)
					);
					ammo.setAmmo(0);
				}

				ammo.setLoadedAmmoItemId(this.ammoItemId);
				ammo.setLoadedAmmoIcon(selectedAmmoIcon);
				ammo.markDirty();
			}
		}

		ItemStack persisted = RuntimeWeaponPersistence.writeAmmo(updated, ammo);
		if (persisted != heldItem) {
			this.heldItemContext
				.getContainer()
				.replaceItemStackInSlot(this.heldItemContext.getSlot(), heldItem, persisted);
			SignatureStatsGuard.preventQueuedReset(ref);
			updated = persisted;
		}

		ammo.clearDirty();
		PageManager pageManager = player.getPageManager();
		pageManager.setPage(ref, store, Page.None);
		HudCoordinator.updateAmmo(playerRef, updated);
	}

	private int resolveMaxAmmo(WeaponAmmoSettings ammoSettings) {
		Integer capacity = ammoSettings != null
		                   ? ammoSettings.capacity()
		                   : null;
		return capacity != null && capacity > 0
		       ? capacity
		       : 1;
	}
}
