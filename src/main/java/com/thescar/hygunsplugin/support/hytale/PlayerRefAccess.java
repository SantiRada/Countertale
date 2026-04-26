package com.thescar.hygunsplugin.support.hytale;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PlayerRefAccess {
	private PlayerRefAccess() {
	}

	@Nullable
	public static PlayerRef get(@Nonnull Ref<EntityStore> ref, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
		return commandBuffer.getComponent(ref, Universe.get().getPlayerRefComponentType());
	}

	@Nullable
	public static PlayerRef get(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
		return store.getComponent(ref, Universe.get().getPlayerRefComponentType());
	}

	@Nullable
	public static PlayerRef getValid(@Nonnull Ref<EntityStore> ref, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
		PlayerRef playerRef = get(ref, commandBuffer);
		return playerRef != null && playerRef.isValid()
		       ? playerRef
		       : null;
	}

	@Nullable
	public static PlayerRef getValid(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
		PlayerRef playerRef = get(ref, store);
		return playerRef != null && playerRef.isValid()
		       ? playerRef
		       : null;
	}

	@Nullable
	public static <T extends Component<EntityStore>> T getComponent(
		@Nonnull PlayerRef playerRef, @Nonnull ComponentType<EntityStore, T> componentType
	) {
		Ref<EntityStore> ref = playerRef.getReference();
		if (ref == null || ref.getStore() == null) {
			return null;
		}
		return ref.getStore().getComponent(ref, componentType);
	}
}
