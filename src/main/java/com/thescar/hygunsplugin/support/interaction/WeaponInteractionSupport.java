package com.thescar.hygunsplugin.support.interaction;

import com.thescar.hygunsplugin.debug.DebugLogger;
import com.thescar.hygunsplugin.support.hytale.PlayerRefAccess;
import com.thescar.hygunsplugin.ui.hud.HudCoordinator;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class WeaponInteractionSupport {
	private WeaponInteractionSupport() {
	}

	public static void updateAmmoHud(@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Ref<EntityStore> ref,
	                                 @Nullable ItemStack itemStack) {
		PlayerRef playerRef = PlayerRefAccess.getValid(ref, commandBuffer);
		if (playerRef == null) {
			DebugLogger.debug("WeaponInteractionSupport", "Skipped ammo HUD update: missing valid PlayerRef");
			return;
		}

		Player player = commandBuffer.getComponent(ref, Player.getComponentType());
		if (itemStack == null) {
			DebugLogger.debug("WeaponInteractionSupport", () -> "Hide ammo HUD for " + playerRef.getUuid());
			HudCoordinator.hideAmmo(playerRef);
			return;
		}

		DebugLogger.debug(
			"WeaponInteractionSupport", () -> "Update ammo HUD runtime-safe for " + playerRef.getUuid()
				+ ", heldItem=" + itemStack.getItemId()
		);
		HudCoordinator.updateAmmoRuntimeSafe(playerRef, player, itemStack);
	}
}
