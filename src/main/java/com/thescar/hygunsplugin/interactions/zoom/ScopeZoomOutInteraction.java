package com.thescar.hygunsplugin.interactions.zoom;

import com.thescar.hygunsplugin.gameplay.zoom.ZoomManager;
import com.thescar.hygunsplugin.support.interaction.InteractionStateSupport;
import com.thescar.hygunsplugin.support.interaction.ZoomInteractionContext;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;

import org.jetbrains.annotations.NotNull;

public class ScopeZoomOutInteraction extends SimpleInstantInteraction {
	public static final String KEY = "Scope_Zoom_Out";
	public static final BuilderCodec<ScopeZoomOutInteraction> CODEC = BuilderCodec
		.builder(ScopeZoomOutInteraction.class, ScopeZoomOutInteraction::new, SimpleInstantInteraction.CODEC)
		.documentation("Decrease zoom while Scope zoom mode is active").build();

	@Override

	protected void firstRun(@NotNull InteractionType interactionType, @NotNull InteractionContext context,
	                        @NotNull CooldownHandler cooldownHandler) {
		ZoomInteractionContext zoomContext = ZoomInteractionContext.resolve(context);
		if (zoomContext == null) {
			InteractionStateSupport.fail(context);
			return;
		}

		if (zoomContext.zoomState() == null) {
			InteractionStateSupport.fail(context);
			return;
		}

		ZoomManager.zoomOut(zoomContext.entityRef(), zoomContext.playerRef());
	}
}
