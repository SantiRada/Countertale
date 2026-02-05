package Tenzinn.Events;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public class PreventItemDrop extends EntityEventSystem<EntityStore, DropItemEvent.PlayerRequest> {

    public PreventItemDrop() { super(DropItemEvent.PlayerRequest.class); }

    @Override
    public void handle(int index,@Nonnull ArchetypeChunk<EntityStore> archetypeChunk,@Nonnull Store<EntityStore> store,@Nonnull CommandBuffer<EntityStore> commandBuffer,@Nonnull DropItemEvent.PlayerRequest dropEvent) {
        dropEvent.setCancelled(true);
    }

    @Override
    public Query<EntityStore> getQuery() { return Archetype.empty(); }
}