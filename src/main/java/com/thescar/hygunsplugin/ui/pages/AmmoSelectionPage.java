package com.thescar.hygunsplugin.ui.pages;

import com.thescar.hygunsplugin.content.registry.AmmoRegistry;
import com.thescar.hygunsplugin.content.registry.GunRegistry;
import com.thescar.hygunsplugin.content.settings.GunSettings;
import com.thescar.hygunsplugin.content.settings.WeaponAmmoSettings;
import com.thescar.hygunsplugin.gameplay.ammo.AmmoInventoryAccess;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceBasePage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceElement;
import com.hypixel.hytale.server.core.inventory.ItemContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

import java.util.Map;

public final class AmmoSelectionPage extends ChoiceBasePage {
	private static final String LAYOUT = "Pages/AmmoSelectionPage.ui";

	public AmmoSelectionPage(PlayerRef playerRef, ItemContainer itemContainer, ItemContext heldItemContext) {
		super(playerRef, getItemElements(itemContainer, heldItemContext), LAYOUT);
	}

	private static ChoiceElement[] getItemElements(ItemContainer itemContainer, ItemContext heldItemContext) {
		ItemStack heldItem = heldItemContext.getItemStack();
		GunSettings gunSettings = heldItem != null
		                          ? GunRegistry.getSettings(heldItem.getItemId())
		                          : null;
		WeaponAmmoSettings weaponAmmo = gunSettings != null
		                                ? gunSettings.ammo()
		                                : null;
		if (weaponAmmo == null) {
			return new ChoiceElement[0];
		}

		Object2IntLinkedOpenHashMap<String> quantities = AmmoInventoryAccess.collectCompatibleAmmoQuantities(itemContainer, weaponAmmo);
		if (quantities.isEmpty()) {
			return new ChoiceElement[0];
		}

		Object2ObjectLinkedOpenHashMap<String, ItemStack> stacksById = new Object2ObjectLinkedOpenHashMap<>();
		for (short slot = 0; slot < itemContainer.getCapacity(); slot++) {
			ItemStack itemStack = itemContainer.getItemStack(slot);
			if (ItemStack.isEmpty(itemStack) || itemStack.getQuantity() <= 0) {
				continue;
			}

			String itemId = itemStack.getItemId();
			if (!quantities.containsKey(itemId)) {
				continue;
			}

			stacksById.putIfAbsent(itemId, itemStack);
		}

		return quantities.keySet().stream().map(itemId -> Map.entry(itemId, AmmoRegistry.getAmmo(itemId)))
			.sorted(AmmoRegistry.ammoOrdering())
			.map(entry -> new AmmoChoiceElement(
				stacksById.getOrDefault(entry.getKey(), new ItemStack(entry.getKey())),
				quantities.getInt(entry.getKey()), new SelectAmmoInteraction(heldItemContext, entry.getKey())
			))
			.toArray(ChoiceElement[]::new);
	}

	@Override
	public void build(Ref<EntityStore> ref, UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, Store<EntityStore> store) {
		if (getElements().length > 0) {
			super.build(ref, commandBuilder, eventBuilder, store);
			return;
		}

		commandBuilder.append(getPageLayout());
		commandBuilder.clear("#ElementList");
		commandBuilder.appendInline("#ElementList", "Label { Text: \"No compatible ammo found\"; Style: (Alignment: Center); }");
	}
}
