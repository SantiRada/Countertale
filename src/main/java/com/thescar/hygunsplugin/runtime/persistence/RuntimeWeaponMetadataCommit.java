package com.thescar.hygunsplugin.runtime.persistence;

import com.thescar.hygunsplugin.runtime.components.AmmoDataComponent;
import com.thescar.hygunsplugin.support.hytale.HeldItemSync;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public final class RuntimeWeaponMetadataCommit {
	private RuntimeWeaponMetadataCommit() {
	}

	@Nonnull
	public static ItemStack commitHeldAmmo(@Nonnull InteractionContext interactionContext, @Nonnull Ref<EntityStore> ref,
	                                       @Nonnull ItemStack previousStack, @Nonnull ItemStack currentStack, @Nonnull AmmoDataComponent ammo) {
		ItemStack persisted = RuntimeWeaponPersistence.writeAmmo(currentStack, ammo);
		HeldItemSync.updateHeldItem(interactionContext, ref, previousStack, persisted);
		ammo.clearDirty();
		return persisted;
	}

	@Nonnull
	public static ItemStack commitContainerAmmo(@Nonnull Ref<EntityStore> ref, @Nonnull ItemContainer container, short slot,
	                                            @Nonnull ItemStack previousStack, @Nonnull ItemStack currentStack, @Nonnull AmmoDataComponent ammo) {
		ItemStack persisted = RuntimeWeaponPersistence.writeAmmo(currentStack, ammo);
		if (persisted != previousStack) {
			container.replaceItemStackInSlot(slot, previousStack, persisted);
			com.thescar.hygunsplugin.support.hytale.SignatureStatsGuard.preventQueuedReset(ref);
		}

		ammo.clearDirty();
		return persisted;
	}
}
