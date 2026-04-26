package com.thescar.hygunsplugin.ui.hud.systems;

import com.thescar.hygunsplugin.gameplay.reload.ReloadManager;
import com.thescar.hygunsplugin.runtime.persistence.RuntimeWeaponInventorySync;
import com.thescar.hygunsplugin.support.hytale.PlayerInventoryAccess;
import com.thescar.hygunsplugin.ui.hud.HudCoordinator;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.event.events.ecs.SwitchActiveSlotEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * Updates (or hides) the Hyguns ammo HUD immediately when the player swaps
 * their active hotbar slot.
 */
public final class ActiveSlotHudUpdateSystem extends EntityEventSystem<EntityStore, SwitchActiveSlotEvent> {
	public ActiveSlotHudUpdateSystem() {
		super(SwitchActiveSlotEvent.class);
	}

	@Override
	public Query<EntityStore> getQuery() {
		// InventoryPacketHandler only invokes this event on player entities; using
		// any() keeps this lightweight.
		return Query.any();
	}

	@Override
	public void handle(int index, ArchetypeChunk<EntityStore> archetypeChunk, Store<EntityStore> store,
	                   @NonNull CommandBuffer<EntityStore> commandBuffer, @NonNull SwitchActiveSlotEvent event) {
		Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
		UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
		if (uuidComponent == null) {
			return;
		}

		UUID uuid = uuidComponent.getUuid();

		PlayerRef playerRef = HudCoordinator.getPlayerRef(uuid);
		Player player = HudCoordinator.getPlayer(uuid);

		if (playerRef == null || player == null) {
			return;
		}

		if (ReloadManager.isReloading(playerRef)) {
			ReloadManager.cancel(playerRef, ReloadManager.CancelReason.SWITCHED_ITEM);
		}

		RuntimeWeaponInventorySync.ensureTrackedInventory(player);

		ItemStack held = PlayerInventoryAccess.getItemInHand(player);
		if (held == null) {
			HudCoordinator.hideAmmo(playerRef);
			return;
		}

		HudCoordinator.updateAmmo(playerRef, held);
	}
}
