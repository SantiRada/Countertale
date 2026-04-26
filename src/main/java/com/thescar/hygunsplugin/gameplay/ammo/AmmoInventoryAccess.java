package com.thescar.hygunsplugin.gameplay.ammo;

import com.thescar.hygunsplugin.content.ammo.AmmoDefinition;
import com.thescar.hygunsplugin.content.registry.AmmoRegistry;
import com.thescar.hygunsplugin.content.registry.GunRegistry;
import com.thescar.hygunsplugin.content.settings.GunSettings;
import com.thescar.hygunsplugin.content.settings.WeaponAmmoSettings;
import com.thescar.hygunsplugin.runtime.api.RuntimeWeaponStateAccess;
import com.thescar.hygunsplugin.runtime.components.AmmoDataComponent;
import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeEcs;
import com.thescar.hygunsplugin.runtime.item.core.RuntimeItemIdentity;
import com.thescar.hygunsplugin.runtime.item.core.RuntimeItemRef;
import com.thescar.hygunsplugin.runtime.persistence.RuntimeWeaponMetadataCommit;
import com.thescar.hygunsplugin.support.hytale.PlayerInventoryAccess;
import com.thescar.hygunsplugin.support.text.ValueUtils;

import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemStackItemContainer;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public final class AmmoInventoryAccess {
	private AmmoInventoryAccess() {
	}

	@Nullable
	public static CombinedItemContainer getAmmoContainer(@Nullable Player player) {
		if (player == null) {
			return null;
		}

		List<ItemContainer> containers = collectAmmoContainers(player);
		if (containers.isEmpty()) {
			return null;
		}

		return new CombinedItemContainer(containers.toArray(ItemContainer[]::new));
	}

	@Nullable
	public static String resolveAvailableAmmoItemId(@Nullable ItemStack gunStack, @Nullable Player player) {
		if (gunStack == null || player == null) {
			return null;
		}

		GunSettings settings = GunRegistry.getSettings(gunStack.getItemId());
		WeaponAmmoSettings weaponAmmo = settings != null
		                                ? settings.ammo()
		                                : null;
		CombinedItemContainer combined = getAmmoContainer(player);
		return resolvePreferredAmmoItemId(gunStack, weaponAmmo, combined);
	}

	public static int countAvailableAmmo(@Nullable ItemStack gunStack, @Nullable Player player) {
		if (gunStack == null || player == null) {
			return -1;
		}

		CombinedItemContainer combined = getAmmoContainer(player);
		String ammoItemId = resolveAvailableAmmoItemId(gunStack, player);
		return countAmmo(combined, ammoItemId);
	}

	@Nullable
	public static ItemStack persistAutoSelectedAmmoForHeldItem(@Nullable Player player, @Nullable ItemStack heldStack) {
		if (player == null || heldStack == null || heldStack.isEmpty()) {
			return heldStack;
		}

		GunSettings settings = GunRegistry.getSettings(heldStack.getItemId());
		WeaponAmmoSettings weaponAmmo = settings != null
		                                ? settings.ammo()
		                                : null;
		if (weaponAmmo == null) {
			return heldStack;
		}

		PlayerInventoryAccess.HeldItemLocation heldLocation = PlayerInventoryAccess.getHeldItemLocation(player);
		if (heldLocation == null) {
			return heldStack;
		}

		RuntimeWeaponStateAccess.AmmoState state = RuntimeWeaponStateAccess.ensureAmmoForConfiguredGun(heldLocation.stack(), settings);
		ItemStack currentStack = state.stack();
		AmmoDataComponent ammo = state.ammo();
		String preferredAmmoItemId = resolvePreferredAmmoItemId(currentStack, weaponAmmo, getAmmoContainer(player));
		boolean changed = false;
		String selectedAmmoItemId = ValueUtils.Checks.nonBlankOrNull(ammo.selectedAmmoItemId());
		String loadedAmmoItemId = ValueUtils.Checks.nonBlankOrNull(ammo.loadedAmmoItemId());
		if (preferredAmmoItemId == null) {
			return currentStack;
		}

		if (!preferredAmmoItemId.equalsIgnoreCase(selectedAmmoItemId)) {
			ammo.setSelectedAmmoItemId(preferredAmmoItemId);
			changed = true;
		}

		if (loadedAmmoItemId == null) {
			ammo.setLoadedAmmoItemId(preferredAmmoItemId);
			ammo.setLoadedAmmoIcon(resolveAmmoIcon(preferredAmmoItemId));
			changed = true;
		}

		if (!changed) {
			return currentStack;
		}

		var playerRef = PlayerInventoryAccess.getReference(player);
		if (playerRef == null) {
			return currentStack;
		}

		return commitHeldAmmoState(playerRef, heldLocation, currentStack, ammo);
	}

	@Nonnull
	private static ItemStack commitHeldAmmoState(@Nonnull Player player, @Nonnull PlayerInventoryAccess.HeldItemLocation heldLocation,
	                                             @Nonnull ItemStack currentStack, @Nonnull AmmoDataComponent ammo) {
		var playerRef = PlayerInventoryAccess.getReference(player);
		if (playerRef == null) {
			return currentStack;
		}

		return commitHeldAmmoState(playerRef, heldLocation, currentStack, ammo);
	}

	@Nonnull
	private static ItemStack commitHeldAmmoState(
		@Nonnull com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> playerRef,
		@Nonnull PlayerInventoryAccess.HeldItemLocation heldLocation, @Nonnull ItemStack currentStack, @Nonnull AmmoDataComponent ammo) {
		return RuntimeWeaponMetadataCommit.commitContainerAmmo(
			playerRef, heldLocation.container(), heldLocation.slot(),
			heldLocation.stack(), currentStack, ammo
		);
	}

	public static int removeAvailableAmmo(@Nullable ItemStack gunStack, @Nullable Player player, int quantity) {
		if (gunStack == null || player == null || quantity <= 0) {
			return 0;
		}

		CombinedItemContainer combined = getAmmoContainer(player);
		String ammoItemId = resolveAvailableAmmoItemId(gunStack, player);
		return removeAmmo(combined, ammoItemId, quantity);
	}

	public static int removeAmmo(@Nullable Player player, @Nullable String ammoItemId, int quantity,
	                             @Nullable InteractionContext interactionContext) {
		if (player == null) {
			return 0;
		}

		return removeAmmo(getAmmoContainer(player), ammoItemId, quantity, interactionContext);
	}

	public static int removeAmmo(@Nullable ItemContainer itemContainer, @Nullable String ammoItemId, int quantity,
	                             @Nullable InteractionContext interactionContext) {
		int removed = removeAmmo(itemContainer, ammoItemId, quantity);
		if (removed > 0 && interactionContext != null && interactionContext.getHeldItem() != null) {
			interactionContext.setHeldItem(interactionContext.getHeldItem());
		}

		return removed;
	}

	public static Map<String, AmmoDefinition> collectCompatibleAmmo(@Nullable ItemContainer itemContainer,
	                                                                @Nullable WeaponAmmoSettings weaponAmmo) {
		LinkedHashMap<String, AmmoDefinition> compatibleAmmo = new LinkedHashMap<>();
		if (itemContainer == null || weaponAmmo == null) {
			return compatibleAmmo;
		}

		for (short slot = 0; slot < itemContainer.getCapacity(); slot++) {
			ItemStack stack = itemContainer.getItemStack(slot);
			if (stack == null || stack.isEmpty() || stack.getQuantity() <= 0) {
				continue;
			}

			addCompatibleStack(compatibleAmmo, weaponAmmo, stack);
		}

		return compatibleAmmo;
	}

	public static Object2IntLinkedOpenHashMap<String> collectCompatibleAmmoQuantities(@Nullable ItemContainer itemContainer,
	                                                                                  @Nullable WeaponAmmoSettings weaponAmmo) {
		Object2IntLinkedOpenHashMap<String> quantities = new Object2IntLinkedOpenHashMap<>();
		if (itemContainer == null || weaponAmmo == null) {
			return quantities;
		}

		for (short slot = 0; slot < itemContainer.getCapacity(); slot++) {
			ItemStack stack = itemContainer.getItemStack(slot);
			if (stack == null || stack.isEmpty() || stack.getQuantity() <= 0) {
				continue;
			}

			addQuantity(quantities, weaponAmmo, stack);
		}

		return quantities;
	}

	@Nullable
	public static String resolvePreferredAmmoItemId(@Nullable ItemStack gunStack, @Nullable WeaponAmmoSettings weaponAmmo,
	                                                @Nullable ItemContainer itemContainer) {
		if (weaponAmmo == null) {
			return null;
		}

		String exactItemId = ValueUtils.Checks.nonBlankOrNull(weaponAmmo.itemId());
		if (exactItemId != null) {
			return exactItemId;
		}

		String selected = selectedAmmoItemId(gunStack);
		if (isCompatibleAmmoItemId(weaponAmmo, selected)) {
			return selected;
		}

		Map<String, AmmoDefinition> compatibleAmmo = collectCompatibleAmmo(itemContainer, weaponAmmo);
		if (compatibleAmmo.isEmpty()) {
			return null;
		}

		String loaded = loadedAmmoItemId(gunStack);
		if (loaded != null && compatibleAmmo.containsKey(loaded)) {
			return loaded;
		}

		return AmmoRegistry.resolveDefaultAmmoItemId(compatibleAmmo);
	}

	public static int countAmmo(@Nullable ItemContainer itemContainer, @Nullable String ammoItemId) {
		if (itemContainer == null) {
			return -1;
		}

		String normalizedItemId = ValueUtils.Checks.nonBlankOrNull(ammoItemId);
		if (normalizedItemId == null) {
			return -1;
		}

		int total = 0;
		for (short slot = 0; slot < itemContainer.getCapacity(); slot++) {
			ItemStack stack = itemContainer.getItemStack(slot);
			if (stack == null || stack.isEmpty()) {
				continue;
			}

			if (normalizedItemId.equalsIgnoreCase(stack.getItemId())) {
				total += Math.max(0, stack.getQuantity());
			}
		}

		return total;
	}

	public static int removeAmmo(@Nullable ItemContainer itemContainer, @Nullable String ammoItemId, int quantity) {
		if (itemContainer == null || quantity <= 0) {
			return 0;
		}

		String normalizedItemId = ValueUtils.Checks.nonBlankOrNull(ammoItemId);
		if (normalizedItemId == null) {
			return 0;
		}

		CombinedItemContainer combined = toCombinedAmmoContainer(itemContainer);
		if (combined == null) {
			return 0;
		}

		return removeFromSingleContainer(combined, normalizedItemId, quantity);
	}

	private static void addCompatibleStack(Map<String, AmmoDefinition> compatibleAmmo, WeaponAmmoSettings weaponAmmo, ItemStack stack) {
		String itemId = ValueUtils.Checks.nonBlankOrNull(stack.getItemId());
		if (itemId == null || compatibleAmmo.containsKey(itemId)) {
			return;
		}

		AmmoDefinition definition = AmmoRegistry.getAmmo(itemId);
		if (AmmoRegistry.isCompatible(weaponAmmo, definition)) {
			compatibleAmmo.put(itemId, definition);
		}
	}

	private static void addQuantity(Object2IntLinkedOpenHashMap<String> quantities, WeaponAmmoSettings weaponAmmo, ItemStack stack) {
		String itemId = ValueUtils.Checks.nonBlankOrNull(stack.getItemId());
		if (itemId == null) {
			return;
		}

		AmmoDefinition definition = AmmoRegistry.getAmmo(itemId);
		if (AmmoRegistry.isCompatible(weaponAmmo, definition)) {
			quantities.addTo(itemId, stack.getQuantity());
		}
	}

	@Nullable
	private static ItemContainer getNestedContainer(ItemContainer parentContainer, short slot, ItemStack stack) {
		if (!hasConfiguredItemContainer(stack)) {
			return null;
		}

		return ItemStackItemContainer.getContainer(parentContainer, slot);
	}

	private static boolean hasConfiguredItemContainer(@Nullable ItemStack stack) {
		if (stack == null || stack.isEmpty() || stack.getItem() == null) {
			return false;
		}

		var config = stack.getItem().getItemStackContainerConfig();
		return config != null && config.getCapacity() > 0;
	}

	private static boolean isCompatibleAmmoItemId(@Nullable WeaponAmmoSettings weaponAmmo, @Nullable String ammoItemId) {
		if (weaponAmmo == null) {
			return false;
		}

		String normalizedItemId = ValueUtils.Checks.nonBlankOrNull(ammoItemId);
		if (normalizedItemId == null) {
			return false;
		}

		AmmoDefinition definition = AmmoRegistry.getAmmo(normalizedItemId);
		return AmmoRegistry.isCompatible(weaponAmmo, definition);
	}

	@Nullable
	private static String resolveAmmoIcon(@Nullable String ammoItemId) {
		AmmoDefinition definition = AmmoRegistry.getAmmo(ammoItemId);
		if (definition == null || definition.settings() == null) {
			return null;
		}

		return ValueUtils.Checks.nonBlankOrNull(definition.settings().icon());
	}

	private static int removeFromSingleContainer(ItemContainer itemContainer, String ammoItemId, int quantity) {
		ItemStack request = new ItemStack(ammoItemId).withQuantity(quantity);
		var tx = itemContainer.removeItemStack(request, true, true);
		if (tx == null) {
			return 0;
		}

		ItemStack remainder = tx.getRemainder();
		int remaining = remainder != null && !remainder.isEmpty()
		                ? remainder.getQuantity()
		                : 0;
		return Math.max(0, quantity - remaining);
	}

	@Nullable
	private static CombinedItemContainer toCombinedAmmoContainer(@Nullable ItemContainer rootContainer) {
		if (rootContainer == null) {
			return null;
		}

		List<ItemContainer> flattened = new ArrayList<>();
		Set<ItemContainer> visited = Collections.newSetFromMap(new IdentityHashMap<>());
		collectAmmoContainersRecursive(rootContainer, flattened, visited);
		if (flattened.isEmpty()) {
			return null;
		}

		return new CombinedItemContainer(flattened.toArray(ItemContainer[]::new));
	}

	private static List<ItemContainer> collectAmmoContainers(@Nonnull Player player) {
		List<ItemContainer> flattened = new ArrayList<>();
		Set<ItemContainer> visited = Collections.newSetFromMap(new IdentityHashMap<>());
		collectAmmoContainersRecursive(PlayerInventoryAccess.getBackpack(player), flattened, visited);
		collectAmmoContainersRecursive(PlayerInventoryAccess.getStorage(player), flattened, visited);
		collectAmmoContainersRecursive(PlayerInventoryAccess.getHotbar(player), flattened, visited);
		collectAmmoContainersRecursive(PlayerInventoryAccess.getUtility(player), flattened, visited);
		return flattened;
	}

	private static void collectAmmoContainersRecursive(@Nullable ItemContainer container, List<ItemContainer> out,
	                                                   Set<ItemContainer> visited) {
		if (container == null || !visited.add(container)) {
			return;
		}

		out.add(container);
		for (short slot = 0; slot < container.getCapacity(); slot++) {
			ItemStack stack = container.getItemStack(slot);
			if (stack == null || stack.isEmpty() || stack.getQuantity() <= 0) {
				continue;
			}

			ItemContainer nested = getNestedContainer(container, slot, stack);
			if (nested != null) {
				collectAmmoContainersRecursive(nested, out, visited);
			}
		}
	}

	@Nullable
	private static String selectedAmmoItemId(@Nullable ItemStack gunStack) {
		RuntimeItemRef ref = RuntimeItemIdentity.resolve(gunStack);
		AmmoDataComponent ammo = ItemRuntimeEcs.getComponent(ref, AmmoDataComponent.getComponentType());
		if (ammo != null && ammo.initialized()) {
			return ammo.selectedAmmoItemId();
		}

		return null;
	}

	@Nullable
	private static String loadedAmmoItemId(@Nullable ItemStack gunStack) {
		RuntimeItemRef ref = RuntimeItemIdentity.resolve(gunStack);
		AmmoDataComponent ammo = ItemRuntimeEcs.getComponent(ref, AmmoDataComponent.getComponentType());
		if (ammo != null && ammo.initialized()) {
			return ammo.loadedAmmoItemId();
		}

		return null;
	}
}
