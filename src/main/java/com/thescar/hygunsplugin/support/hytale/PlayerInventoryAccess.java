package com.thescar.hygunsplugin.support.hytale;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;

public final class PlayerInventoryAccess {
	private PlayerInventoryAccess() {
	}

	@Nullable
	public static ItemStack getItemInHand(@Nullable Player player) {
		Ref<EntityStore> ref = getReference(player);
		Store<EntityStore> store = getStore(player);
		if (ref == null || store == null) {
			return null;
		}

		return InventoryComponent.getItemInHand(store, ref);
	}

	@Nullable
	public static HeldItemLocation getHeldItemLocation(@Nullable Player player) {
		HeldItemLocation location = getActiveLocation(player, InventoryComponent.HOTBAR_SECTION_ID);
		if (location != null) {
			return location;
		}

		location = getActiveLocation(player, InventoryComponent.UTILITY_SECTION_ID);
		if (location != null) {
			return location;
		}

		return getActiveLocation(player, InventoryComponent.TOOLS_SECTION_ID);
	}

	@Nullable
	public static ItemContainer getSection(@Nullable Player player, int sectionId) {
		ComponentType<EntityStore, ? extends InventoryComponent> componentType = InventoryComponent.getComponentTypeById(sectionId);
		return componentType == null
		       ? null
		       : getSection(player, componentType);
	}

	@Nullable
	public static ItemContainer getHotbar(@Nullable Player player) {
		return getSection(player, InventoryComponent.Hotbar.getComponentType());
	}

	@Nullable
	public static ItemContainer getUtility(@Nullable Player player) {
		return getSection(player, InventoryComponent.Utility.getComponentType());
	}

	@Nullable
	public static ItemContainer getStorage(@Nullable Player player) {
		return getSection(player, InventoryComponent.Storage.getComponentType());
	}

	@Nullable
	public static ItemContainer getBackpack(@Nullable Player player) {
		return getSection(player, InventoryComponent.Backpack.getComponentType());
	}

	@Nullable
	public static ItemContainer getArmor(@Nullable Player player) {
		return getSection(player, InventoryComponent.Armor.getComponentType());
	}

	@Nullable
	public static ItemContainer getTools(@Nullable Player player) {
		return getSection(player, InventoryComponent.Tool.getComponentType());
	}

	public static byte getActiveSlot(@Nullable Player player, int sectionId) {
		if (sectionId == InventoryComponent.HOTBAR_SECTION_ID) {
			InventoryComponent.Hotbar hotbar = getComponent(player, InventoryComponent.Hotbar.getComponentType());
			return hotbar != null
			       ? hotbar.getActiveSlot()
			       : InventoryComponent.INACTIVE_SLOT_INDEX;
		}

		if (sectionId == InventoryComponent.UTILITY_SECTION_ID) {
			InventoryComponent.Utility utility = getComponent(player, InventoryComponent.Utility.getComponentType());
			return utility != null
			       ? utility.getActiveSlot()
			       : InventoryComponent.INACTIVE_SLOT_INDEX;
		}

		if (sectionId == InventoryComponent.TOOLS_SECTION_ID) {
			InventoryComponent.Tool tools = getComponent(player, InventoryComponent.Tool.getComponentType());
			return tools != null
			       ? tools.getActiveSlot()
			       : InventoryComponent.INACTIVE_SLOT_INDEX;
		}

		return InventoryComponent.INACTIVE_SLOT_INDEX;
	}

	@SafeVarargs
	@Nullable
	public static CombinedItemContainer getCombined(@Nullable Player player,
	                                                ComponentType<EntityStore, ? extends InventoryComponent>... componentTypes) {
		Ref<EntityStore> ref = getReference(player);
		Store<EntityStore> store = getStore(player);
		if (ref == null || store == null) {
			return null;
		}

		return InventoryComponent.getCombined(store, ref, componentTypes);
	}

	@Nullable
	public static CombinedItemContainer getCombinedArmorHotbarUtilityStorage(@Nullable Player player) {
		return getCombined(player, InventoryComponent.ARMOR_HOTBAR_UTILITY_STORAGE);
	}

	@Nullable
	public static CombinedItemContainer getCombinedBackpackStorageHotbar(@Nullable Player player) {
		return getCombined(player, InventoryComponent.BACKPACK_STORAGE_HOTBAR);
	}

	@Nullable
	public static Ref<EntityStore> getReference(@Nullable Player player) {
		if (player == null) {
			return null;
		}

		Ref<EntityStore> ref = player.getReference();
		return ref != null && ref.isValid()
		       ? ref
		       : null;
	}

	@Nullable
	public static Store<EntityStore> getStore(@Nullable Player player) {
		Ref<EntityStore> ref = getReference(player);
		return ref != null
		       ? ref.getStore()
		       : null;
	}

	@Nullable
	public static <T extends InventoryComponent> T getComponent(@Nullable Player player, ComponentType<EntityStore, T> componentType) {
		Ref<EntityStore> ref = getReference(player);
		Store<EntityStore> store = getStore(player);
		if (ref == null || store == null || componentType == null) {
			return null;
		}

		return store.getComponent(ref, componentType);
	}

	@Nullable
	private static ItemContainer getSection(@Nullable Player player,
	                                        ComponentType<EntityStore, ? extends InventoryComponent> componentType) {
		InventoryComponent component = getComponent(player, componentType);
		return component != null
		       ? component.getInventory()
		       : null;
	}

	@Nullable
	private static HeldItemLocation getActiveLocation(@Nullable Player player, int sectionId) {
		ItemContainer container = getSection(player, sectionId);
		if (container == null) {
			return null;
		}

		byte activeSlot = getActiveSlot(player, sectionId);
		if (activeSlot == InventoryComponent.INACTIVE_SLOT_INDEX || activeSlot < 0 || activeSlot >= container.getCapacity()) {
			return null;
		}

		ItemStack stack = container.getItemStack(activeSlot);
		if (stack == null || stack.isEmpty()) {
			return null;
		}

		return new HeldItemLocation(sectionId, container, activeSlot, stack);
	}

	public record HeldItemLocation(int sectionId, ItemContainer container, short slot, ItemStack stack) {
	}
}
