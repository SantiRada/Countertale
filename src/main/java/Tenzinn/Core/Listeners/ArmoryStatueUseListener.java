package Tenzinn.Core.Listeners;

import Tenzinn.Core.Armory.ArmoryPage;
import Tenzinn.Core.Storage.HologramStorage;

import com.hypixel.hytale.component.*;
import org.joml.Vector3i;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.List;

public class ArmoryStatueUseListener extends EntityEventSystem<EntityStore, UseBlockEvent.Pre> {

    private static final double TOLERANCE = 1.0;

    public ArmoryStatueUseListener() { super(UseBlockEvent.Pre.class); }

    @Override
    public void handle(int index, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
                       @NonNullDecl Store<EntityStore> store,
                       @NonNullDecl CommandBuffer<EntityStore> commandBuffer,
                       @NonNullDecl UseBlockEvent.Pre event) {

        HologramStorage.HologramEntry entry = findArmoryEntry();
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

    private HologramStorage.HologramEntry findArmoryEntry() {
        List<HologramStorage.HologramEntry> entries = HologramStorage.getInstance().loadAll();
        return entries.stream()
                .filter(e -> "armory".equalsIgnoreCase(e.statueType))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Query<EntityStore> getQuery() { return PlayerRef.getComponentType(); }
}
