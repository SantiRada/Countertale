package com.thescar.hygunsplugin.runtime.systems;

import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeEcs;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class ItemRuntimeStoreTickBridgeSystem extends TickingSystem<EntityStore> {
	@Override
	public void tick(float dt, int index, Store<EntityStore> store) {
		ItemRuntimeEcs.rememberWorldThread(store.getExternalData().getWorld());
		ItemRuntimeEcs.tick(dt);
	}
}
