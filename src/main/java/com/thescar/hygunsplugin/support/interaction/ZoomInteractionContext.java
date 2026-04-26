package com.thescar.hygunsplugin.support.interaction;

import com.thescar.hygunsplugin.debug.DebugLogger;
import com.thescar.hygunsplugin.runtime.components.ZoomStateComponent;
import com.thescar.hygunsplugin.support.hytale.PlayerInventoryAccess;
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

public final class ZoomInteractionContext {
	private final @Nonnull CommandBuffer<EntityStore> commandBuffer;
	private final @Nonnull Ref<EntityStore> entityRef;
	private final @Nonnull Player player;
	private final @Nonnull PlayerRef playerRef;

	private ZoomInteractionContext(@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Ref<EntityStore> entityRef,
	                               @Nonnull Player player, @Nonnull PlayerRef playerRef) {
		this.commandBuffer = commandBuffer;
		this.entityRef = entityRef;
		this.player = player;
		this.playerRef = playerRef;
	}

	@Nullable
	public static ZoomInteractionContext resolve(@Nonnull InteractionContext interactionContext) {
		CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
		Ref<EntityStore> entityRef = interactionContext.getEntity();
		if (commandBuffer == null || entityRef == null) {
			DebugLogger.debug("ZoomInteractionContext", "Resolve failed: missing commandBuffer or entity ref");
			return null;
		}

		Player player = commandBuffer.getComponent(entityRef, Player.getComponentType());
		PlayerRef playerRef = PlayerRefAccess.getValid(entityRef, commandBuffer);
		if (player == null || playerRef == null) {
			DebugLogger.debug(
				"ZoomInteractionContext", () -> "Resolve failed: player=" + (player != null)
					+ ", playerRef=" + (playerRef != null)
			);
			return null;
		}

		DebugLogger.debug("ZoomInteractionContext", () -> "Resolved playerRef=" + playerRef.getUuid());
		return new ZoomInteractionContext(commandBuffer, entityRef, player, playerRef);
	}

	public @Nonnull CommandBuffer<EntityStore> commandBuffer() {
		return this.commandBuffer;
	}

	public @Nonnull Ref<EntityStore> entityRef() {
		return this.entityRef;
	}

	public @Nonnull Player player() {
		return this.player;
	}

	public @Nonnull PlayerRef playerRef() {
		return this.playerRef;
	}

	public @Nullable ItemStack heldItem() {
		return PlayerInventoryAccess.getItemInHand(this.player);
	}

	public @Nullable String heldItemId() {
		ItemStack held = heldItem();
		return held != null
		       ? held.getItemId()
		       : null;
	}

	public @Nullable ZoomStateComponent zoomState() {
		return this.commandBuffer.getComponent(this.entityRef, ZoomStateComponent.getComponentType());
	}
}
