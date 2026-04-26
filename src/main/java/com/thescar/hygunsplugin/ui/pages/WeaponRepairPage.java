package com.thescar.hygunsplugin.ui.pages;

import com.thescar.hygunsplugin.content.registry.GunRegistry;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceBasePage;
import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceElement;
import com.hypixel.hytale.server.core.entity.entities.player.pages.itemrepair.ItemRepairElement;
import com.hypixel.hytale.server.core.inventory.ItemContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;

public class WeaponRepairPage extends ChoiceBasePage {
	private static final String LAYOUT = "Pages/WeaponRepairPage.ui";

	public WeaponRepairPage(PlayerRef playerRef, ItemContainer itemContainer, ItemContext heldItemContext) {
		super(playerRef, getItemElements(itemContainer, heldItemContext), LAYOUT);
	}

	private static ChoiceElement[] getItemElements(ItemContainer itemContainer, ItemContext heldItemContext) {
		List<ChoiceElement> elements = new ObjectArrayList<>();
		for (short slot = 0; slot < itemContainer.getCapacity(); slot++) {
			ItemStack itemStack = itemContainer.getItemStack(slot);
			if (ItemStack.isEmpty(itemStack) || itemStack.isUnbreakable()) {
				continue;
			}

			if (GunRegistry.getSettings(itemStack.getItemId()) == null) {
				continue;
			}

			if (itemStack.getDurability() >= itemStack.getMaxDurability()) {
				continue;
			}

			ItemContext itemContext = new ItemContext(itemContainer, slot, itemStack);
			elements.add(new ItemRepairElement(itemStack, new WeaponRepairInteraction(itemContext, heldItemContext)));
		}

		return elements.toArray(ChoiceElement[]::new);
	}

	@Override
	public void build(Ref<EntityStore> ref, UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, Store<EntityStore> store) {
		if (getElements().length > 0) {
			super.build(ref, commandBuilder, eventBuilder, store);
			return;
		}

		commandBuilder.append(getPageLayout());
		commandBuilder.clear("#ElementList");
		commandBuilder.appendInline("#ElementList", "Label { Text: %server.customUI.itemRepairPage.noItems; Style: (Alignment: Center); }");
	}
}
