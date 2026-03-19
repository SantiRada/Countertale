package Tenzinn.Core.Events;

import Tenzinn.Core.GameMatch;
import Tenzinn.Core.Listeners.MessageListeners;
import Tenzinn.Core.Objects.PlayerStats;
import Tenzinn.Core.Tools.RefactorTool;
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
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);

        Player player = store.getComponent(ref, Player.getComponentType());
        assert player != null;

        PlayerRef playerRef = Universe.get().getPlayerByUsername(player.getDisplayName(), NameMatching.EXACT);
        assert playerRef != null;

        // Is in queue or match
        if (RefactorTool.getPlayerStats(playerRef) != null) {
            PlayerStats playerStats = RefactorTool.getPlayerStats(playerRef);
            assert playerStats != null;
            GameMatch match = playerStats.getCurrentMatch();
            String mode = match.getMode();

            if (mode.equalsIgnoreCase("dm")) {
                dropEvent.setCancelled(true);
                CommandManager.get().handleCommand(playerRef, "shop");
            } else {
                // Is in FVF to playing
                if(match.getState() == GameMatch.MatchState.STARTING || match.getState() == GameMatch.MatchState.IN_PROGRESS) {
                    CommandManager.get().handleCommand(playerRef, "shop");
                } else {
                    // Is in lobby in queue to FVF
                    playerRef.sendMessage(Message.raw(MessageListeners.get(MessageListeners.MessageKey.CHAT_SHOP_IN_LOBBY_FVF)).color(Color.cyan));
                }
            }
        }
        else {
            // Is in the lobby without queue
            dropEvent.setCancelled(true);
            playerRef.sendMessage(Message.raw(MessageListeners.get(MessageListeners.MessageKey.CHAT_SHOP_IN_LOBBY)).color(Color.cyan));
        }
    }

    @Override
    public Query<EntityStore> getQuery() { return Archetype.empty(); }
}