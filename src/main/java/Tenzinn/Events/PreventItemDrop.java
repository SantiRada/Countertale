package Tenzinn.Events;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class PreventItemDrop extends EntityEventSystem<EntityStore, DropItemEvent.PlayerRequest> {

    public PreventItemDrop() { super(DropItemEvent.PlayerRequest.class); }

    @Override
    public void handle(int index,@Nonnull ArchetypeChunk<EntityStore> archetypeChunk,@Nonnull Store<EntityStore> store,@Nonnull CommandBuffer<EntityStore> commandBuffer,@Nonnull DropItemEvent.PlayerRequest dropEvent) {
        dropEvent.setCancelled(true);

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = Universe.get().getPlayerByUsername(player.getDisplayName(), NameMatching.EXACT);

        CommandManager.get().handleCommand(playerRef, "shop");
    }

    @Override
    public Query<EntityStore> getQuery() { return Archetype.empty(); }
}