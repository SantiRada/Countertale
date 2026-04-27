package com.thescar.hygunsplugin.runtime.api;

import com.thescar.hygunsplugin.content.registry.GunRegistry;
import com.thescar.hygunsplugin.content.settings.GunSettings;
import com.thescar.hygunsplugin.content.settings.WeaponAmmoSettings;
import com.thescar.hygunsplugin.runtime.components.AmmoDataComponent;
import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeEcs;
import com.thescar.hygunsplugin.runtime.item.core.RuntimeItemIdentity;
import com.thescar.hygunsplugin.runtime.item.core.RuntimeItemRef;
import com.thescar.hygunsplugin.runtime.persistence.RuntimeWeaponPersistence;

import com.hypixel.hytale.server.core.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class RuntimeWeaponStateAccess {
	private RuntimeWeaponStateAccess() {
	}

	@Nonnull
	public static TrackedItem ensureTracked(@Nonnull ItemStack itemStack) {
		RuntimeItemIdentity.Assignment assignment = RuntimeWeaponIdentitySupport.ensureTrackableWeapon(itemStack);
		return new TrackedItem(assignment.stack(), assignment.ref(), assignment.created());
	}

	@Nullable
	public static RuntimeItemRef resolve(@Nullable ItemStack itemStack) {
		return RuntimeItemIdentity.resolve(itemStack);
	}

	@Nonnull
	public static AmmoState ensureAmmoForConfiguredGun(@Nonnull ItemStack itemStack) {
		GunSettings settings = GunRegistry.getSettings(itemStack.getItemId());
		return ensureAmmoForConfiguredGun(itemStack, settings);
	}

	@Nonnull
	public static AmmoState ensureAmmoForConfiguredGun(@Nonnull ItemStack itemStack, @Nullable GunSettings settings) {
		TrackedItem tracked = ensureTracked(itemStack);
		AmmoDataComponent ammo = ItemRuntimeEcs.ensureComponent(tracked.ref(), AmmoDataComponent.getComponentType());
		WeaponAmmoSettings ammoSettings = settings != null
		                                  ? settings.ammo()
		                                  : null;
		if (!ammo.initialized() && ammoSettings != null) {
			bootstrapAmmo(ammo, tracked.stack(), resolveMaxAmmo(ammoSettings), true);
		}

		return new AmmoState(tracked.stack(), tracked.ref(), tracked.created(), ammo);
	}

	@Nonnull
	public static AmmoState ensureAmmoForInteraction(@Nonnull ItemStack itemStack, int maxAmmo) {
		TrackedItem tracked = ensureTracked(itemStack);
		AmmoDataComponent ammo = ItemRuntimeEcs.ensureComponent(tracked.ref(), AmmoDataComponent.getComponentType());
		if (!ammo.initialized()) {
			bootstrapAmmo(ammo, tracked.stack(), maxAmmo, true);
		}

		return new AmmoState(tracked.stack(), tracked.ref(), tracked.created(), ammo);
	}

	@Nonnull
	public static AmmoState ensureAmmoForInteraction(@Nonnull ItemStack itemStack, @Nullable GunSettings settings) {
		return ensureAmmoForInteraction(itemStack, resolveMaxAmmo(settings));
	}

	@Nonnull
	public static AmmoState ensureAmmoForReloadApply(@Nonnull ItemStack itemStack, int maxAmmo) {
		TrackedItem tracked = ensureTracked(itemStack);
		AmmoDataComponent ammo = ItemRuntimeEcs.ensureComponent(tracked.ref(), AmmoDataComponent.getComponentType());
		if (!ammo.initialized()) {
			bootstrapAmmo(ammo, tracked.stack(), maxAmmo, false);
		}

		return new AmmoState(tracked.stack(), tracked.ref(), tracked.created(), ammo);
	}

	public static int resolveMaxAmmo(@Nullable GunSettings settings) {
		WeaponAmmoSettings ammo = settings != null
		                          ? settings.ammo()
		                          : null;
		return resolveMaxAmmo(ammo);
	}

	public static int resolveMaxAmmo(@Nullable WeaponAmmoSettings ammoSettings) {
		Integer capacity = ammoSettings != null
		                   ? ammoSettings.capacity()
		                   : null;
		return capacity != null && capacity > 0
		       ? capacity
		       : 1;
	}

	private static void bootstrapAmmo(@Nonnull AmmoDataComponent component, @Nonnull ItemStack stack, int configuredMaxAmmo,
	                                  boolean grantInitialMagazineIfMissing) {
		RuntimeWeaponPersistence.PersistedAmmoState persisted = RuntimeWeaponPersistence.readAmmo(stack);
		if (persisted != null) {
			RuntimeWeaponPersistence.apply(component, persisted);
			if (component.maxAmmo() <= 0) {
				component.setMaxAmmo(Math.max(1, configuredMaxAmmo));
			}

			return;
		}

		int maxAmmo = Math.max(1, configuredMaxAmmo);
		int ammo = grantInitialMagazineIfMissing
		           ? maxAmmo
		           : 0;
		component.setAmmo(ammo);
		component.setMaxAmmo(maxAmmo);
		component.setInitialized(true);
		component.setSelectedAmmoItemId(null);
		component.setLoadedAmmoItemId(null);
		component.setLoadedAmmoIcon(null);
		component.clearDirty();
	}

	public record TrackedItem(@Nonnull ItemStack stack, @Nonnull RuntimeItemRef ref, boolean created) {
	}

	public record AmmoState(
		@Nonnull ItemStack stack, @Nonnull RuntimeItemRef ref, boolean created, @Nonnull AmmoDataComponent ammo
	) {
	}

}
