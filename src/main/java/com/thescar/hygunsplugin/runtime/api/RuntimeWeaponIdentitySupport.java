package com.thescar.hygunsplugin.runtime.api;

import com.thescar.hygunsplugin.content.registry.GunRegistry;
import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeEcs;
import com.thescar.hygunsplugin.runtime.item.core.RuntimeItemIdentity;
import com.thescar.hygunsplugin.runtime.item.core.RuntimeItemRef;
import com.thescar.hygunsplugin.support.hytale.HeldItemSync;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class RuntimeWeaponIdentitySupport {
	private RuntimeWeaponIdentitySupport() {
	}

	public static boolean isTrackableWeapon(@Nullable ItemStack itemStack) {
		return itemStack != null && !itemStack.isEmpty() && GunRegistry.getSettings(itemStack.getItemId()) != null;
	}

	@Nonnull
	public static ItemStack checkAndEnsureRuntimeItemId(@Nonnull InteractionContext interactionContext, @Nonnull Ref<EntityStore> ref,
	                                                    @Nonnull ItemStack itemStack) {
		if (!isTrackableWeapon(itemStack)) {
			return itemStack;
		}

		RuntimeItemIdentity.Assignment assignment = ItemRuntimeEcs.ensureTracked(itemStack);
		if (assignment.created()) {
			HeldItemSync.updateHeldItem(interactionContext, ref, itemStack, assignment.stack());
			return assignment.stack();
		}

		return itemStack;
	}

	@Nonnull
	public static RuntimeItemIdentity.Assignment ensureTrackableWeapon(@Nonnull ItemStack itemStack) {
		if (!isTrackableWeapon(itemStack)) {
			RuntimeItemRef existing = RuntimeItemIdentity.resolve(itemStack);
			if (existing != null) {
				return new RuntimeItemIdentity.Assignment(existing, itemStack, false);
			}

			throw new IllegalArgumentException("Item is not a trackable weapon: " + itemStack.getItemId());
		}

		return ItemRuntimeEcs.ensureTracked(itemStack);
	}
}
