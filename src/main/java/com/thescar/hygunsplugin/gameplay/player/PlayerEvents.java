package com.thescar.hygunsplugin.gameplay.player;

import com.thescar.hygunsplugin.content.migration.ItemIdVersioning;
import com.thescar.hygunsplugin.content.particles.ParticleColorVariantService;
import com.thescar.hygunsplugin.debug.DebugLogger;
import com.thescar.hygunsplugin.gameplay.reload.ReloadManager;
import com.thescar.hygunsplugin.gameplay.zoom.ZoomManager;
import com.thescar.hygunsplugin.runtime.persistence.RuntimeWeaponInventorySync;
import com.thescar.hygunsplugin.runtime.persistence.RuntimeWeaponPersistenceService;
import com.thescar.hygunsplugin.support.hytale.PlayerInventoryAccess;
import com.thescar.hygunsplugin.support.hytale.PlayerRefAccess;
import com.thescar.hygunsplugin.ui.hud.HudCoordinator;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PlayerEvents {
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	private static final long POST_JOIN_HUD_RESYNC_DELAY_MS = 500L;
	private static final long INVENTORY_READY_RETRY_DELAY_MS = 500L;
	private static final int INVENTORY_READY_MAX_RETRIES = 10;

	private static final Set<UUID> OUTDATED_PACK_WARNED_PLAYERS = ConcurrentHashMap.newKeySet();

	public static void onPlayerReady(PlayerReadyEvent event) {
		Player player = event.getPlayer();
		if (player == null) {
			return;
		}
		World world = player.getWorld();
		if (world == null) {
			return;
		}
		Ref<EntityStore> entityRef = event.getPlayerRef();
		world.execute(() -> onPlayerReadyOnWorldThread(player, entityRef));
	}

	public static void onPlayerAddedToWorld(AddPlayerToWorldEvent event) {
		if (event == null || event.getWorld() == null || event.getHolder() == null) {
			return;
		}

		Player player = event.getHolder().getComponent(Player.getComponentType());
		if (player == null || player.wasRemoved()) {
			return;
		}

		DebugLogger.debug(
			"WorldTransfer", () -> "AddPlayerToWorld: player=" + player.getDisplayName()
				+ ", world=" + event.getWorld().getName()
		);
		com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeEcs.rememberWorldThread(event.getWorld());
		RuntimeWeaponInventorySync.ensureTrackedInventory(event.getWorld(), player);
		PlayerRef playerRef = event.getHolder().getComponent(Universe.get().getPlayerRefComponentType());
		if (playerRef != null) {
			ParticleColorVariantService.rememberPlayer(playerRef);
			HudCoordinator.attachPlayer(playerRef, player, event.getWorld());
			paintHeldItem(event.getWorld(), player, playerRef);
		}
	}

	public static void onPlayerDrainedFromWorld(DrainPlayerFromWorldEvent event) {
		if (event == null || event.getWorld() == null || event.getHolder() == null) {
			return;
		}

		Player player = event.getHolder().getComponent(Player.getComponentType());
		if (player == null) {
			return;
		}

		DebugLogger.debug(
			"WorldTransfer", () -> "DrainPlayerFromWorld: player=" + player.getDisplayName()
				+ ", world=" + event.getWorld().getName()
		);
		com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeEcs.rememberWorldThread(event.getWorld());
		RuntimeWeaponPersistenceService.get().forceSyncPlayerOnWorldThread(event.getWorld(), player);
		com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeEcs.clearPlayerInventoryRuntime(player);
		ReloadManager.cancel(player, ReloadManager.CancelReason.OTHER);
	}

	private static void onPlayerReadyOnWorldThread(Player player, Ref<EntityStore> entityRef) {
		if (player == null || player.wasRemoved()) {
			return;
		}
		if (entityRef == null) {
			return;
		}
		PlayerRef playerRef = PlayerRefAccess.getValid(entityRef, entityRef.getStore());
		if (playerRef == null) {
			return;
		}
		com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeEcs.rememberWorldThread(player.getWorld());
		ParticleColorVariantService.rememberPlayer(playerRef);
		maybeWarnOutdatedPack(player, playerRef);
		// Start inventory readiness check once. It will retry by itself if inventory is
		// not ready yet.
		ensureInventoryThenMigrateAndUpdate(player, playerRef, INVENTORY_READY_MAX_RETRIES);
		// Attach is deferred until inventory is ready to avoid HUD race on initial
		// join.
	}

	private static void maybeWarnOutdatedPack(Player player, PlayerRef playerRef) {
		if (player == null || playerRef == null) {
			return;
		}
		if (!ItemIdVersioning.hasOutdatedPackWarning()) {
			return;
		}
		if (!OUTDATED_PACK_WARNED_PLAYERS.add(playerRef.getUuid())) {
			return;
		}
		String requiredVersion = ItemIdVersioning.getRequiredPackVersionWarning();
		if (requiredVersion == null || requiredVersion.isBlank()) {
			return;
		}
		player.sendMessage(Message.raw("You use outdated version of HyGuns pack.\n" + "This may work incorrectly.\n" + "Please update to "
			+ requiredVersion + " version or higher."));
	}

	public static void onPlayerLeave(PlayerDisconnectEvent event) {
		PlayerRef playerRef = event.getPlayerRef();
		if (playerRef == null) {
			return;
		}
		HudCoordinator.detachPlayer(playerRef.getUuid());
		ParticleColorVariantService.forgetPlayer(playerRef.getUuid());
		UUID worldUuid = playerRef.getWorldUuid();
		if (worldUuid == null) {
			return;
		}

		World world = Universe.get().getWorld(worldUuid);
		if (world == null) {
			return;
		}

		world.execute(() -> {
			com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeEcs.rememberWorldThread(world);
			if (!playerRef.isValid()) {
				ReloadManager.cancelByPlayerUuid(playerRef.getUuid());
				return;
			}
			Player player = PlayerRefAccess.getComponent(playerRef, Player.getComponentType());
			if (player != null && !player.wasRemoved()) {
				ZoomManager.disableZoom(player);
			}
			RuntimeWeaponPersistenceService.get().forceSyncAndUntrackOnWorldThread(playerRef);
			ReloadManager.cancelByPlayerUuid(playerRef.getUuid());
		});
	}

	private static void paintHeldItem(Player player, PlayerRef playerRef) {
		paintHeldItem(
			player != null
			? player.getWorld()
			: null, player, playerRef
		);
	}

	private static void paintHeldItem(@Nullable World world, Player player, PlayerRef playerRef) {
		if (player == null || playerRef == null) {
			return;
		}
		RuntimeWeaponInventorySync.ensureTrackedInventory(world, player);
		ItemStack held = PlayerInventoryAccess.getItemInHand(player);
		// On initial join the held slot can be transiently null while client HUDs are
		// still initializing.
		// Hiding here may replace current custom HUD with EmptyHud and break
		// first-frame UI composition.
		if (held == null) {
			return;
		}
		HudCoordinator.updateAmmo(playerRef, held);
	}

	private static void ensureInventoryThenMigrateAndUpdate(Player player, PlayerRef playerRef, int retriesLeft) {
		if (player == null || playerRef == null) {
			return;
		}
		if (player.wasRemoved()) {
			return;
		}
		if (PlayerInventoryAccess.getReference(player) == null) {
			if (retriesLeft <= 0) {
				// Stop delaying after max retries.
				// No further scheduling.
				LOGGER.atWarning().log(
					"Inventory readiness retry failed for %s: %s times", playerRef.getUuid(),
					INVENTORY_READY_MAX_RETRIES
				);
				return;
			}

			HytaleServer.SCHEDULED_EXECUTOR.schedule(
				() -> {
					try {
						if (player.wasRemoved()) {
							return;
						}
						if (player.getWorld() == null) {
							return;
						}
						player
							.getWorld()
							.execute(() -> ensureInventoryThenMigrateAndUpdate(player, playerRef, retriesLeft - 1));
					} catch (Exception t) {
						LOGGER.atWarning().log("Inventory readiness retry failed for %s: %s", playerRef.getUuid(), t);
					}

				}, INVENTORY_READY_RETRY_DELAY_MS, TimeUnit.MILLISECONDS
			);
			return;
		}

		int migratedStacks = ItemIdVersioning.migratePlayerInventory(player);
		if (migratedStacks > 0) {
			LOGGER
				.atInfo()
				.log("Migrated %d inventory stack(s) to latest weapon ids for player %s", migratedStacks, playerRef.getUuid());
		}

		RuntimeWeaponInventorySync.ensureTrackedInventory(player);

		boolean alreadyAttached =
				HudCoordinator.getPlayerRef(playerRef.getUuid()) != null
						&& HudCoordinator.getPlayer(playerRef.getUuid()) != null;

		if (!alreadyAttached) {
			HudCoordinator.attachPlayer(playerRef, player);
			schedulePostJoinHudResync(player, playerRef);
		}

		paintHeldItem(player, playerRef);
	}

	private static void schedulePostJoinHudResync(Player player, PlayerRef playerRef) {
		if (player == null || playerRef == null) {
			return;
		}
		HytaleServer.SCHEDULED_EXECUTOR.schedule(
			() -> {
				try {
					if (player.wasRemoved()) {
						return;
					}
					World world = player.getWorld();
					if (world == null) {
						return;
					}
					world.execute(() -> paintHeldItem(player, playerRef));
				} catch (Exception t) {
					LOGGER.atWarning().log("Post-join HUD resync failed for %s: %s", playerRef.getUuid(), t);
				}

			}, POST_JOIN_HUD_RESYNC_DELAY_MS, TimeUnit.MILLISECONDS
		);
	}
}
