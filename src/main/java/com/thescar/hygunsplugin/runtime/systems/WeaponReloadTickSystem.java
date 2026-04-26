package com.thescar.hygunsplugin.runtime.systems;

import com.thescar.hygunsplugin.gameplay.ammo.AmmoService;
import com.thescar.hygunsplugin.gameplay.reload.ReloadManager;
import com.thescar.hygunsplugin.runtime.components.AmmoDataComponent;
import com.thescar.hygunsplugin.runtime.components.ItemRuntimeIdentityComponent;
import com.thescar.hygunsplugin.runtime.components.WeaponItemComponent;
import com.thescar.hygunsplugin.runtime.components.WeaponReloadComponent;
import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeStore;
import com.thescar.hygunsplugin.runtime.item.core.RuntimeItemIdentity;
import com.thescar.hygunsplugin.runtime.item.core.RuntimeItemRef;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeArchetypeChunk;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeCommandBuffer;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeEntityTickingSystem;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeQuery;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeStore;
import com.thescar.hygunsplugin.runtime.persistence.RuntimeWeaponDirtySync;
import com.thescar.hygunsplugin.support.hytale.PlayerInventoryAccess;
import com.thescar.hygunsplugin.support.hytale.PlayerRefAccess;
import com.thescar.hygunsplugin.ui.hud.HudCoordinator;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public final class WeaponReloadTickSystem extends RuntimeEntityTickingSystem<ItemRuntimeStore> {
	private static void applyReload(@Nonnull RuntimeCommandBuffer<ItemRuntimeStore> commandBuffer,
	                                @Nonnull RuntimeArchetypeChunk<ItemRuntimeStore> archetypeChunk, int index, @Nonnull RuntimeItemRef weaponRef,
	                                @Nonnull PlayerRef playerRef, @Nonnull Player player, @Nonnull ItemStack heldItem, @Nonnull AmmoDataComponent ammo,
	                                @Nonnull WeaponReloadComponent reload) {
		ammo.setMaxAmmo(reload.maxAmmo());
		if (!ammo.initialized() || ammo.ammo() >= ammo.maxAmmo()) {
			removeReloadComponent(commandBuffer, archetypeChunk, index, weaponRef, reload);
			HudCoordinator.updateAmmoRuntimeSafe(playerRef, player, heldItem);
			return;
		}

		String ammoItemId = reload.ammoItemId();
		if (ammoItemId == null || ammoItemId.isBlank()) {
			player.sendMessage(Message.raw("This weapon has no ammo type configured."));
			removeReloadComponent(commandBuffer, archetypeChunk, index, weaponRef, reload);
			HudCoordinator.updateAmmoRuntimeSafe(playerRef, player, heldItem);
			return;
		}

		if (AmmoService.getAmmoContainer(player) == null) {
			removeReloadComponent(commandBuffer, archetypeChunk, index, weaponRef, reload);
			HudCoordinator.updateAmmoRuntimeSafe(playerRef, player, heldItem);
			return;
		}

		int reloadCap = reload.reloadAmountPerInteraction() <= 0
		                ? ammo.maxAmmo()
		                : reload.reloadAmountPerInteraction();
		int neededAmmo = Math.min(ammo.maxAmmo() - ammo.ammo(), reloadCap);
		if (neededAmmo <= 0) {
			removeReloadComponent(commandBuffer, archetypeChunk, index, weaponRef, reload);
			HudCoordinator.updateAmmoRuntimeSafe(playerRef, player, heldItem);
			return;
		}

		int availableAmmo = AmmoService.countAmmo(AmmoService.getAmmoContainer(player), ammoItemId);
		int ammoToLoad = Math.min(neededAmmo, Math.max(0, availableAmmo));
		if (ammoToLoad <= 0) {
			player.sendMessage(Message.raw("No ammo in inventory."));
			removeReloadComponent(commandBuffer, archetypeChunk, index, weaponRef, reload);
			HudCoordinator.updateAmmoRuntimeSafe(playerRef, player, heldItem);
			return;
		}

		int removedAmmo = AmmoService.removeAmmo(player, ammoItemId, ammoToLoad, null);
		if (removedAmmo <= 0) {
			player.sendMessage(Message.raw("No ammo in inventory."));
			removeReloadComponent(commandBuffer, archetypeChunk, index, weaponRef, reload);
			HudCoordinator.updateAmmoRuntimeSafe(playerRef, player, heldItem);
			return;
		}

		ammo.reload(removedAmmo);
		ammo.setLoadedAmmoItemId(ammoItemId);
		ammo.setLoadedAmmoIcon(reload.ammoIcon());
		ammo.markDirty();
		removeReloadComponent(commandBuffer, archetypeChunk, index, weaponRef, reload);
		HudCoordinator.updateAmmoRuntimeSafe(playerRef, player, heldItem);
	}

	@Nonnull
	private static RuntimeItemRef weaponRef(@Nonnull ItemRuntimeIdentityComponent identity) {
		return new RuntimeItemRef(identity.runtimeId());
	}

	private static void removeReloadComponent(@Nonnull RuntimeCommandBuffer<ItemRuntimeStore> commandBuffer,
	                                          @Nonnull RuntimeArchetypeChunk<ItemRuntimeStore> archetypeChunk, int index,
	                                          @Nonnull RuntimeItemRef weaponRef, @Nullable WeaponReloadComponent reload) {
		commandBuffer.removeComponent(archetypeChunk.getReferenceTo(index), WeaponReloadComponent.getComponentType());
		ReloadManager.unregister(reload != null ? reload.playerUuid() : null, weaponRef);
	}

	@Nonnull
	@Override
	public RuntimeQuery<ItemRuntimeStore> getQuery() {
		return RuntimeQuery.and(
			WeaponItemComponent.getComponentType(), AmmoDataComponent.getComponentType(),
			WeaponReloadComponent.getComponentType()
		);
	}

	@Override
	public void tick(float dt, int index, @Nonnull RuntimeArchetypeChunk<ItemRuntimeStore> archetypeChunk, @Nonnull RuntimeStore<ItemRuntimeStore> store,
	                 @Nonnull RuntimeCommandBuffer<ItemRuntimeStore> commandBuffer) {
		ItemRuntimeIdentityComponent identity = archetypeChunk.getComponent(index, ItemRuntimeIdentityComponent.getComponentType());
		WeaponReloadComponent reload = archetypeChunk.getComponent(index, WeaponReloadComponent.getComponentType());
		AmmoDataComponent ammo = archetypeChunk.getComponent(index, AmmoDataComponent.getComponentType());
		if (identity == null || identity.runtimeId() == null || reload == null || ammo == null || !reload.active()) {
			return;
		}

		UUID playerUuid = reload.playerUuid();
		if (playerUuid == null) {
			removeReloadComponent(commandBuffer, archetypeChunk, index, weaponRef(identity), reload);
			return;
		}

		PlayerRef playerRef = Universe.get().getPlayer(playerUuid);
		if (playerRef == null || !playerRef.isValid() || !RuntimeWeaponDirtySync.isPlayerInCurrentWorld(playerRef)) {
			removeReloadComponent(commandBuffer, archetypeChunk, index, weaponRef(identity), reload);
			return;
		}

		Player player = PlayerRefAccess.getComponent(playerRef, Player.getComponentType());
		if (player == null || player.wasRemoved()) {
			removeReloadComponent(commandBuffer, archetypeChunk, index, weaponRef(identity), reload);
			return;
		}

		RuntimeItemRef weaponRef = weaponRef(identity);
		ItemStack heldItem = PlayerInventoryAccess.getItemInHand(player);
		RuntimeItemRef heldRef = RuntimeItemIdentity.resolve(heldItem);
		if (!weaponRef.equals(heldRef)) {
			removeReloadComponent(commandBuffer, archetypeChunk, index, weaponRef, reload);
			return;
		}

		long nowMs = System.currentTimeMillis();
		if (nowMs < reload.readyAtMs()) {
			return;
		}

		applyReload(commandBuffer, archetypeChunk, index, weaponRef, playerRef, player, heldItem, ammo, reload);
	}
}
