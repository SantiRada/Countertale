package com.thescar.hygunsplugin.interactions.weapon;

import com.thescar.hygunsplugin.HygunsPluginMain;
import com.thescar.hygunsplugin.content.registry.GunRegistry;
import com.thescar.hygunsplugin.content.settings.GunSettings;
import com.thescar.hygunsplugin.content.settings.WeaponAmmoSettings;
import com.thescar.hygunsplugin.gameplay.ammo.AmmoInventoryAccess;
import com.thescar.hygunsplugin.gameplay.reload.ReloadManager;
import com.thescar.hygunsplugin.runtime.api.RuntimeWeaponStateAccess;
import com.thescar.hygunsplugin.runtime.components.AmmoDataComponent;
import com.thescar.hygunsplugin.support.interaction.InteractionChain;
import com.thescar.hygunsplugin.support.interaction.InteractionStateSupport;
import com.thescar.hygunsplugin.support.interaction.WeaponInteractionContext;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ReloadCheckInteraction extends SimpleInstantInteraction {
	public static final String KEY = HygunsPluginMain.key("ReloadCheck");
	public static final BuilderCodec<ReloadCheckInteraction> CODEC = InteractionChain
		.of(
			ReloadCheckInteraction.class, ReloadCheckInteraction::new,
			BuilderCodec.builder(ReloadCheckInteraction.class, ReloadCheckInteraction::new, SimpleInstantInteraction.CODEC)
		)
		.build();

	@Nonnull
	@Override

	public WaitForDataFrom getWaitForDataFrom() {
		return WaitForDataFrom.Server;
	}

	@Override
	protected void firstRun(@Nonnull InteractionType interactionType, @Nonnull InteractionContext interactionContext,
	                        @Nonnull CooldownHandler cooldownHandler) {
		WeaponInteractionContext weaponContext = WeaponInteractionContext.resolve(interactionContext);
		if (weaponContext == null) {
			InteractionStateSupport.fail(interactionContext);
			return;
		}

		var player = weaponContext.player();
		if (com.thescar.hygunsplugin.support.hytale.PlayerInventoryAccess.getReference(player) == null) {
			InteractionStateSupport.fail(interactionContext);
			return;
		}

		PlayerRef playerRef = weaponContext.playerRef();
		if (playerRef == null) {
			InteractionStateSupport.fail(interactionContext);
			return;
		}

		if (ReloadManager.isReloading(playerRef)) {
			InteractionStateSupport.fail(interactionContext);
			return;
		}

		ItemStack held = weaponContext.ensureTrackedHeldWeapon();
		GunSettings settings = GunRegistry.getSettings(held.getItemId());
		WeaponAmmoSettings weaponAmmo = settings != null
		                                ? settings.ammo()
		                                : null;
		var ensured = weaponContext.ensureAmmoForReloadApply(
			RuntimeWeaponStateAccess.resolveMaxAmmo(weaponAmmo)
		);
		held = weaponContext.itemStack();

		AmmoDataComponent ammo = ensured.ammo();
		if (!ammo.initialized()) {
			InteractionStateSupport.fail(interactionContext);
			return;
		}

		if (ammo.ammo() >= ammo.maxAmmo()) {
			InteractionStateSupport.fail(interactionContext);
			return;
		}

		CombinedItemContainer combined = AmmoInventoryAccess.getAmmoContainer(player);
		if (combined == null) {
			InteractionStateSupport.fail(interactionContext);
			return;
		}

		String ammoItemId = resolveAmmoItemId(held, weaponAmmo, combined);
		if (ammoItemId == null) {
			InteractionStateSupport.fail(interactionContext);
			return;
		}

		if (AmmoInventoryAccess.countAmmo(combined, ammoItemId) <= 0) {
			InteractionStateSupport.fail(interactionContext);
		}
	}

	@Nullable
	private String resolveAmmoItemId(@Nonnull ItemStack gunStack, @Nullable WeaponAmmoSettings weaponAmmo,
	                                 @Nonnull CombinedItemContainer combined) {
		return AmmoInventoryAccess.resolvePreferredAmmoItemId(gunStack, weaponAmmo, combined);
	}
}
