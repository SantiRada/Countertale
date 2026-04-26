package com.thescar.hygunsplugin.interactions.weapon;

import com.thescar.hygunsplugin.HygunsPluginMain;
import com.thescar.hygunsplugin.runtime.api.RuntimeItems;
import com.thescar.hygunsplugin.runtime.components.FireDelayComponent;
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

public class CanShootInteraction extends SimpleInstantInteraction {
	public static final String KEY = HygunsPluginMain.key("CanShoot");
	public static final BuilderCodec<CanShootInteraction> CODEC = InteractionChain.of(
		CanShootInteraction.class, CanShootInteraction::new,
		BuilderCodec.builder(CanShootInteraction.class, CanShootInteraction::new, SimpleInstantInteraction.CODEC)
	).build();

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

		RuntimeItems.RuntimeItemHandle runtime = RuntimeItems.resolve(weaponContext.ensureTrackedHeldWeapon());
		if (runtime == null) {
			InteractionStateSupport.fail(interactionContext);
			return;
		}

		FireDelayComponent fireDelay = runtime.fireDelay();
		if (fireDelay != null && fireDelay.blocksAt(System.currentTimeMillis())) {
			InteractionStateSupport.fail(interactionContext);
		}
	}
}
