package com.thescar.hygunsplugin.interactions.item;

import com.thescar.hygunsplugin.HygunsPluginMain;
import com.thescar.hygunsplugin.debug.DebugLogger;
import com.thescar.hygunsplugin.runtime.components.HeatDataComponent;
import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeEcs;
import com.thescar.hygunsplugin.runtime.logic.RuntimeHeatLogic;
import com.thescar.hygunsplugin.support.interaction.InteractionStateSupport;
import com.thescar.hygunsplugin.support.interaction.InteractionValue;
import com.thescar.hygunsplugin.support.interaction.HeldItemInteractionContext;
import com.thescar.hygunsplugin.support.text.ValueUtils;
import com.thescar.hygunsplugin.ui.hud.screens.HeatUiSettings;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;

import javax.annotation.Nonnull;

import java.io.IOException;

import org.bson.BsonDocument;
import org.bson.BsonValue;

public class HeatInteraction extends SimpleInstantInteraction {
	public static final String KEY = HygunsPluginMain.key("Heat");
	private static final Codec<BsonDocument> BSON_DOCUMENT_CODEC = new Codec<>() {
		@Override
		public BsonDocument decode(BsonValue bsonValue, ExtraInfo extraInfo) {
			return bsonValue != null && bsonValue.isDocument()
			       ? bsonValue.asDocument()
			       : new BsonDocument();
		}

		@Override
		public BsonValue encode(BsonDocument bsonDocument, ExtraInfo extraInfo) {
			return bsonDocument != null
			       ? bsonDocument
			       : new BsonDocument();
		}

		@Override
		@SuppressWarnings("deprecation")
		public BsonDocument decodeJson(RawJsonReader rawJsonReader, ExtraInfo extraInfo) throws IOException {
			return RawJsonReader.readBsonDocument(rawJsonReader);
		}

		@Override
		public Schema toSchema(SchemaContext schemaContext) {
			Schema schema = new Schema();
			schema.setTypes(new String[]{"object"});
			return schema;
		}
	};
	public static final BuilderCodec<HeatInteraction> CODEC = BuilderCodec
		.builder(HeatInteraction.class, HeatInteraction::new, SimpleInstantInteraction.CODEC)
		.appendInherited(
			new KeyedCodec<>("OverheatEnabled", Codec.BOOLEAN),
			(interaction, value) -> interaction.overheatEnabledValue.set(value), interaction -> interaction.overheatEnabledValue.get(),
			(interaction, parent) -> interaction.overheatEnabledValue.set(parent.overheatEnabledValue.get())
		)
		.add()
		.appendInherited(
			new KeyedCodec<>("OverheatTime", Codec.DOUBLE), (interaction, value) -> interaction.overheatTimeValue.set(value),
			interaction -> interaction.overheatTimeValue.get(),
			(interaction, parent) -> interaction.overheatTimeValue.set(parent.overheatTimeValue.get())
		)
		.add()
		.appendInherited(
			new KeyedCodec<>("CooldownTime", Codec.DOUBLE), (interaction, value) -> interaction.cooldownTimeValue.set(value),
			interaction -> interaction.cooldownTimeValue.get(),
			(interaction, parent) -> interaction.cooldownTimeValue.set(parent.cooldownTimeValue.get())
		)
		.add()
		.appendInherited(
			new KeyedCodec<>("UI", BSON_DOCUMENT_CODEC), (interaction, value) -> interaction.uiValue.set(value),
			interaction -> interaction.uiValue.get(),
			(interaction, parent) -> interaction.uiValue.set(parent.uiValue.get())
		)
		.add().documentation("Tracks heat on the held item and fails while overheated.").build();
	private final InteractionValue<Boolean> overheatEnabledValue = new InteractionValue<>(true);
	private final InteractionValue<Double> overheatTimeValue = new InteractionValue<>(10.0D)
		.addValidator(ValueUtils.Validators::nonNegativeDouble);
	private final InteractionValue<Double> cooldownTimeValue = new InteractionValue<>(10.0D)
		.addValidator(ValueUtils.Validators::nonNegativeDouble);
	private final InteractionValue<BsonDocument> uiValue = new InteractionValue<>(new BsonDocument());

	public HeatInteraction(String id) {
		super(id);
	}

	protected HeatInteraction() {
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
		heat.setUiSettings(HeatUiSettings.fromBson(this.uiValue.get()));
		boolean overheatEnabled = Boolean.TRUE.equals(this.overheatEnabledValue.get());
		DebugLogger.debug(
			"Heat", () -> "Start heldItem=" + heldItemContext.itemStack().getItemId()
				+ ", enabled=" + overheatEnabled
				+ ", currentHeat=" + RuntimeHeatLogic.currentHeat(heat, RuntimeHeatLogic.wrappedNowMillis())
				+ ", overheated=" + heat.overheated()
		);
		if (!overheatEnabled) {
			ItemRuntimeEcs.removeComponent(state.ref(), HeatDataComponent.getComponentType());
			DebugLogger.debug("Heat", "Removed heat component because overheat is disabled");
			InteractionStateSupport.finish(interactionContext);
			return;
		}

		int overheatTimeMs = RuntimeHeatLogic.secondsToMillis(this.overheatTimeValue.get(), 10.0D);
		if (overheatTimeMs <= 0) {
			ItemRuntimeEcs.removeComponent(state.ref(), HeatDataComponent.getComponentType());
			DebugLogger.debug("Heat", "Removed heat component because overheat time resolved to <= 0");
			InteractionStateSupport.finish(interactionContext);
			return;
		}

		int cooldownTimeMs = RuntimeHeatLogic.secondsToMillis(this.cooldownTimeValue.get(), this.overheatTimeValue.get());
		if (cooldownTimeMs <= 0) {
			cooldownTimeMs = overheatTimeMs;
		}

		int nowMs = RuntimeHeatLogic.wrappedNowMillis();
		RuntimeHeatLogic.Result result = RuntimeHeatLogic.onUse(heat, true, overheatTimeMs, cooldownTimeMs, nowMs);
		int effectiveCooldownTimeMs = cooldownTimeMs;
		DebugLogger.debug(
			"Heat", () -> "After onUse: failed=" + result.failed()
				+ ", overheated=" + heat.overheated()
				+ ", currentHeat=" + RuntimeHeatLogic.currentHeat(heat, nowMs)
				+ ", overheatTimeMs=" + overheatTimeMs
				+ ", cooldownTimeMs=" + effectiveCooldownTimeMs
		);
		if (result.failed()) {
			InteractionStateSupport.fail(interactionContext);
			return;
		}

		InteractionStateSupport.finish(interactionContext);
	}
}
