package Tenzinn.Core.Events;

import Tenzinn.Core.GameMatch;
import Tenzinn.Core.Listeners.MessageListeners;
import Tenzinn.Core.Localization.Lang;
import Tenzinn.Core.Objects.PlayerStats;
import Tenzinn.Core.Tools.RefactorTool;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
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

        UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
        PlayerRef playerRef = uuidComponent != null ? Universe.get().getPlayer(uuidComponent.getUuid()) : null;
        assert playerRef != null;

        // Is in queue or match
        PlayerStats playerStats = RefactorTool.getPlayerStats(playerRef);
        if (playerStats != null) {
            GameMatch match = playerStats.getCurrentMatch();
            String mode = match.getMode();

            if (mode.equalsIgnoreCase("dm")) {
                CommandManager.get().handleCommand(playerRef, "shop");
            } else {
                // Is in FVF to playing
                if(match.getState() == GameMatch.MatchState.STARTING || match.getState() == GameMatch.MatchState.ON_PURCHASE) {
                    CommandManager.get().handleCommand(playerRef, "shop");
                }
                else if(match.getState() == GameMatch.MatchState.IN_PROGRESS) {
                    playerRef.sendMessage(Lang.msg("shop.buying-round-closed"));
                }
                else {
                    // Is in lobby in queue to FVF
                    playerRef.sendMessage(MessageListeners.message(MessageListeners.MessageKey.CHAT_SHOP_IN_LOBBY_FVF).color(Color.cyan));
                }
            }
        }
        else {
            playerRef.sendMessage(MessageListeners.message(MessageListeners.MessageKey.CHAT_SHOP_IN_LOBBY).color(Color.cyan));
        }
    }

    @Override
    public Query<EntityStore> getQuery() { return Archetype.empty(); }
}
