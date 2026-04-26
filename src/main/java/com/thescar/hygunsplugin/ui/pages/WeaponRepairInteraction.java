package com.thescar.hygunsplugin.ui.pages;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager;
import com.hypixel.hytale.server.core.entity.entities.player.pages.itemrepair.RepairItemInteraction;
import com.hypixel.hytale.server.core.inventory.ItemContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class WeaponRepairInteraction extends RepairItemInteraction {
	public WeaponRepairInteraction(ItemContext itemContext, ItemContext heldItemContext) {
		super(itemContext, 0.0D, heldItemContext);
	}

	@Override
	public void run(Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef) {
		Player player = store.getComponent(ref, Player.getComponentType());
		if (player == null) {
			return;
		}

		PageManager pageManager = player.getPageManager();
		ItemStack itemStack = this.itemContext.getItemStack();
		if (itemStack == null || itemStack.isEmpty() || itemStack.isUnbreakable()) {
			pageManager.setPage(ref, store, Page.None);
			return;
		}

		double maxDurability = itemStack.getMaxDurability();
		if (maxDurability <= 0.0D || itemStack.getDurability() >= maxDurability) {
			pageManager.setPage(ref, store, Page.None);
			return;
		}

		ItemContainer heldContainer = this.heldItemContext.getContainer();
		short heldSlot = this.heldItemContext.getSlot();
		ItemStack heldItem = this.heldItemContext.getItemStack();
		ItemStackSlotTransaction removeTransaction = heldContainer.removeItemStackFromSlot(heldSlot, heldItem, 1);
		if (!removeTransaction.succeeded()) {
			pageManager.setPage(ref, store, Page.None);
			return;
		}

		ItemStack repairedItem = itemStack.withDurability(maxDurability);
		ItemStackSlotTransaction replaceTransaction = this.itemContext.getContainer().replaceItemStackInSlot(
			this.itemContext.getSlot(),
			itemStack, repairedItem
		);
		if (!replaceTransaction.succeeded()) {
			SimpleItemContainer.addOrDropItemStack(store, ref, heldContainer, heldSlot, heldItem.withQuantity(1));
			pageManager.setPage(ref, store, Page.None);
			return;
		}

		Message itemName = Message.translation(repairedItem.getItem().getTranslationKey());
		playerRef.sendMessage(Message.translation("server.general.repair.successful").param("itemName", itemName));
		pageManager.setPage(ref, store, Page.None);
		int soundEventIndex = SoundEvent.getAssetMap().getIndexOrDefault("SFX_Item_Repair", SoundEvent.EMPTY_ID);
		SoundUtil.playSoundEvent2d(ref, soundEventIndex, SoundCategory.UI, store);
	}
}
