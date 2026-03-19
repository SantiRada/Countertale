package Tenzinn.Core.Interactions;

import Tenzinn.Core.Tools.RefactorTool;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;

import javax.annotation.Nonnull;

public class UseActionBookInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<UseActionBookInteraction> CODEC = BuilderCodec.builder(UseActionBookInteraction.class, UseActionBookInteraction::new, SimpleInstantInteraction.CODEC).build();

    @Override
    protected void firstRun(@Nonnull InteractionType interactionType, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {

        Player player = context.getCommandBuffer().getComponent(context.getEntity(), Player.getComponentType());

        PlayerRef playerRef = context.getCommandBuffer().getComponent(context.getEntity(), PlayerRef.getComponentType());
        boolean inQueue = RefactorTool.getPlayerStats(playerRef) != null;

        if (player == null) {context.getState().state = InteractionState.Failed; return; }

        if(inQueue) CommandManager.get().handleCommand(player, "leave");
        else CommandManager.get().handleCommand(player, "queue --mode=null");

        context.getState().state = InteractionState.Finished;
    }
}