package com.thescar.hygunsplugin.runtime.persistence;

import com.thescar.hygunsplugin.runtime.components.AmmoDataComponent;
import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeEcs;
import com.thescar.hygunsplugin.runtime.item.core.RuntimeItemIdentity;
import com.thescar.hygunsplugin.runtime.item.core.RuntimeItemRef;
import com.thescar.hygunsplugin.support.hytale.PlayerInventoryAccess;
import com.thescar.hygunsplugin.support.hytale.PlayerRefAccess;
import com.thescar.hygunsplugin.support.hytale.SignatureStatsGuard;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public final class RuntimeWeaponDirtySync {
	private RuntimeWeaponDirtySync() {
	}

	public static int syncPlayerInventory(@Nullable Player player, boolean force) {
		if (player == null) {
			return 0;
		}

		int changed = 0;
		changed += syncSection(player, PlayerInventoryAccess.getHotbar(player), force);
		changed += syncSection(player, PlayerInventoryAccess.getUtility(player), force);
		changed += syncSection(player, PlayerInventoryAccess.getStorage(player), force);
		changed += syncSection(player, PlayerInventoryAccess.getBackpack(player), force);
		return changed;
	}

	public static boolean syncRuntimeWeapon(@Nonnull RuntimeItemRef runtimeRef, @Nonnull AmmoDataComponent ammo, boolean force) {
		if (!ammo.initialized() || (!force && !ammo.dirty())) {
			return false;
		}

		LocatedStack located = locate(runtimeRef);
		if (located == null) {
			return false;
		}

		ItemStack updated = RuntimeWeaponPersistence.writeAmmo(located.stack(), ammo);
		if (updated != located.stack()) {
			located.container().replaceItemStackInSlot(located.slot(), located.stack(), updated);
			SignatureStatsGuard.preventQueuedReset(located.player().getReference());
		}

		ammo.clearDirty();
		return true;
	}

	public static boolean isActivelyHeld(@Nonnull RuntimeItemRef runtimeRef) {
		String currentWorldName = ItemRuntimeEcs.currentWorldName();
		for (PlayerRef playerRef : Universe.get().getPlayers()) {
			if (playerRef == null || !playerRef.isValid()) {
				continue;
			}
			if (!isPlayerInCurrentWorld(playerRef, currentWorldName)) {
				continue;
			}

			Player player = PlayerRefAccess.getComponent(playerRef, Player.getComponentType());
			if (player == null || player.wasRemoved()) {
				continue;
			}

			ItemStack held = PlayerInventoryAccess.getItemInHand(player);
			RuntimeItemRef heldRef = RuntimeItemIdentity.resolve(held);
			if (runtimeRef.equals(heldRef)) {
				return true;
			}
		}

		return false;
	}

	private static int syncSection(@Nonnull Player player, @Nullable ItemContainer container, boolean force) {
		if (container == null) {
			return 0;
		}

		int changed = 0;
		short capacity = container.getCapacity();
		for (short slot = 0; slot < capacity; slot++) {
			ItemStack stack = container.getItemStack(slot);
			RuntimeItemRef ref = RuntimeItemIdentity.resolve(stack);
			if (ref == null) {
				continue;
			}

			AmmoDataComponent ammo = ItemRuntimeEcs.getComponent(ref, ItemRuntimeEcs.AMMO_DATA_TYPE);
			if (ammo == null || !ammo.initialized() || (!force && !ammo.dirty())) {
				continue;
			}

			ItemStack updated = RuntimeWeaponPersistence.writeAmmo(stack, ammo);
			if (updated != stack) {
				container.replaceItemStackInSlot(slot, stack, updated);
				SignatureStatsGuard.preventQueuedReset(player.getReference());
				changed++;
			}

			ammo.clearDirty();
		}

		return changed;
	}

	private static @Nullable LocatedStack locate(@Nonnull RuntimeItemRef runtimeRef) {
		String currentWorldName = ItemRuntimeEcs.currentWorldName();
		for (PlayerRef playerRef : Universe.get().getPlayers()) {
			if (playerRef == null || !playerRef.isValid()) {
				continue;
			}
			if (!isPlayerInCurrentWorld(playerRef, currentWorldName)) {
				continue;
			}

			Player player = PlayerRefAccess.getComponent(playerRef, Player.getComponentType());
			if (player == null || player.wasRemoved()) {
				continue;
			}

			LocatedStack located = locateInPlayer(player, runtimeRef);
			if (located != null) {
				return located;
			}
		}

		return null;
	}

	public static boolean isPlayerInCurrentWorld(@Nonnull PlayerRef playerRef) {
		return isPlayerInCurrentWorld(playerRef, ItemRuntimeEcs.currentWorldName());
	}

	private static boolean isPlayerInCurrentWorld(@Nonnull PlayerRef playerRef, @Nullable String currentWorldName) {
		if (currentWorldName == null) {
			return true;
		}

		UUID playerWorldUuid = playerRef.getWorldUuid();
		if (playerWorldUuid == null) {
			return false;
		}

		World playerWorld = Universe.get().getWorld(playerWorldUuid);
		if (playerWorld == null) {
			return false;
		}

		String playerWorldName = playerWorld.getName();
		return currentWorldName.equals(playerWorldName == null || playerWorldName.isBlank() ? "default" : playerWorldName);
	}

	private static @Nullable LocatedStack locateInPlayer(@Nonnull Player player, @Nonnull RuntimeItemRef runtimeRef) {
		LocatedStack located = locateInSection(player, PlayerInventoryAccess.getHotbar(player), runtimeRef);
		if (located != null) {
			return located;
		}

		located = locateInSection(player, PlayerInventoryAccess.getUtility(player), runtimeRef);
		if (located != null) {
			return located;
		}

		located = locateInSection(player, PlayerInventoryAccess.getStorage(player), runtimeRef);
		if (located != null) {
			return located;
		}

		return locateInSection(player, PlayerInventoryAccess.getBackpack(player), runtimeRef);
	}

	private static @Nullable LocatedStack locateInSection(@Nonnull Player player, @Nullable ItemContainer container,
	                                                      @Nonnull RuntimeItemRef runtimeRef) {
		if (container == null) {
			return null;
		}

		short capacity = container.getCapacity();
		for (short slot = 0; slot < capacity; slot++) {
			ItemStack stack = container.getItemStack(slot);
			RuntimeItemRef candidate = RuntimeItemIdentity.resolve(stack);
			if (runtimeRef.equals(candidate)) {
				return new LocatedStack(player, container, slot, stack);
			}
		}

		return null;
	}

	private record LocatedStack(
		@Nonnull Player player, @Nonnull ItemContainer container, short slot, @Nonnull ItemStack stack
	) {
	}
}
