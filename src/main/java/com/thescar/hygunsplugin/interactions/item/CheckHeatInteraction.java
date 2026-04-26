package com.thescar.hygunsplugin.interactions.item;

import com.thescar.hygunsplugin.HygunsPluginMain;
import com.thescar.hygunsplugin.runtime.components.HeatDataComponent;
import com.thescar.hygunsplugin.runtime.logic.RuntimeHeatLogic;
import com.thescar.hygunsplugin.support.interaction.HeldItemInteractionContext;
import com.thescar.hygunsplugin.support.interaction.InteractionStateSupport;
import com.thescar.hygunsplugin.support.interaction.InteractionValue;
import com.thescar.hygunsplugin.support.text.ValueUtils;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;

import javax.annotation.Nonnull;

public class CheckHeatInteraction extends SimpleInstantInteraction {
	public static final String KEY = HygunsPluginMain.key("CheckHeat");
	public static final BuilderCodec<CheckHeatInteraction> CODEC = BuilderCodec
		.builder(CheckHeatInteraction.class, CheckHeatInteraction::new, SimpleInstantInteraction.CODEC)
		.appendInherited(
			new KeyedCodec<>("Overheated", Codec.BOOLEAN), (interaction, value) -> interaction.overheatedValue.set(value),
			interaction -> interaction.overheatedValue.get(), (interaction, parent) -> interaction.overheatedValue.set(parent.overheatedValue.get())
		)
		.add()
		.appendInherited(
			new KeyedCodec<>("MinHeat", Codec.DOUBLE), (interaction, value) -> interaction.minHeatValue.set(value),
			interaction -> interaction.minHeatValue.get(), (interaction, parent) -> interaction.minHeatValue.set(parent.minHeatValue.get())
		)
		.add()
		.appendInherited(
			new KeyedCodec<>("MaxHeat", Codec.DOUBLE), (interaction, value) -> interaction.maxHeatValue.set(value),
			interaction -> interaction.maxHeatValue.get(), (interaction, parent) -> interaction.maxHeatValue.set(parent.maxHeatValue.get())
		)
		.add()
		.documentation("Checks held item heat state.")
		.build();

	private final InteractionValue<Boolean> overheatedValue = new InteractionValue<Boolean>(null);
	private final InteractionValue<Double> minHeatValue = new InteractionValue<Double>(null).addNullableValidator(
		value -> ValueUtils.Validators.nonNegativeDouble(value) && (value == null || value.doubleValue() <= 1.0D)
	);
	private final InteractionValue<Double> maxHeatValue = new InteractionValue<Double>(null).addNullableValidator(
		value -> ValueUtils.Validators.nonNegativeDouble(value) && (value == null || value.doubleValue() <= 1.0D)
	);

	public CheckHeatInteraction(String id) {
		super(id);
	}

	protected CheckHeatInteraction() {
	}

	private static float toHeatValue(Double value) {
		return value != null && Double.isFinite(value)
		       ? Math.max(0.0F, Math.min(1.0F, value.floatValue()))
		       : 0.0F;
	}

	@Nonnull
	@Override
	public WaitForDataFrom getWaitForDataFrom() {
		return WaitForDataFrom.Server;
	}

	@Override
	protected void firstRun(@Nonnull InteractionType interactionType, @Nonnull InteractionContext interactionContext,
	                        @Nonnull CooldownHandler cooldownHandler) {
		HeldItemInteractionContext heldItemContext = HeldItemInteractionContext.resolve(interactionContext);
		if (heldItemContext == null) {
			InteractionStateSupport.fail(interactionContext);
			return;
		}

		var state = heldItemContext.ensureHeat();
		HeatDataComponent heat = state.heat();

		int nowMs = RuntimeHeatLogic.wrappedNowMillis();
		float currentHeat = RuntimeHeatLogic.currentHeat(heat, nowMs);
		Boolean requiredOverheated = this.overheatedValue.get();
		if (requiredOverheated != null && requiredOverheated.booleanValue() != heat.overheated()) {
			InteractionStateSupport.fail(interactionContext);
			return;
		}

		float minHeat = toHeatValue(this.minHeatValue.get());
		if (currentHeat < minHeat) {
			InteractionStateSupport.fail(interactionContext);
			return;
		}

		Double maxHeatValue = this.maxHeatValue.get();
		if (maxHeatValue != null && currentHeat > toHeatValue(maxHeatValue)) {
			InteractionStateSupport.fail(interactionContext);
		}

	}
}
