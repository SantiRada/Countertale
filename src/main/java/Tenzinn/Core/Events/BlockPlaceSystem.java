package Tenzinn.Core.Events;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockPlaceSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

    private static boolean blockingEnabled = true;

    public static void setBlocking(boolean enabled) { blockingEnabled = enabled; }
    public static boolean isBlocking() { return blockingEnabled; }

    public BlockPlaceSystem() { super(PlaceBlockEvent.class); }

    @Override
    public void handle(int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull PlaceBlockEvent event) {
        if (blockingEnabled) event.setCancelled(true);
    }

    @Nullable @Override
    public Query<EntityStore> getQuery() { return PlayerRef.getComponentType(); }
}