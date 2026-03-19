package Tenzinn.Core.Listeners;

import Tenzinn.Core.Tools.RefactorTool;
import Tenzinn.Core.Storage.HologramStorage;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.List;

public class QueueStatueListener extends EntityEventSystem<EntityStore, DamageBlockEvent> {

    private static final double TOLERANCE = 1.0;

    public QueueStatueListener() { super(DamageBlockEvent.class); }

    @Override
    public void handle(int index, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk, @NonNullDecl Store<EntityStore> store,
                       @NonNullDecl CommandBuffer<EntityStore> commandBuffer, @NonNullDecl DamageBlockEvent event) {

        List<HologramStorage.HologramEntry> entries = HologramStorage.getInstance().loadAll();
        HologramStorage.HologramEntry queueEntry = entries.stream().filter(e -> e.statueType.equals("queue"))
                .findFirst().orElse(null);

        if (queueEntry == null) return;

        Vector3i blockPos = event.getTargetBlock();
        boolean isQueueBlock = Math.abs(blockPos.x - queueEntry.x) < TOLERANCE && Math.abs(blockPos.y - queueEntry.y) <
                TOLERANCE && Math.abs(blockPos.z - queueEntry.z) < TOLERANCE;

        if (!isQueueBlock) return;
        event.setCancelled(true);

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        Player player = store.getComponent(ref, Player.getComponentType());

        if (playerRef == null || player == null) return;

        boolean inQueue = RefactorTool.getPlayerStats(playerRef) != null;

        if (inQueue) CommandManager.get().handleCommand(player, "leave");
        else CommandManager.get().handleCommand(player, "queue --mode=dm");
    }

    @Override
    public Query<EntityStore> getQuery() { return PlayerRef.getComponentType(); }
}