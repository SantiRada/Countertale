package com.thescar.hygunsplugin.interactions.zoom;

import com.thescar.hygunsplugin.HygunsPluginMain;
import com.thescar.hygunsplugin.gameplay.zoom.ZoomManager;
import com.thescar.hygunsplugin.runtime.components.ZoomStateComponent;
import com.thescar.hygunsplugin.support.interaction.InteractionChain;
import com.thescar.hygunsplugin.support.interaction.InteractionStateSupport;
import com.thescar.hygunsplugin.support.interaction.InteractionValue;
import com.thescar.hygunsplugin.support.interaction.ZoomInteractionContext;
import com.thescar.hygunsplugin.support.text.ValueUtils;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;

import javax.annotation.Nonnull;

public class SetZoomInteraction extends SimpleInstantInteraction {
	public static final String KEY = HygunsPluginMain.key("SetZoom");
	public static final BuilderCodec<SetZoomInteraction> CODEC = InteractionChain
		.of(
			SetZoomInteraction.class, SetZoomInteraction::new,
			BuilderCodec.builder(SetZoomInteraction.class, SetZoomInteraction::new, SimpleInstantInteraction.CODEC)
		)
		.inheritedField("OverlayTexturePath", Codec.STRING, interaction -> interaction.overlayTexturePathValue)
		.inheritedField("MaxDistance", Codec.FLOAT, interaction -> interaction.maxDistanceValue)
		.inheritedField("MinDistance", Codec.FLOAT, interaction -> interaction.minDistanceValue)
		.inheritedField("DefaultZoomMultiplier", Codec.FLOAT, interaction -> interaction.defaultZoomMultiplierValue)
		.inheritedField("MaxZoomMultiplier", Codec.FLOAT, interaction -> interaction.maxZoomMultiplierValue)
		.inheritedField("ZoomMultiplierStep", Codec.FLOAT, interaction -> interaction.zoomMultiplierStepValue)
		.inheritedField("ExactStep", Codec.INTEGER, interaction -> interaction.exactStepValue)
		.documentation("Enables zoom and forces it to a specific zoom step.")
		.build();

	private final InteractionValue<String> overlayTexturePathValue = new InteractionValue<>(null);
	private final InteractionValue<Float> maxDistanceValue = new InteractionValue<>(ZoomManager.ZoomConfig.MAX_DISTANCE)
		.addValidator(ValueUtils.Validators::positiveFloat);
	private final InteractionValue<Float> minDistanceValue = new InteractionValue<>(ZoomManager.ZoomConfig.MIN_DISTANCE)
		.addValidator(ValueUtils.Validators::positiveFloat);
	private final InteractionValue<Float> defaultZoomMultiplierValue = new InteractionValue<>(ZoomManager.ZoomConfig.DEFAULT_ZOOM_MULTIPLIER)
		.addValidator(ValueUtils.Validators::positiveFloat);
	private final InteractionValue<Float> maxZoomMultiplierValue = new InteractionValue<>(ZoomManager.ZoomConfig.MAX_ZOOM_MULTIPLIER)
		.addValidator(ValueUtils.Validators::positiveFloat);
	private final InteractionValue<Float> zoomMultiplierStepValue = new InteractionValue<>(ZoomManager.ZoomConfig.ZOOM_MULTIPLIER_STEP)
		.addValidator(ValueUtils.Validators::positiveFloat);
	private final InteractionValue<Integer> exactStepValue = new InteractionValue<>(0);

	public SetZoomInteraction(String id) {
		super(id);
	}

	protected SetZoomInteraction() {
	}

	@Override
	protected void firstRun(@Nonnull InteractionType interactionType, @Nonnull InteractionContext context,
	                        @Nonnull CooldownHandler cooldownHandler) {
		ZoomInteractionContext zoomContext = ZoomInteractionContext.resolve(context);
		if (zoomContext == null) {
			InteractionStateSupport.fail(context);
			return;
		}

		String itemId = zoomContext.heldItemId();
		if (itemId == null) {
			InteractionStateSupport.fail(context);
			return;
		}

		ZoomManager.ZoomSettings settings = ZoomManager.ZoomSettings.of(
			this.maxDistanceValue.get(), this.minDistanceValue.get(),
			this.defaultZoomMultiplierValue.get(), this.maxZoomMultiplierValue.get(), this.zoomMultiplierStepValue.get(),
			this.overlayTexturePathValue.get()
		);
		ZoomManager.enableZoom(zoomContext.entityRef(), zoomContext.playerRef(), itemId, settings, zoomContext.commandBuffer());

		ZoomStateComponent zoomState = zoomContext.zoomState();
		if (zoomState == null) {
			InteractionStateSupport.fail(context);
			return;
		}

		ZoomManager.setZoomStep(zoomState, this.exactStepValue.get());
	}
}
