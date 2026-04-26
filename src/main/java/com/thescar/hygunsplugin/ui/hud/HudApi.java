package com.thescar.hygunsplugin.ui.hud;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nullable;

public final class HudApi {
	private HudApi() {
	}

	public static void updateAmmo(@Nullable PlayerRef playerRef, @Nullable ItemStack itemStack) {
		if (playerRef == null || itemStack == null) {
			if (playerRef != null) {
				HudCoordinator.hideAmmo(playerRef);
			}

			return;
		}

		HudCoordinator.updateAmmo(playerRef, itemStack);
	}

	public static void hideAmmo(@Nullable PlayerRef playerRef) {
		HudCoordinator.hideAmmo(playerRef);
	}

	public static boolean showScope(@Nullable PlayerRef playerRef, @Nullable String overlayTexturePath) {
		return HudCoordinator.showScope(playerRef, overlayTexturePath);
	}

	public static void hideScope(@Nullable PlayerRef playerRef) {
		HudCoordinator.hideScope(playerRef);
	}

	public static boolean isAmmoVisible(@Nullable PlayerRef playerRef) {
		return HudCoordinator.isAmmoVisible(playerRef);
	}

	public static void attachPlayer(@Nullable PlayerRef playerRef, @Nullable Player player) {
		if (playerRef != null && player != null) {
			HudCoordinator.attachPlayer(playerRef, player);
		}
	}
}
