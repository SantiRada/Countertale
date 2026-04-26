package com.thescar.hygunsplugin.runtime.api;

import com.thescar.hygunsplugin.runtime.components.AmmoDataComponent;
import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeEcs;
import com.thescar.hygunsplugin.runtime.item.core.RuntimeItemIdentity;
import com.thescar.hygunsplugin.runtime.item.core.RuntimeItemRef;
import com.thescar.hygunsplugin.runtime.persistence.RuntimeWeaponMetadataCommit;
import com.thescar.hygunsplugin.runtime.persistence.RuntimeWeaponPersistence;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class WeaponRuntimeApi {
	private WeaponRuntimeApi() {
	}

	@Nullable
	public static RuntimeItemRef resolve(@Nullable ItemStack itemStack) {
		return RuntimeWeaponStateAccess.resolve(itemStack);
	}

	@Nonnull
	public static RuntimeWeaponStateAccess.AmmoState ensureAmmoForConfiguredGun(@Nonnull ItemStack itemStack) {
		return RuntimeWeaponStateAccess.ensureAmmoForConfiguredGun(itemStack);
	}

	@Nonnull
	public static RuntimeWeaponStateAccess.AmmoState ensureAmmoForConfiguredGun(@Nonnull ItemStack itemStack,
	                                                                            @Nullable com.thescar.hygunsplugin.content.settings.GunSettings settings) {
		return RuntimeWeaponStateAccess.ensureAmmoForConfiguredGun(itemStack, settings);
	}

	@Nonnull
	public static RuntimeWeaponStateAccess.AmmoState ensureAmmoForReloadApply(@Nonnull ItemStack itemStack, int maxAmmo) {
		return RuntimeWeaponStateAccess.ensureAmmoForReloadApply(itemStack, maxAmmo);
	}

	@Nullable
	public static AmmoDataComponent ammo(@Nullable ItemStack itemStack) {
		RuntimeItemRef ref = RuntimeItemIdentity.resolve(itemStack);
		return ItemRuntimeEcs.getComponent(ref, AmmoDataComponent.getComponentType());
	}

	@Nonnull
	public static ItemStack commitHeldAmmo(@Nonnull InteractionContext interactionContext, @Nonnull Ref<EntityStore> ref,
	                                       @Nonnull ItemStack previousStack, @Nonnull ItemStack currentStack, @Nonnull AmmoDataComponent ammo) {
		return RuntimeWeaponMetadataCommit.commitHeldAmmo(interactionContext, ref, previousStack, currentStack, ammo);
	}

	@Nonnull
	public static ItemStack commitContainerAmmo(@Nonnull Ref<EntityStore> ref, @Nonnull ItemContainer container, short slot,
	                                            @Nonnull ItemStack previousStack, @Nonnull ItemStack currentStack, @Nonnull AmmoDataComponent ammo) {
		return RuntimeWeaponMetadataCommit.commitContainerAmmo(ref, container, slot, previousStack, currentStack, ammo);
	}

	@Nullable
	public static RuntimeWeaponPersistence.PersistedAmmoState readPersistedAmmo(@Nullable ItemStack stack) {
		return RuntimeWeaponPersistence.readAmmo(stack);
	}
}
