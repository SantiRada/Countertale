package Tenzinn.Core.Interactions;

import Tenzinn.Core.UI.CaseInventoryPage;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class UseInventoryBookInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<UseInventoryBookInteraction> CODEC = BuilderCodec
            .builder(UseInventoryBookInteraction.class, UseInventoryBookInteraction::new, SimpleInstantInteraction.CODEC)
            .build();

    @Override
    protected void firstRun(@Nonnull InteractionType interactionType,
                            @Nonnull InteractionContext context,
                            @Nonnull CooldownHandler cooldownHandler) {

        Ref<EntityStore> ref = context.getEntity();
        Store<EntityStore> store = context.getCommandBuffer().getStore();

        Player player = context.getCommandBuffer().getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = context.getCommandBuffer().getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        player.getPageManager().openCustomPage(ref, store, new CaseInventoryPage(playerRef));
        context.getState().state = InteractionState.Finished;
    }
}
