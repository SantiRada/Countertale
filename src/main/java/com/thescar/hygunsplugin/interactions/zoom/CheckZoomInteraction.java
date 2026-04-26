package com.thescar.hygunsplugin.interactions.zoom;

import com.thescar.hygunsplugin.HygunsPluginMain;
import com.thescar.hygunsplugin.gameplay.zoom.ZoomManager;
import com.thescar.hygunsplugin.runtime.components.ZoomStateComponent;
import com.thescar.hygunsplugin.support.interaction.InteractionStateSupport;
import com.thescar.hygunsplugin.support.interaction.InteractionValue;
import com.thescar.hygunsplugin.support.interaction.ZoomInteractionContext;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;

import javax.annotation.Nonnull;

public class CheckZoomInteraction extends SimpleInstantInteraction {
	public static final String KEY = HygunsPluginMain.key("CheckZoom");
	public static final BuilderCodec<CheckZoomInteraction> CODEC = BuilderCodec
		.builder(CheckZoomInteraction.class, CheckZoomInteraction::new, SimpleInstantInteraction.CODEC)
		.appendInherited(
			new KeyedCodec<>("Zoomed", Codec.BOOLEAN), (interaction, value) -> interaction.zoomedValue.set(value),
			interaction -> interaction.zoomedValue.get(), (interaction, parent) -> interaction.zoomedValue.set(parent.zoomedValue.get())
		)
		.add()
		.appendInherited(
			new KeyedCodec<>("MinStep", Codec.INTEGER), (interaction, value) -> interaction.minStepValue.set(value),
			interaction -> interaction.minStepValue.get(), (interaction, parent) -> interaction.minStepValue.set(parent.minStepValue.get())
		)
		.add()
		.appendInherited(
			new KeyedCodec<>("MaxStep", Codec.INTEGER), (interaction, value) -> interaction.maxStepValue.set(value),
			interaction -> interaction.maxStepValue.get(), (interaction, parent) -> interaction.maxStepValue.set(parent.maxStepValue.get())
		)
		.add()
		.appendInherited(
			new KeyedCodec<>("ExactStep", Codec.INTEGER), (interaction, value) -> interaction.exactStepValue.set(value),
			interaction -> interaction.exactStepValue.get(), (interaction, parent) -> interaction.exactStepValue.set(parent.exactStepValue.get())
		)
		.add()
		.documentation("Checks current zoom state and zoom step on the player.")
		.build();

	private final InteractionValue<Boolean> zoomedValue = new InteractionValue<Boolean>(null);
	private final InteractionValue<Integer> minStepValue = new InteractionValue<Integer>(null);
	private final InteractionValue<Integer> maxStepValue = new InteractionValue<Integer>(null);
	private final InteractionValue<Integer> exactStepValue = new InteractionValue<Integer>(null);

	public CheckZoomInteraction(String id) {
		super(id);
	}

	protected CheckZoomInteraction() {
	}

	@Override
	protected void firstRun(@Nonnull InteractionType interactionType, @Nonnull InteractionContext context,
	                        @Nonnull CooldownHandler cooldownHandler) {
		ZoomInteractionContext zoomContext = ZoomInteractionContext.resolve(context);
		if (zoomContext == null) {
			InteractionStateSupport.fail(context);
			return;
		}

		ZoomStateComponent zoomState = zoomContext.zoomState();
		boolean isZoomed = zoomState != null;
		Boolean expectedZoomed = this.zoomedValue.get();
		if (expectedZoomed != null && expectedZoomed.booleanValue() != isZoomed) {
			InteractionStateSupport.fail(context);
			return;
		}

		int currentStep = zoomState != null
		                  ? ZoomManager.getZoomStep(zoomState)
		                  : 0;
		Integer exactStep = this.exactStepValue.get();
		if (exactStep != null && currentStep != Math.max(0, exactStep)) {
			InteractionStateSupport.fail(context);
			return;
		}

		Integer minStep = this.minStepValue.get();
		if (minStep != null && currentStep < Math.max(0, minStep)) {
			InteractionStateSupport.fail(context);
			return;
		}

		Integer maxStep = this.maxStepValue.get();
		if (maxStep != null && currentStep > Math.max(0, maxStep)) {
			InteractionStateSupport.fail(context);
		}

	}
}
