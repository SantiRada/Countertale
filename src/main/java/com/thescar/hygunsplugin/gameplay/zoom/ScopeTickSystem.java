package com.thescar.hygunsplugin.gameplay.zoom;

import com.thescar.hygunsplugin.runtime.components.ZoomStateComponent;
import com.thescar.hygunsplugin.support.hytale.PlayerInventoryAccess;
import com.thescar.hygunsplugin.support.hytale.PlayerRefAccess;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class ScopeTickSystem extends EntityTickingSystem<EntityStore> {
	@Nonnull
	private final Query<EntityStore> query;

	public ScopeTickSystem() {
		this.query = Query.and(Player.getComponentType(), ZoomStateComponent.getComponentType());
	}

	@Override
	public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store,
	                 @Nonnull CommandBuffer<EntityStore> commandBuffer) {
		var playerType = Player.getComponentType();
		if (playerType == null) {
			return;
		}

		Player player = EntityUtils.toHolder(index, archetypeChunk).getComponent(playerType);
		ZoomStateComponent zoomState = archetypeChunk.getComponent(index, ZoomStateComponent.getComponentType());
		if (player == null) {
			return;
		}

		if (zoomState == null) {
			return;
		}

		Ref<EntityStore> playerEntityRef = archetypeChunk.getReferenceTo(index);
		PlayerRef playerRef = PlayerRefAccess.getValid(playerEntityRef, store);
		if (playerRef == null) {
			commandBuffer.removeComponent(playerEntityRef, ZoomStateComponent.getComponentType());
			return;
		}

		var item = PlayerInventoryAccess.getItemInHand(player);
		String currentItemId = item == null
		                       ? null
		                       : item.getItemId();
		if (currentItemId == null || !currentItemId.equals(zoomState.scopedItemId())) {
			commandBuffer.removeComponent(playerEntityRef, ZoomStateComponent.getComponentType());
			ZoomManager.clearZoomView(playerRef);
			return;
		}

		World world = commandBuffer.getExternalData().getWorld();
		ZoomManager.updateZoom(zoomState, playerRef, commandBuffer, world, playerEntityRef);
	}

	@Override
	public boolean isParallel(int archetypeChunkSize, int taskCount) {
		return false;
	}

	@Nonnull
	@Override
	public Query<EntityStore> getQuery() {
		return this.query;
	}
}
