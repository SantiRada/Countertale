package com.thescar.hygunsplugin.interactions.zoom;

import com.thescar.hygunsplugin.HygunsPluginMain;
import com.thescar.hygunsplugin.gameplay.zoom.ZoomManager;
import com.thescar.hygunsplugin.support.interaction.InteractionStateSupport;
import com.thescar.hygunsplugin.support.interaction.ZoomInteractionContext;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;

import javax.annotation.Nonnull;

public class ResetZoomInteraction extends SimpleInstantInteraction {
	public static final String KEY = HygunsPluginMain.key("ResetZoom");
	public static final BuilderCodec<ResetZoomInteraction> CODEC = BuilderCodec
		.builder(ResetZoomInteraction.class, ResetZoomInteraction::new, SimpleInstantInteraction.CODEC)
		.documentation("Disables current zoom state on the player.")
		.build();

	public ResetZoomInteraction(String id) {
		super(id);
	}

	protected ResetZoomInteraction() {
	}

	@Override
	protected void firstRun(@Nonnull InteractionType interactionType, @Nonnull InteractionContext context,
	                        @Nonnull CooldownHandler cooldownHandler) {
		ZoomInteractionContext zoomContext = ZoomInteractionContext.resolve(context);
		if (zoomContext == null) {
			InteractionStateSupport.fail(context);
			return;
		}

		ZoomManager.disableZoom(zoomContext.entityRef(), zoomContext.playerRef(), zoomContext.commandBuffer());
	}
}
