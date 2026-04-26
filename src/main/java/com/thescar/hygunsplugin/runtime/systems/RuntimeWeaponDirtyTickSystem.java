package com.thescar.hygunsplugin.runtime.systems;

import com.thescar.hygunsplugin.runtime.components.AmmoDataComponent;
import com.thescar.hygunsplugin.runtime.components.ItemRuntimeIdentityComponent;
import com.thescar.hygunsplugin.runtime.components.WeaponItemComponent;
import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeStore;
import com.thescar.hygunsplugin.runtime.item.core.RuntimeItemRef;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeArchetypeChunk;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeCommandBuffer;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeEntityTickingSystem;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeQuery;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeStore;
import com.thescar.hygunsplugin.runtime.persistence.RuntimeWeaponDirtySync;

import javax.annotation.Nonnull;

public final class RuntimeWeaponDirtyTickSystem extends RuntimeEntityTickingSystem<ItemRuntimeStore> {
	@Nonnull
	@Override
	public RuntimeQuery<ItemRuntimeStore> getQuery() {
		return RuntimeQuery.and(WeaponItemComponent.getComponentType(), AmmoDataComponent.getComponentType());
	}

	@Override
	public void tick(float dt, int index, @Nonnull RuntimeArchetypeChunk<ItemRuntimeStore> archetypeChunk, @Nonnull RuntimeStore<ItemRuntimeStore> store,
	                 @Nonnull RuntimeCommandBuffer<ItemRuntimeStore> commandBuffer) {
		ItemRuntimeIdentityComponent identity = archetypeChunk.getComponent(index, ItemRuntimeIdentityComponent.getComponentType());
		if (identity == null || identity.runtimeId() == null) {
			return;
		}

		AmmoDataComponent ammo = archetypeChunk.getComponent(index, AmmoDataComponent.getComponentType());
		if (ammo == null || !ammo.initialized() || !ammo.dirty()) {
			return;
		}

		RuntimeItemRef runtimeRef = new RuntimeItemRef(identity.runtimeId());
		if (RuntimeWeaponDirtySync.isActivelyHeld(runtimeRef)) {
			return;
		}

		RuntimeWeaponDirtySync.syncRuntimeWeapon(runtimeRef, ammo, false);
	}
}
