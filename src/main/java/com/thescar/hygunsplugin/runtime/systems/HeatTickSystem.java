package com.thescar.hygunsplugin.runtime.systems;

import com.thescar.hygunsplugin.runtime.components.HeatDataComponent;
import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeStore;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeArchetypeChunk;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeCommandBuffer;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeEntityTickingSystem;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeQuery;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeStore;
import com.thescar.hygunsplugin.runtime.logic.RuntimeHeatLogic;

import javax.annotation.Nonnull;

public final class HeatTickSystem extends RuntimeEntityTickingSystem<ItemRuntimeStore> {
	@Nonnull
	@Override
	public RuntimeQuery<ItemRuntimeStore> getQuery() {
		return RuntimeQuery.and(HeatDataComponent.getComponentType());
	}

	@Override
	public void tick(float dt, int index, @Nonnull RuntimeArchetypeChunk<ItemRuntimeStore> archetypeChunk, @Nonnull RuntimeStore<ItemRuntimeStore> store,
	                 @Nonnull RuntimeCommandBuffer<ItemRuntimeStore> commandBuffer) {
		HeatDataComponent heat = archetypeChunk.getComponent(index, HeatDataComponent.getComponentType());
		if (heat == null) {
			return;
		}

		if (heat.overheatTimeMs() <= 0 || heat.cooldownTimeMs() <= 0) {
			commandBuffer.removeComponent(archetypeChunk.getReferenceTo(index), HeatDataComponent.getComponentType());
			return;
		}

		int nowMs = RuntimeHeatLogic.wrappedNowMillis();
		RuntimeHeatLogic.tick(heat, dt, nowMs);
		if (!RuntimeHeatLogic.isActivelyFiring(heat, nowMs) && RuntimeHeatLogic.currentHeat(heat, nowMs) <= 0.0F) {
			commandBuffer.removeComponent(archetypeChunk.getReferenceTo(index), HeatDataComponent.getComponentType());
		}
	}
}
