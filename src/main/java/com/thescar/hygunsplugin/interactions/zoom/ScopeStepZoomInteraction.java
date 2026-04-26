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

public class ScopeStepZoomInteraction extends SimpleInstantInteraction {
	public static final String KEY = "Scope_Step_Zoom";
	public static final BuilderCodec<ScopeStepZoomInteraction> CODEC = BuilderCodec
		.builder(ScopeStepZoomInteraction.class, ScopeStepZoomInteraction::new, SimpleInstantInteraction.CODEC)
		.documentation("Step zoom when left-clicked with a scope in Zoom Mode").build();

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

		ZoomManager.stepZoom(zoomContext.entityRef(), zoomContext.playerRef());
	}
}
