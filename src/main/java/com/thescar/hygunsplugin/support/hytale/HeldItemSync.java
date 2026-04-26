package com.thescar.hygunsplugin.support.hytale;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public final class HeldItemSync {
	private HeldItemSync() {
	}

	public static void updateHeldItem(@Nonnull InteractionContext interactionContext, @Nonnull Ref<EntityStore> ref,
	                                  @Nonnull ItemStack previous, @Nonnull ItemStack updated) {
		if (updated == previous) {
			return;
		}

		interactionContext.setHeldItem(updated);
		ItemContainer itemContainer = interactionContext.getHeldItemContainer();
		if (itemContainer == null) {
			return;
		}

		ItemContext itemContext = new ItemContext(itemContainer, interactionContext.getHeldItemSlot(), updated);
		itemContainer.replaceItemStackInSlot(itemContext.getSlot(), previous, updated);
		SignatureStatsGuard.preventQueuedReset(ref);
	}
}
