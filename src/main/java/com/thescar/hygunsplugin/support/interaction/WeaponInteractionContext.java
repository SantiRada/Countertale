package com.thescar.hygunsplugin.support.interaction;

import com.thescar.hygunsplugin.debug.DebugLogger;
import com.thescar.hygunsplugin.runtime.api.RuntimeWeaponIdentitySupport;
import com.thescar.hygunsplugin.runtime.api.RuntimeWeaponStateAccess;
import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeEcs;
import com.thescar.hygunsplugin.support.hytale.HeldItemSync;
import com.thescar.hygunsplugin.support.hytale.PlayerRefAccess;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class WeaponInteractionContext {
	private final @Nonnull InteractionContext interactionContext;
	private final @Nonnull CommandBuffer<EntityStore> commandBuffer;
	private final @Nonnull Ref<EntityStore> ref;
	private final @Nonnull Player player;
	private @Nonnull ItemStack itemStack;

	private WeaponInteractionContext(@Nonnull InteractionContext interactionContext, @Nonnull CommandBuffer<EntityStore> commandBuffer,
	                                 @Nonnull Ref<EntityStore> ref, @Nonnull Player player, @Nonnull ItemStack itemStack) {
		this.interactionContext = interactionContext;
		this.commandBuffer = commandBuffer;
		this.ref = ref;
		this.player = player;
		this.itemStack = itemStack;
	}

	@Nullable
	public static WeaponInteractionContext resolve(@Nonnull InteractionContext interactionContext) {
		CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
		Ref<EntityStore> ref = interactionContext.getEntity();
		if (commandBuffer == null || ref == null) {
			DebugLogger.debug("WeaponInteractionContext", "Resolve failed: missing commandBuffer or entity ref");
			return null;
		}

		Player player = commandBuffer.getComponent(ref, Player.getComponentType());
		ItemStack itemStack = interactionContext.getHeldItem();
		if (player == null || itemStack == null) {
			DebugLogger.debug(
				"WeaponInteractionContext", () -> "Resolve failed: player=" + (player != null)
					+ ", heldItem=" + (itemStack != null
					                   ? itemStack.getItemId()
					                   : "null")
			);
			return null;
		}

		ItemRuntimeEcs.rememberWorldThread(player.getWorld());
		DebugLogger.debug("WeaponInteractionContext", () -> "Resolved heldItem=" + itemStack.getItemId());
		return new WeaponInteractionContext(interactionContext, commandBuffer, ref, player, itemStack);
	}

	public @Nonnull InteractionContext interactionContext() {
		return this.interactionContext;
	}

	public @Nonnull CommandBuffer<EntityStore> commandBuffer() {
		return this.commandBuffer;
	}

	public @Nonnull Ref<EntityStore> ref() {
		return this.ref;
	}

	public @Nonnull Player player() {
		return this.player;
	}

	public @Nullable PlayerRef playerRef() {
		return PlayerRefAccess.getValid(this.ref, this.commandBuffer);
	}

	public @Nonnull ItemStack itemStack() {
		return this.itemStack;
	}

	public @Nonnull ItemStack ensureTrackedHeldWeapon() {
		this.itemStack = RuntimeWeaponIdentitySupport.checkAndEnsureRuntimeItemId(this.interactionContext, this.ref, this.itemStack);
		DebugLogger.debug("WeaponInteractionContext", () -> "Tracked heldItem=" + this.itemStack.getItemId());
		return this.itemStack;
	}

	public @Nonnull RuntimeWeaponStateAccess.AmmoState ensureAmmoForConfiguredGun() {
		RuntimeWeaponStateAccess.AmmoState state = RuntimeWeaponStateAccess.ensureAmmoForConfiguredGun(this.itemStack);
		applyUpdatedStack(state.created(), state.stack());
		return state;
	}

	public @Nonnull RuntimeWeaponStateAccess.AmmoState ensureAmmoForInteraction(int maxAmmo) {
		RuntimeWeaponStateAccess.AmmoState state = RuntimeWeaponStateAccess.ensureAmmoForInteraction(this.itemStack, maxAmmo);
		applyUpdatedStack(state.created(), state.stack());
		return state;
	}

	public @Nonnull RuntimeWeaponStateAccess.AmmoState ensureAmmoForReloadApply(int maxAmmo) {
		RuntimeWeaponStateAccess.AmmoState state = RuntimeWeaponStateAccess.ensureAmmoForReloadApply(this.itemStack, maxAmmo);
		applyUpdatedStack(state.created(), state.stack());
		return state;
	}

	private void applyUpdatedStack(boolean created, @Nonnull ItemStack updatedStack) {
		if (!created) {
			return;
		}

		DebugLogger.debug("WeaponInteractionContext", () -> "Held item updated after ensure: " + updatedStack.getItemId());
		HeldItemSync.updateHeldItem(this.interactionContext, this.ref, this.itemStack, updatedStack);
		this.itemStack = updatedStack;
	}
}
