package com.thescar.hygunsplugin.interactions.weapon;

import com.thescar.hygunsplugin.HygunsPluginMain;
import com.thescar.hygunsplugin.gameplay.reload.ReloadManager;
import com.thescar.hygunsplugin.support.hytale.PlayerRefAccess;
import com.thescar.hygunsplugin.support.interaction.InteractionStateSupport;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class CancelReloadInteraction extends SimpleInstantInteraction {
	public static final String KEY = HygunsPluginMain.key("CancelReload");
	public static final BuilderCodec<CancelReloadInteraction> CODEC = BuilderCodec
		.builder(CancelReloadInteraction.class, CancelReloadInteraction::new, SimpleInstantInteraction.CODEC)
		.documentation("Cancels active reload on the current player.")
		.build();

	public CancelReloadInteraction(String id) {
		super(id);
	}

	protected CancelReloadInteraction() {
	}

	@Nonnull
	@Override
	public WaitForDataFrom getWaitForDataFrom() {
		return WaitForDataFrom.Server;
	}

	@Override
	protected void firstRun(@Nonnull InteractionType interactionType, @Nonnull InteractionContext interactionContext,
	                        @Nonnull CooldownHandler cooldownHandler) {
		CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
		Ref<EntityStore> ref = interactionContext.getEntity();
		PlayerRef playerRef = PlayerRefAccess.getValid(ref, commandBuffer);
		if (playerRef == null) {
			InteractionStateSupport.fail(interactionContext);
			return;
		}

		ReloadManager.cancel(playerRef, ReloadManager.CancelReason.OTHER);
	}
}
