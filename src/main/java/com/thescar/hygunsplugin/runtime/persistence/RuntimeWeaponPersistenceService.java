package com.thescar.hygunsplugin.runtime.persistence;

import com.thescar.hygunsplugin.debug.DebugLogger;
import com.thescar.hygunsplugin.support.hytale.PlayerRefAccess;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class RuntimeWeaponPersistenceService {
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	private static final RuntimeWeaponPersistenceService INSTANCE = new RuntimeWeaponPersistenceService();

	private RuntimeWeaponPersistenceService() {
	}

	@Nonnull
	public static RuntimeWeaponPersistenceService get() {
		return INSTANCE;
	}

	private static @Nullable UUID resolvePlayerUuid(@Nonnull Player player) {
		var ref = player.getReference();
		if (ref == null || ref.getStore() == null) {
			return null;
		}

		var playerRef = PlayerRefAccess.getValid(ref, ref.getStore());
		return playerRef != null
		       ? playerRef.getUuid()
		       : null;
	}

	public void shutdown() {
		forceSyncAll();
	}

	public void trackPlayer(@Nullable Player player) {
		// Dirty sync is handled by the item runtime store tick flow.
	}

	public void forceSyncAndUntrack(@Nullable PlayerRef playerRef) {
		if (playerRef == null) {
			return;
		}

		UUID worldUuid = playerRef.getWorldUuid();
		if (worldUuid == null) {
			return;
		}

		World world = Universe.get().getWorld(worldUuid);
		if (world == null) {
			return;
		}

		CountDownLatch latch = new CountDownLatch(1);
		try {
			world.execute(() -> {
				try {
					forceSyncAndUntrackOnWorldThread(playerRef);
				} finally {
					latch.countDown();
				}

			});
			latch.await(2L, TimeUnit.SECONDS);
		} catch (Exception e) {
			LOGGER.atWarning().log("Runtime weapon metadata sync failed: %s", e);
		}
	}

	public void forceSyncAndUntrackOnWorldThread(@Nullable PlayerRef playerRef) {
		if (playerRef == null) {
			return;
		}

		Player player = playerRef.isValid()
		                ? PlayerRefAccess.getComponent(playerRef, Player.getComponentType())
		                : null;
		syncPlayerOnWorldThread(player, true);
	}

	public void forceSyncPlayerOnWorldThread(@Nullable Player player) {
		syncPlayerOnWorldThread(player, true);
	}

	public void forceSyncPlayerOnWorldThread(@Nullable World world, @Nullable Player player) {
		syncPlayerOnWorldThread(world, player, true);
	}

	public void forceSyncAll() {
		for (PlayerRef playerRef : Universe.get().getPlayers()) {
			if (playerRef == null || !playerRef.isValid()) {
				continue;
			}

			forceSyncAndUntrack(playerRef);
		}
	}

	private void scheduleSyncPlayer(@Nullable Player player, boolean force, boolean waitForCompletion) {
		if (player == null) {
			return;
		}

		World world = player.getWorld();
		if (world == null) {
			return;
		}

		CountDownLatch latch = waitForCompletion
		                       ? new CountDownLatch(1)
		                       : null;
		try {
			world.execute(() -> {
				try {
					syncPlayerOnWorldThread(player, force);
				} finally {
					if (latch != null) {
						latch.countDown();
					}
				}

			});
			if (latch != null) {
				latch.await(2L, TimeUnit.SECONDS);
			}

		} catch (Exception e) {
			LOGGER.atWarning().log("Runtime weapon metadata sync failed: %s", e);
		}
	}

	private void syncPlayerOnWorldThread(@Nullable Player player, boolean force) {
		syncPlayerOnWorldThread(player != null ? player.getWorld() : null, player, force);
	}

	private void syncPlayerOnWorldThread(@Nullable World world, @Nullable Player player, boolean force) {
		if (player == null) {
			return;
		}

		if (!force && player.wasRemoved()) {
			return;
		}

		DebugLogger.debug("AmmoPersistence", () -> "Sync player inventory on world thread: player="
			+ resolvePlayerUuid(player)
			+ ", world=" + (world != null ? world.getName() : "null")
			+ ", force=" + force);
		com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeEcs.rememberWorldThread(world);
		RuntimeWeaponDirtySync.syncPlayerInventory(player, force);
	}
}
