package com.thescar.hygunsplugin.interactions.weapon;

import com.thescar.hygunsplugin.HygunsPluginMain;
import com.thescar.hygunsplugin.debug.DebugLogger;
import com.thescar.hygunsplugin.gameplay.ammo.AmmoInventoryAccess;
import com.thescar.hygunsplugin.runtime.api.RuntimeWeaponStateAccess;
import com.thescar.hygunsplugin.runtime.components.AmmoDataComponent;
import com.thescar.hygunsplugin.support.interaction.HeldAmmoSyncOnCompletion;
import com.thescar.hygunsplugin.support.interaction.InteractionStateSupport;
import com.thescar.hygunsplugin.support.interaction.InteractionValue;
import com.thescar.hygunsplugin.support.interaction.WeaponInteractionContext;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;

import javax.annotation.Nonnull;

public class UseAmmoInteraction extends SimpleInstantInteraction {
	public static final String KEY = HygunsPluginMain.key("UseAmmo");
	public static final BuilderCodec<UseAmmoInteraction> CODEC = BuilderCodec
		.builder(UseAmmoInteraction.class, UseAmmoInteraction::new, SimpleInstantInteraction.CODEC)
		.documentation("Consumes ammo from the held item without firing a Hyguns projectile.")
		.appendInherited(
			new KeyedCodec<>("Amount", Codec.INTEGER), (interaction, value) -> interaction.amountValue.set(value),
			interaction -> interaction.amountValue.get(), (interaction, parent) -> interaction.amountValue.set(parent.amountValue.get())
		)
		.add()
		.appendInherited(
			new KeyedCodec<>("Available", Codec.BOOLEAN), (interaction, value) -> interaction.availableValue.set(value),
			interaction -> interaction.availableValue.get(),
			(interaction, parent) -> interaction.availableValue.set(parent.availableValue.get())
		)
		.add()
		.build();
	private final InteractionValue<Integer> amountValue = new InteractionValue<>(1);
	private final InteractionValue<Boolean> availableValue = new InteractionValue<>(false);

	public UseAmmoInteraction(String id) {
		super(id);
	}

	protected UseAmmoInteraction() {
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

		Player player = weaponContext.player();
		ItemStack itemStack = weaponContext.ensureTrackedHeldWeapon();
		int amount = resolveAmount();
		boolean available = Boolean.TRUE.equals(this.availableValue.get());
		DebugLogger.debug(
			"UseAmmo", () -> "Start heldItem=" + itemStack.getItemId()
				+ ", amount=" + amount
				+ ", available=" + available
		);
		if (available) {
			int availableAmmo = AmmoInventoryAccess.countAvailableAmmo(itemStack, player);
			DebugLogger.debug("UseAmmo", () -> "Available ammo before remove=" + availableAmmo + ", required=" + amount);
			if (availableAmmo < amount) {
				InteractionStateSupport.fail(interactionContext);
				return;
			}

			int removed = AmmoInventoryAccess.removeAvailableAmmo(itemStack, player, amount);
			DebugLogger.debug("UseAmmo", () -> "Removed available ammo=" + removed + "/" + amount);
			if (removed < amount) {
				InteractionStateSupport.fail(interactionContext);
				return;
			}

			return;
		}

		RuntimeWeaponStateAccess.AmmoState state = weaponContext.ensureAmmoForConfiguredGun();
		AmmoDataComponent ammo = state.ammo();
		DebugLogger.debug(
			"UseAmmo", () -> "Loaded ammo before consume: initialized=" + ammo.initialized()
				+ ", current=" + ammo.effectiveAmmo()
				+ ", max=" + ammo.maxAmmo()
		);
		if (!ammo.initialized() || ammo.effectiveAmmo() < amount) {
			DebugLogger.debug("UseAmmo", () -> "FAILED BECAUSE AMMO NOT INITIALIZED OR EFFECTIVE AMMO < AMOUNT");
			InteractionStateSupport.fail(interactionContext);
			return;
		}

		if (!ammo.consume(amount)) {
			DebugLogger.debug("UseAmmo", () -> "FAILED BECAUSE AMMO NOT CONSUMED");
			InteractionStateSupport.fail(interactionContext);
			return;
		}

		HeldAmmoSyncOnCompletion.schedule(interactionContext, weaponContext.ref(), ammo);
		DebugLogger.debug("UseAmmo", () -> "Loaded ammo after consume: current=" + ammo.effectiveAmmo());
	}

	private int resolveAmount() {
		Integer amount = this.amountValue.get();
		return amount != null && amount > 0
		       ? amount
		       : 1;
	}
}
