package com.thescar.hygunsplugin.ui.hud;

import com.thescar.hygunsplugin.gameplay.ammo.AmmoService;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class HudCoordinator {

	private static final ConcurrentHashMap<UUID, PlayerRef> PLAYER_REFS = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<UUID, Player> PLAYERS = new ConcurrentHashMap<>();
	private static final Set<UUID> AMMO_VISIBLE = ConcurrentHashMap.newKeySet();

	private HudCoordinator() {
	}

	@Nullable
	public static PlayerRef getPlayerRef(UUID uuid) {
		return uuid == null ? null : PLAYER_REFS.get(uuid);
	}

	@Nullable
	public static Player getPlayer(UUID uuid) {
		return uuid == null ? null : PLAYERS.get(uuid);
	}

	public static void attachPlayer(PlayerRef playerRef, Player player) {
		attachPlayer(playerRef, player, player != null ? player.getWorld() : null);
	}

	public static void attachPlayer(PlayerRef playerRef, Player player, @Nullable World world) {
		if (playerRef == null || player == null) return;

		UUID uuid = playerRef.getUuid();
		PLAYER_REFS.put(uuid, playerRef);
		PLAYERS.put(uuid, player);
	}

	public static void detachPlayer(UUID uuid) {
		if (uuid == null) return;

		Player player = PLAYERS.remove(uuid);
		PLAYER_REFS.remove(uuid);
		AMMO_VISIBLE.remove(uuid);

		CountertaleHudBridge.hideAmmo(player);
	}

	public static void shutdown() {
		for (Player player : PLAYERS.values()) {
			CountertaleHudBridge.hideAmmo(player);
		}

		PLAYER_REFS.clear();
		PLAYERS.clear();
		AMMO_VISIBLE.clear();
	}

	public static void updateAmmo(PlayerRef playerRef, ItemStack itemStack) {
		if (playerRef == null) return;

		Player player = PLAYERS.get(playerRef.getUuid());

		ItemStack effectiveStack = player != null
				? AmmoService.persistAutoSelectedAmmoForHeldItem(player, itemStack)
				: itemStack;

		updateAmmoRuntimeSafe(playerRef, player, effectiveStack);
	}

	public static void updateAmmoRuntimeSafe(@Nullable PlayerRef playerRef, @Nullable Player player, @Nullable ItemStack itemStack) {
		if (playerRef == null) return;

		if (player == null) {
			player = PLAYERS.get(playerRef.getUuid());
		}

		if (itemStack == null) {
			hideAmmo(playerRef);
			return;
		}

		AMMO_VISIBLE.add(playerRef.getUuid());
		CountertaleHudBridge.updateAmmo(playerRef, player, itemStack);
	}

	public static void hideAmmo(PlayerRef playerRef) {
		if (playerRef == null) return;

		AMMO_VISIBLE.remove(playerRef.getUuid());

		Player player = PLAYERS.get(playerRef.getUuid());
		CountertaleHudBridge.hideAmmo(player);
	}

	public static boolean isAmmoVisible(@Nullable PlayerRef playerRef) {
		return playerRef != null && AMMO_VISIBLE.contains(playerRef.getUuid());
	}

	public static boolean showScope(PlayerRef playerRef, @Nullable String overlayTexturePath) {
		// Scope camera logic can still run through ZoomManager.
		// For now, the old Hyguns scope UI is deliberately disabled.
		return false;
	}

	public static void hideScope(PlayerRef playerRef) {
		// No-op because Countertale does not yet have a scope overlay.
	}
}