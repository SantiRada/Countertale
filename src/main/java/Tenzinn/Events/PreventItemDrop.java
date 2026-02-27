package Tenzinn.Events;

import Tenzinn.Deathmatch.GameMatch;
import Tenzinn.Tools.RefactorTool;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.awt.*;

public class PreventItemDrop extends EntityEventSystem<EntityStore, DropItemEvent.PlayerRequest> {

    public PreventItemDrop() { super(DropItemEvent.PlayerRequest.class); }

    @Override
    public void handle(int index,@Nonnull ArchetypeChunk<EntityStore> archetypeChunk,@Nonnull Store<EntityStore> store,@Nonnull CommandBuffer<EntityStore> commandBuffer,@Nonnull DropItemEvent.PlayerRequest dropEvent) {
        dropEvent.setCancelled(true);

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);

        Player player = store.getComponent(ref, Player.getComponentType());
        assert player != null;

        PlayerRef playerRef = Universe.get().getPlayerByUsername(player.getDisplayName(), NameMatching.EXACT);
        assert playerRef != null;

        if (RefactorTool.getPlayerStats(playerRef) != null) { CommandManager.get().handleCommand(playerRef, "shop"); }
        else { playerRef.sendMessage(Message.raw("You must be in a match or in the queue to open the shop.").color(Color.cyan)); }
    }

    @Override
    public Query<EntityStore> getQuery() { return Archetype.empty(); }
}