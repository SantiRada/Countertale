package com.thescar.hygunsplugin.interactions.item;

import com.thescar.hygunsplugin.HygunsPluginMain;
import com.thescar.hygunsplugin.runtime.components.HeatDataComponent;
import com.thescar.hygunsplugin.runtime.logic.RuntimeHeatLogic;
import com.thescar.hygunsplugin.support.interaction.HeldItemInteractionContext;
import com.thescar.hygunsplugin.support.interaction.InteractionChain;
import com.thescar.hygunsplugin.support.interaction.InteractionStateSupport;
import com.thescar.hygunsplugin.support.interaction.InteractionValue;
import com.thescar.hygunsplugin.support.text.ValueUtils;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;

import javax.annotation.Nonnull;

public class AddHeatInteraction extends SimpleInstantInteraction {
	public static final String KEY = HygunsPluginMain.key("AddHeat");
	public static final BuilderCodec<AddHeatInteraction> CODEC = InteractionChain
		.of(
			AddHeatInteraction.class, AddHeatInteraction::new,
			BuilderCodec.builder(AddHeatInteraction.class, AddHeatInteraction::new, SimpleInstantInteraction.CODEC)
		)
		.inheritedField("Amount", Codec.DOUBLE, interaction -> interaction.amountValue)
		.documentation("Adds normalized heat to the held item directly without consuming ammo or inventory. Amount is in range 0.0..1.0.")
		.build();

	private final InteractionValue<Double> amountValue = new InteractionValue<>(0.0D).addValidator(value -> {
		if (!ValueUtils.Validators.nonNegativeDouble(value)) {
			return false;
		}
		return value == null || value.doubleValue() <= 1.0D;
	});

	public AddHeatInteraction(String id) {
		super(id);
	}

	protected AddHeatInteraction() {
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
		float amount = this.amountValue.get() != null
		               ? Math.max(0.0F, Math.min(1.0F, this.amountValue.get().floatValue()))
		               : 0.0F;
		float nextHeat = RuntimeHeatLogic.currentHeat(heat, nowMs) + amount;
		RuntimeHeatLogic.setHeat(heat, nextHeat, nowMs);
	}
}
