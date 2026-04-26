package com.thescar.hygunsplugin.interactions.weapon;

import com.thescar.hygunsplugin.HygunsPluginMain;
import com.thescar.hygunsplugin.runtime.api.RuntimeWeaponStateAccess;
import com.thescar.hygunsplugin.runtime.persistence.RuntimeWeaponMetadataCommit;
import com.thescar.hygunsplugin.support.interaction.InteractionStateSupport;
import com.thescar.hygunsplugin.support.interaction.WeaponInteractionContext;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;

import javax.annotation.Nonnull;

public class SyncAmmoInteraction extends SimpleInstantInteraction {
	public static final String KEY = HygunsPluginMain.key("SyncAmmo");
	public static final BuilderCodec<SyncAmmoInteraction> CODEC = BuilderCodec
		.builder(SyncAmmoInteraction.class, SyncAmmoInteraction::new, SimpleInstantInteraction.CODEC)
		.documentation("Commits the current held weapon ammo runtime state into item metadata.")
		.build();

	public SyncAmmoInteraction(String id) {
		super(id);
		this.cancelOnItemChange = false;
	}

	protected SyncAmmoInteraction() {
		this.cancelOnItemChange = false;
	}

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

		ItemStack currentStack = weaponContext.ensureTrackedHeldWeapon();
		RuntimeWeaponStateAccess.AmmoState ammoState = weaponContext.ensureAmmoForConfiguredGun();
		RuntimeWeaponMetadataCommit.commitHeldAmmo(
			interactionContext,
			weaponContext.ref(),
			currentStack,
			ammoState.stack(),
			ammoState.ammo()
		);
	}
}
