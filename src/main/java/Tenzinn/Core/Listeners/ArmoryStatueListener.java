package Tenzinn.Core.Listeners;

import Tenzinn.Core.Armory.ArmoryPage;
import Tenzinn.Core.Storage.HologramStorage;

import com.hypixel.hytale.component.*;
import org.joml.Vector3i;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.List;

public class ArmoryStatueListener extends EntityEventSystem<EntityStore, DamageBlockEvent> {

    private static final double TOLERANCE = 1.0;

    public ArmoryStatueListener() { super(DamageBlockEvent.class); }

    @Override
    public void handle(int index, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
                       @NonNullDecl Store<EntityStore> store,
                       @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
                       @NonNullDecl DamageBlockEvent event) {

        List<HologramStorage.HologramEntry> entries = HologramStorage.getInstance().loadAll();
        HologramStorage.HologramEntry entry = entries.stream()
                .filter(e -> e.statueType.equals("armory"))
                .findFirst().orElse(null);

        if (entry == null) return;

        Vector3i blockPos = event.getTargetBlock();
        boolean isArmoryBlock =
                Math.abs(blockPos.x - entry.x) < TOLERANCE &&
                Math.abs(blockPos.y - entry.y) < TOLERANCE &&
                Math.abs(blockPos.z - entry.z) < TOLERANCE;

        if (!isArmoryBlock) return;
        event.setCancelled(true);

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        PlayerRef playerRef  = store.getComponent(ref, PlayerRef.getComponentType());
        Player    player     = store.getComponent(ref, Player.getComponentType());

        if (playerRef == null || player == null) return;

        player.getPageManager().openCustomPage(ref, store, new ArmoryPage(playerRef));
    }

    @Override
    public Query<EntityStore> getQuery() { return PlayerRef.getComponentType(); }
}
