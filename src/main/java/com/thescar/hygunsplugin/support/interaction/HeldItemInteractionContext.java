package com.thescar.hygunsplugin.support.interaction;

import com.thescar.hygunsplugin.debug.DebugLogger;
import com.thescar.hygunsplugin.runtime.api.RuntimeItemStateAccess;
import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeEcs;
import com.thescar.hygunsplugin.support.hytale.HeldItemSync;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class HeldItemInteractionContext {
	private final @Nonnull InteractionContext interactionContext;
	private final @Nonnull CommandBuffer<EntityStore> commandBuffer;
	private final @Nonnull Ref<EntityStore> ref;
	private final @Nonnull Player player;
	private @Nonnull ItemStack itemStack;

	private HeldItemInteractionContext(@Nonnull InteractionContext interactionContext, @Nonnull CommandBuffer<EntityStore> commandBuffer,
	                                   @Nonnull Ref<EntityStore> ref, @Nonnull Player player, @Nonnull ItemStack itemStack) {
		this.interactionContext = interactionContext;
		this.commandBuffer = commandBuffer;
		this.ref = ref;
		this.player = player;
		this.itemStack = itemStack;
	}

	@Nullable
	public static HeldItemInteractionContext resolve(@Nonnull InteractionContext interactionContext) {
		CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
		Ref<EntityStore> ref = interactionContext.getEntity();
		if (commandBuffer == null || ref == null) {
			DebugLogger.debug("HeldItemInteractionContext", "Resolve failed: missing commandBuffer or entity ref");
			return null;
		}

		Player player = commandBuffer.getComponent(ref, Player.getComponentType());
		ItemStack itemStack = interactionContext.getHeldItem();
		if (player == null || itemStack == null) {
			DebugLogger.debug(
				"HeldItemInteractionContext", () -> "Resolve failed: player=" + (player != null)
					+ ", heldItem=" + (itemStack != null
					                   ? itemStack.getItemId()
					                   : "null")
			);
			return null;
		}

		ItemRuntimeEcs.rememberWorldThread(player.getWorld());
		DebugLogger.debug("HeldItemInteractionContext", () -> "Resolved heldItem=" + itemStack.getItemId());
		return new HeldItemInteractionContext(interactionContext, commandBuffer, ref, player, itemStack);
	}

	public @Nonnull ItemStack itemStack() {
		return this.itemStack;
	}

	public @Nonnull RuntimeItemStateAccess.HeatState ensureHeat() {
		RuntimeItemStateAccess.HeatState state = RuntimeItemStateAccess.ensureHeat(this.itemStack);
		applyUpdatedStack(state.created(), state.stack());
		return state;
	}

	private void applyUpdatedStack(boolean created, @Nonnull ItemStack updatedStack) {
		if (!created) {
			return;
		}

		DebugLogger.debug("HeldItemInteractionContext", () -> "Held item updated after ensure: " + updatedStack.getItemId());
		HeldItemSync.updateHeldItem(this.interactionContext, this.ref, this.itemStack, updatedStack);
		this.itemStack = updatedStack;
	}
}
