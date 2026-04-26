package com.thescar.hygunsplugin.interactions.inventory;

import com.thescar.hygunsplugin.HygunsPluginMain;
import com.thescar.hygunsplugin.debug.DebugLogger;
import com.thescar.hygunsplugin.support.interaction.InteractionChain;
import com.thescar.hygunsplugin.support.interaction.InteractionStateSupport;
import com.thescar.hygunsplugin.support.interaction.InteractionValue;
import com.thescar.hygunsplugin.support.text.StringUtil;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class HoldingInteraction extends SimpleInstantInteraction {
	public static final String KEY = HygunsPluginMain.key("Holding");
	public static final String SHORT_KEY = "Holding";
	private static final MapCodec<String, LinkedHashMap<String, String>> SELECT_VARS_CODEC = new MapCodec<>(
		RootInteraction.CHILD_ASSET_CODEC, LinkedHashMap::new
	);
	private static final MapCodec<Map<String, String>, LinkedHashMap<String, Map<String, String>>> SELECT_CODEC = new MapCodec<>(
		SELECT_VARS_CODEC, LinkedHashMap::new
	);

	public static final BuilderCodec<HoldingInteraction> CODEC = InteractionChain
		.of(
			HoldingInteraction.class, HoldingInteraction::new,
			BuilderCodec.builder(HoldingInteraction.class, HoldingInteraction::new, SimpleInstantInteraction.CODEC)
		)
		.inheritedField("Utility", Codec.BOOLEAN, interaction -> interaction.utilityValue)
		.inheritedField("ItemId", Codec.STRING, interaction -> interaction.itemIdValue)
		.inheritedField("ItemIds", Codec.STRING_ARRAY, interaction -> interaction.itemIdsValue)
		.inheritedField("Select", SELECT_CODEC, interaction -> interaction.selectValue)
		.documentation("Checks the active main-hand or utility-hand item id and can overlay interaction variables by matched item id.")
		.build();

	private final InteractionValue<Boolean> utilityValue = new InteractionValue<>(false);
	private final InteractionValue<String> itemIdValue = new InteractionValue<>("");
	private final InteractionValue<String[]> itemIdsValue = new InteractionValue<>(new String[0]);
	private final InteractionValue<Map<String, Map<String, String>>> selectValue = new InteractionValue<>(Map.of());

	public HoldingInteraction(String id) {
		super(id);
	}

	protected HoldingInteraction() {
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
		String heldItemId = stack != null && !stack.isEmpty()
		                    ? StringUtil.normalize(stack.getItemId())
		                    : null;
		if (matchesAny(heldItemId)) {
			applySelectedVars(interactionContext, heldItemId);
			InteractionStateSupport.finish(interactionContext);
			return;
		}

		DebugLogger.debug(
			"Holding", () -> "utility=" + resolveUtility()
				+ ", heldItem=" + (heldItemId != null
				                   ? heldItemId
				                   : "null")
				+ ", expected=" + expectedListForDebug()
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

	private boolean matchesAny(@Nullable String heldItemId) {
		if (heldItemId == null) {
			return false;
		}

		String itemId = StringUtil.normalize(this.itemIdValue.get());
		if (itemId != null && heldItemId.equalsIgnoreCase(itemId)) {
			return true;
		}

		if (findSelectedVars(heldItemId) != null) {
			return true;
		}

		String[] itemIds = this.itemIdsValue.get();
		if (itemIds == null) {
			return false;
		}

		for (String candidate : itemIds) {
			String normalized = StringUtil.normalize(candidate);
			if (normalized != null && heldItemId.equalsIgnoreCase(normalized)) {
				return true;
			}
		}

		return false;
	}

	private void applySelectedVars(@Nonnull InteractionContext interactionContext, @Nullable String heldItemId) {
		Map<String, String> selectedVars = findSelectedVars(heldItemId);
		if (selectedVars == null || selectedVars.isEmpty()) {
			return;
		}

		Function<InteractionContext, Map<String, String>> originalGetter = interactionContext.getInteractionVarsGetter();
		interactionContext.setInteractionVarsGetter(context -> {
			Map<String, String> originalVars = originalGetter != null
			                                   ? originalGetter.apply(context)
			                                   : null;
			LinkedHashMap<String, String> merged = new LinkedHashMap<>();
			if (originalVars != null) {
				merged.putAll(originalVars);
			}
			merged.putAll(selectedVars);
			return merged;
		});
	}

	@Nullable
	private Map<String, String> findSelectedVars(@Nullable String heldItemId) {
		if (heldItemId == null) {
			return null;
		}

		for (Map.Entry<String, Map<String, String>> entry : normalizedSelect().entrySet()) {
			if (heldItemId.equalsIgnoreCase(entry.getKey())) {
				return entry.getValue();
			}
		}
		return null;
	}

	private Map<String, Map<String, String>> normalizedSelect() {
		Map<String, Map<String, String>> select = this.selectValue.get();
		if (select == null || select.isEmpty()) {
			return Map.of();
		}

		LinkedHashMap<String, Map<String, String>> normalized = new LinkedHashMap<>();
		for (Map.Entry<String, Map<String, String>> entry : select.entrySet()) {
			String itemId = StringUtil.normalize(entry.getKey());
			Map<String, String> vars = normalizeVars(entry.getValue());
			if (itemId == null || vars.isEmpty()) {
				continue;
			}

			normalized.put(itemId, vars);
		}
		return normalized;
	}

	private static Map<String, String> normalizeVars(@Nullable Map<String, String> vars) {
		if (vars == null || vars.isEmpty()) {
			return Map.of();
		}

		LinkedHashMap<String, String> normalized = new LinkedHashMap<>();
		for (Map.Entry<String, String> entry : vars.entrySet()) {
			String key = StringUtil.normalize(entry.getKey());
			String value = StringUtil.normalize(entry.getValue());
			if (key == null || value == null) {
				continue;
			}
			normalized.put(key, value);
		}
		return normalized;
	}

	private String expectedListForDebug() {
		String itemId = StringUtil.normalize(this.itemIdValue.get());
		String[] itemIds = this.itemIdsValue.get();
		StringBuilder builder = new StringBuilder("[");
		if (itemId != null) {
			builder.append(itemId);
		}

		if (itemIds != null) {
			for (String candidate : itemIds) {
				String normalized = StringUtil.normalize(candidate);
				if (normalized == null) {
					continue;
				}

				if (builder.length() > 1) {
					builder.append(", ");
				}
				builder.append(normalized);
			}
		}

		for (String candidate : normalizedSelect().keySet()) {
			if (builder.length() > 1) {
				builder.append(", ");
			}
			builder.append(candidate);
		}

		return builder.append(']').toString();
	}
}
