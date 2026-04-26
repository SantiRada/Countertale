package com.thescar.hygunsplugin.interactions.inventory;

import com.thescar.hygunsplugin.HygunsPluginMain;
import com.thescar.hygunsplugin.debug.DebugLogger;
import com.thescar.hygunsplugin.support.interaction.InteractionChain;
import com.thescar.hygunsplugin.support.interaction.InteractionStateSupport;
import com.thescar.hygunsplugin.support.interaction.InteractionValue;
import com.thescar.hygunsplugin.support.math.NumericComparison;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CheckDurabilityInteraction extends SimpleInstantInteraction {
	public static final String KEY = HygunsPluginMain.key("CheckDurability");
	public static final String SHORT_KEY = "CheckDurability";
	public static final BuilderCodec<CheckDurabilityInteraction> CODEC = InteractionChain
		.of(
			CheckDurabilityInteraction.class, CheckDurabilityInteraction::new,
			BuilderCodec.builder(CheckDurabilityInteraction.class, CheckDurabilityInteraction::new, SimpleInstantInteraction.CODEC)
		)
		.inheritedField("Utility", Codec.BOOLEAN, interaction -> interaction.utilityValue)
		.inheritedField("Max", Codec.BOOLEAN, interaction -> interaction.maxValue)
		.inheritedField("Amount", Codec.STRING, interaction -> interaction.amountValue)
		.documentation("Checks held item durability or max durability in the main hand or utility hand.")
		.build();

	private final InteractionValue<Boolean> utilityValue = new InteractionValue<>(false);
	private final InteractionValue<Boolean> maxValue = new InteractionValue<>(false);
	private final InteractionValue<String> amountValue = new InteractionValue<>(">0");

	public CheckDurabilityInteraction(String id) {
		super(id);
	}

	protected CheckDurabilityInteraction() {
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
		if (commandBuffer == null || ref == null) {
			InteractionStateSupport.fail(interactionContext);
			return;
		}

		ItemStack stack = resolveUtility()
		                  ? getUtilityItem(commandBuffer, ref)
		                  : InventoryComponent.getItemInHand(commandBuffer, ref);
		NumericComparison check = NumericComparison.parse(this.amountValue.get());
		boolean checkMax = resolveMax();
		double durability = stack != null && !stack.isEmpty()
		                    ? (checkMax
		                       ? stack.getMaxDurability()
		                       : stack.getDurability())
		                    : 0.0D;
		if (stack != null && !stack.isEmpty() && check != null && check.test(durability)) {
			InteractionStateSupport.finish(interactionContext);
			return;
		}

		DebugLogger.debug(
			"CheckDurability", () -> "utility=" + resolveUtility()
				+ ", max=" + checkMax
				+ ", item=" + (stack != null && !stack.isEmpty()
				                ? stack.getItemId()
				                : "null")
				+ ", durability=" + (stack != null
				                      ? stack.getDurability()
				                      : "null")
				+ ", maxDurability=" + (stack != null
				                         ? stack.getMaxDurability()
				                         : "null")
				+ ", checkedDurability=" + durability
				+ ", amount=" + this.amountValue.get()
		);
		InteractionStateSupport.fail(interactionContext);
	}

	@Nullable
	private static ItemStack getUtilityItem(@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Ref<EntityStore> ref) {
		InventoryComponent.Utility utility = commandBuffer.getComponent(ref, InventoryComponent.Utility.getComponentType());
		return utility != null
		       ? utility.getActiveItem()
		       : null;
	}

	private boolean resolveUtility() {
		return Boolean.TRUE.equals(this.utilityValue.get());
	}

	private boolean resolveMax() {
		return Boolean.TRUE.equals(this.maxValue.get());
	}
}
