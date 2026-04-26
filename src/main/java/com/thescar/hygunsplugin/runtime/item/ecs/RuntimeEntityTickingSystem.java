package com.thescar.hygunsplugin.runtime.item.ecs;

import javax.annotation.Nonnull;

public abstract class RuntimeEntityTickingSystem<S> {
	@Nonnull
	public abstract RuntimeQuery<S> getQuery();

	public abstract void tick(float dt, int index, @Nonnull RuntimeArchetypeChunk<S> archetypeChunk,
	                          @Nonnull RuntimeStore<S> store, @Nonnull RuntimeCommandBuffer<S> commandBuffer);
}
