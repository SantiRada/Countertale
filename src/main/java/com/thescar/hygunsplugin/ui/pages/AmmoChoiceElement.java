package com.thescar.hygunsplugin.ui.pages;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceElement;
import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceInteraction;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public final class AmmoChoiceElement extends ChoiceElement {
	private static final String ELEMENT_LAYOUT = "Pages/ItemRepairElement.ui";

	private final ItemStack itemStack;

	private final String countLabel;

	public AmmoChoiceElement(ItemStack itemStack, int quantity, ChoiceInteraction interaction) {
		this.itemStack = itemStack;
		this.countLabel = Integer.toString(Math.max(0, quantity));
		this.interactions = new ChoiceInteraction[]{interaction};
	}

	@Override
	public void addButton(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, String path, PlayerRef playerRef) {
		commandBuilder.append("#ElementList", ELEMENT_LAYOUT);
		commandBuilder.set(path + " #Icon.ItemId", this.itemStack.getItemId());
		commandBuilder.set(
			path + " #Name.TextSpans", Message.translation(this.itemStack
				.getItem()
				.getTranslationKey())
		);
		commandBuilder.set(path + " #Durability.Text", this.countLabel);
	}
}
