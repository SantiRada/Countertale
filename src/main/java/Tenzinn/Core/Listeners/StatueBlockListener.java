package Tenzinn.Core.Listeners;

import com.hypixel.hytale.component.*;
import Tenzinn.Core.Storage.HologramStorage;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.*;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;

public class StatueBlockListener extends EntityEventSystem<EntityStore, DamageBlockEvent> {

    private static final StatueBlockListener INSTANCE = new StatueBlockListener();

    private final Map<UUID, PendingStatue> pending = new ConcurrentHashMap<>();
    private final Map<String, List<Ref<EntityStore>>> activeHolograms = new ConcurrentHashMap<>();

    private StatueBlockListener() { super(DamageBlockEvent.class); }

    public static StatueBlockListener getInstance() { return INSTANCE; }

    public void activateFor(UUID playerId, PlayerRef playerRef, World world, String statueType) { pending.put(playerId, new PendingStatue(playerRef, world, statueType)); }

    @Override
    public void handle(int index, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk, @NonNullDecl Store<EntityStore> store,
                       @NonNullDecl CommandBuffer<EntityStore> commandBuffer, @NonNullDecl DamageBlockEvent event) {

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
        if (uuidComponent == null) return;

        UUID playerId = uuidComponent.getUuid();
        PendingStatue pendingStatue = pending.remove(playerId);
        if (pendingStatue == null) return;

        event.setCancelled(true);

        Vector3i blockPos = event.getTargetBlock();
        Vector3d position = new Vector3d(blockPos.x, blockPos.y, blockPos.z);

        // Remove any previous holograms of the same type, if they exist.
        removeOldHolograms(pendingStatue.statueType, pendingStatue.world);
        HologramStorage.getInstance().savePosition(pendingStatue.statueType, position.x, position.y, position.z);

        // Create the two stacked holograms
        String titleLine;
        String subtitleLine;

        switch(pendingStatue.statueType.toLowerCase()) {
            case "queue":
                titleLine = MessageListeners.get(MessageListeners.MessageKey.UI_TITLE_QUEUE);
                subtitleLine = MessageListeners.get(MessageListeners.MessageKey.UI_DESC_QUEUE);
                break;
            default:
                titleLine    = MessageListeners.get(MessageListeners.MessageKey.UI_TITLE_SHOP);
                subtitleLine = MessageListeners.get(MessageListeners.MessageKey.UI_DESC_SHOP);
                break;
        }

        createHologramLine(position, pendingStatue.world, pendingStatue.playerRef, pendingStatue.statueType, titleLine, 1.25);
        createHologramLine(position, pendingStatue.world, pendingStatue.playerRef, pendingStatue.statueType, subtitleLine, 0.90);

        pendingStatue.playerRef.sendMessage(
                Message.raw("[" + pendingStatue.statueType.toUpperCase() + "] assigned in ("
                        + blockPos.x + ", " + blockPos.y + ", " + blockPos.z + ")").color(Color.green)
        );
    }

    private void removeOldHolograms(String statueType, World world) {
        List<Ref<EntityStore>> oldRefs = activeHolograms.remove(statueType);
        if (oldRefs == null) return;

        world.execute(() -> {
            Store<EntityStore> entityStore = world.getEntityStore().getStore();
            for (Ref<EntityStore> oldRef : oldRefs) {
                if (oldRef.isValid()) {
                    entityStore.removeEntity(oldRef, RemoveReason.REMOVE);
                }
            }
        });
    }

    public void deleteAllHolograms(World world, PlayerRef playerRef) {
        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            List<Ref<EntityStore>> toRemove = new ArrayList<>();

            Query<EntityStore> query = new Query<EntityStore>() {
                @Override
                public boolean test(Archetype<EntityStore> archetype) {
                    return archetype.contains(Nameplate.getComponentType())
                            && !archetype.contains(PlayerRef.getComponentType());
                }

                @Override
                public boolean requiresComponentType(ComponentType<EntityStore, ?> componentType) { return false; }

                @Override
                public void validateRegistry(ComponentRegistry<EntityStore> registry) {}

                @Override
                public void validate() {}
            };

            store.forEachChunk(query, (ArchetypeChunk<EntityStore> chunk, CommandBuffer<EntityStore> commandBuffer) -> {
                for (int i = 0; i < chunk.size(); i++) { toRemove.add(chunk.getReferenceTo(i)); }
            });

            int count = toRemove.size();
            for (Ref<EntityStore> ref : toRemove) {
                if (ref.isValid()) { store.removeEntity(ref, RemoveReason.REMOVE); }
            }

            activeHolograms.clear();
            HologramStorage.getInstance().clearAll();

            playerRef.sendMessage(Message.raw(count + " holograms were removed.").color(Color.green));
        });
    }

    public void createHologramLine(Vector3d position, World world, PlayerRef playerRef, String statueType, String label, double yOffset) {
        Transform playerTransform = playerRef.getTransform();
        world.execute(() -> {
            Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
            ProjectileComponent projectileComponent = new ProjectileComponent("Projectile");
            holder.putComponent(ProjectileComponent.getComponentType(), projectileComponent);

            Vector3d holoPos = new Vector3d(position.x + 0.5, position.y + yOffset, position.z + 0.5);

            holder.putComponent(TransformComponent.getComponentType(),
                    new TransformComponent(holoPos, playerTransform.getRotation().clone()));
            holder.ensureComponent(UUIDComponent.getComponentType());

            if (projectileComponent.getProjectile() == null) {
                projectileComponent.initialize();
                if (projectileComponent.getProjectile() == null) return;
            }

            holder.addComponent(NetworkId.getComponentType(),
                    new NetworkId(world.getEntityStore().getStore().getExternalData().takeNextNetworkId()));

            holder.addComponent(Nameplate.getComponentType(), new Nameplate(label));

            Ref<EntityStore> newRef = world.getEntityStore().getStore().addEntity(holder, AddReason.SPAWN);

            // Add the reference to the list of the corresponding type.
            activeHolograms.computeIfAbsent(statueType, k -> new ArrayList<>()).add(newRef);
        });
    }

    @Nullable @Override
    public Query<EntityStore> getQuery() { return PlayerRef.getComponentType(); }

    private static class PendingStatue {
        final PlayerRef playerRef;
        final World world;
        final String statueType;

        PendingStatue(PlayerRef playerRef, World world, String statueType) {
            this.playerRef = playerRef;
            this.world = world;
            this.statueType = statueType;
        }
    }
}