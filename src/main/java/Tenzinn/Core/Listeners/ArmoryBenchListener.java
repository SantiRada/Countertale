package Tenzinn.Core.Listeners;

import Tenzinn.Core.UI.CaseInventoryPage;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;

public class ArmoryBenchListener extends EntityEventSystem<EntityStore, UseBlockEvent.Pre> {

    private static final String BENCH_ID = "Armory";

    public ArmoryBenchListener() { super(UseBlockEvent.Pre.class); }

    @Override
    public void handle(int i, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                       CommandBuffer<EntityStore> buffer, UseBlockEvent.Pre event) {

        if (event.isCancelled()) return;
        if (!BENCH_ID.equals(event.getBlockType().getId())) return;

        event.setCancelled(true);

        Ref<EntityStore> ref = chunk.getReferenceTo(i);
        Player    player     = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef  = store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null || playerRef == null) return;

        player.getPageManager().openCustomPage(ref, store, new CaseInventoryPage(playerRef));
    }

    @Nullable @Override
    public Query<EntityStore> getQuery() { return PlayerRef.getComponentType(); }
}
