package com.thescar.hygunsplugin.interactions.weapon;

import com.thescar.hygunsplugin.HygunsPluginMain;
import com.thescar.hygunsplugin.content.registry.GunRegistry;
import com.thescar.hygunsplugin.content.settings.GunSettings;
import com.thescar.hygunsplugin.runtime.api.RuntimeWeaponStateAccess;
import com.thescar.hygunsplugin.runtime.components.AmmoDataComponent;
import com.thescar.hygunsplugin.support.interaction.InteractionChain;
import com.thescar.hygunsplugin.support.interaction.InteractionStateSupport;
import com.thescar.hygunsplugin.support.interaction.WeaponInteractionContext;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;

import javax.annotation.Nonnull;

public class GunValidationInteraction extends SimpleInstantInteraction {
	public static final String KEY = HygunsPluginMain.key("ShootCheck");
	public static final BuilderCodec<GunValidationInteraction> CODEC = InteractionChain
		.of(
			GunValidationInteraction.class, GunValidationInteraction::new,
			BuilderCodec.builder(GunValidationInteraction.class, GunValidationInteraction::new, SimpleInstantInteraction.CODEC)
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

		GunSettings settings = GunRegistry.getSettings(weaponContext.ensureTrackedHeldWeapon().getItemId());
		RuntimeWeaponStateAccess.AmmoState state = weaponContext.ensureAmmoForInteraction(
			RuntimeWeaponStateAccess.resolveMaxAmmo(settings != null
			                                        ? settings.ammo()
			                                        : null)
		);
		AmmoDataComponent ammo = state.ammo();
		if (!ammo.initialized() || ammo.effectiveAmmo() <= 0) {
			InteractionStateSupport.fail(interactionContext);
		}
	}
}
