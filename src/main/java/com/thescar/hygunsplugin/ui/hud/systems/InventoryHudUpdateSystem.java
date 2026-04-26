package com.thescar.hygunsplugin.ui.hud.systems;

import com.thescar.hygunsplugin.runtime.persistence.RuntimeWeaponInventorySync;
import com.thescar.hygunsplugin.support.hytale.PlayerInventoryAccess;
import com.thescar.hygunsplugin.ui.hud.HudCoordinator;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryChangeEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import org.jspecify.annotations.NonNull;

import java.util.UUID;

public final class InventoryHudUpdateSystem extends EntityEventSystem<EntityStore, InventoryChangeEvent> {
	public InventoryHudUpdateSystem() {
		super(InventoryChangeEvent.class);
	}

	private static boolean isTrackedContainer(@NonNull Player player, ItemContainer changedContainer, @NonNull InventoryChangeEvent event) {
		if (changedContainer == null) {
			return false;
		}

		return event.getComponentType() == InventoryComponent.Hotbar.getComponentType()
			|| event.getComponentType() == InventoryComponent.Utility.getComponentType()
			|| event.getComponentType() == InventoryComponent.Storage.getComponentType()
			|| event.getComponentType() == InventoryComponent.Backpack.getComponentType()
			|| changedContainer.equals(PlayerInventoryAccess.getHotbar(player))
			|| changedContainer.equals(PlayerInventoryAccess.getUtility(player))
			|| changedContainer.equals(PlayerInventoryAccess.getStorage(player))
			|| changedContainer.equals(PlayerInventoryAccess.getBackpack(player));
	}

	@Override
	public Query<EntityStore> getQuery() {
		return Archetype.of(Player.getComponentType());
	}

	@Override
	public void handle(int index, ArchetypeChunk<EntityStore> archetypeChunk, Store<EntityStore> store,
	                   @NonNull CommandBuffer<EntityStore> commandBuffer, @NonNull InventoryChangeEvent event) {
		Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
		Player player = archetypeChunk.getComponent(index, Player.getComponentType());
		if (ref == null || player == null) {
			return;
		}

		UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
		if (uuidComponent == null) {
			return;
		}

		UUID uuid = uuidComponent.getUuid();

		PlayerRef playerRef = HudCoordinator.getPlayerRef(uuid);
		if (playerRef == null) {
			return;
		}

		ItemContainer changedContainer = event.getItemContainer();
		if (!isTrackedContainer(player, changedContainer, event)) {
			return;
		}

		RuntimeWeaponInventorySync.ensureTrackedInventory(player);

		ItemStack held = InventoryComponent.getItemInHand(store, ref);
		if (held == null) {
			HudCoordinator.hideAmmo(playerRef);
			return;
		}

		HudCoordinator.updateAmmo(playerRef, held);
	}
}
