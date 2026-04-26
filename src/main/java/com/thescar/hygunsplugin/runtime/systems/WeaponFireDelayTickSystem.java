package com.thescar.hygunsplugin.runtime.systems;

import com.thescar.hygunsplugin.runtime.components.FireDelayComponent;
import com.thescar.hygunsplugin.runtime.components.WeaponItemComponent;
import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeStore;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeArchetypeChunk;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeCommandBuffer;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeEntityTickingSystem;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeQuery;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeStore;

import javax.annotation.Nonnull;

public final class WeaponFireDelayTickSystem extends RuntimeEntityTickingSystem<ItemRuntimeStore> {
	@Nonnull
	@Override
	public RuntimeQuery<ItemRuntimeStore> getQuery() {
		return RuntimeQuery.and(WeaponItemComponent.getComponentType(), FireDelayComponent.getComponentType());
	}

	@Override
	public void tick(float dt, int index, @Nonnull RuntimeArchetypeChunk<ItemRuntimeStore> archetypeChunk, @Nonnull RuntimeStore<ItemRuntimeStore> store,
	                 @Nonnull RuntimeCommandBuffer<ItemRuntimeStore> commandBuffer) {
		FireDelayComponent fireDelay = archetypeChunk.getComponent(index, FireDelayComponent.getComponentType());
		if (fireDelay == null || fireDelay.blocksAt(System.currentTimeMillis())) {
			return;
		}

		commandBuffer.removeComponent(archetypeChunk.getReferenceTo(index), FireDelayComponent.getComponentType());
	}
}
