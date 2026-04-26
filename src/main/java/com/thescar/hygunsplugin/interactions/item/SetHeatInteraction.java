package com.thescar.hygunsplugin.interactions.item;

import com.thescar.hygunsplugin.HygunsPluginMain;
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

public class SetHeatInteraction extends SimpleInstantInteraction {
	public static final String KEY = HygunsPluginMain.key("SetHeat");
	public static final BuilderCodec<SetHeatInteraction> CODEC = InteractionChain
		.of(
			SetHeatInteraction.class, SetHeatInteraction::new,
			BuilderCodec.builder(SetHeatInteraction.class, SetHeatInteraction::new, SimpleInstantInteraction.CODEC)
		)
		.inheritedField("Amount", Codec.DOUBLE, interaction -> interaction.amountValue)
		.documentation("Sets held item heat directly without consuming ammo or inventory. Amount is normalized heat in range 0.0..1.0.")
		.build();

	private final InteractionValue<Double> amountValue = new InteractionValue<>(0.0D).addValidator(value -> {
		if (!ValueUtils.Validators.nonNegativeDouble(value)) {
			return false;
		}
		return value == null || value.doubleValue() <= 1.0D;
	});

	public SetHeatInteraction(String id) {
		super(id);
	}

	protected SetHeatInteraction() {
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
		float amount = this.amountValue.get() != null
		               ? Math.max(0.0F, Math.min(1.0F, this.amountValue.get().floatValue()))
		               : 0.0F;
		RuntimeHeatLogic.setHeat(state.heat(), amount, RuntimeHeatLogic.wrappedNowMillis());
	}
}
