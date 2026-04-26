package com.thescar.hygunsplugin.interactions.weapon;

import com.thescar.hygunsplugin.HygunsPluginMain;
import com.thescar.hygunsplugin.debug.DebugLogger;
import com.thescar.hygunsplugin.runtime.components.AmmoDataComponent;
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

public class CheckAmmoInteraction extends SimpleInstantInteraction {
	public static final String KEY = HygunsPluginMain.key("CheckAmmo");
	public static final BuilderCodec<CheckAmmoInteraction> CODEC = BuilderCodec
		.builder(CheckAmmoInteraction.class, CheckAmmoInteraction::new, SimpleInstantInteraction.CODEC)
		.documentation("Checks held-item ammo and branches to Next or Failed.")
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
		.add().build();
	private final InteractionValue<Integer> amountValue = new InteractionValue<>(1);
	private final InteractionValue<Boolean> availableValue = new InteractionValue<>(false);

	public CheckAmmoInteraction(String id) {
		super(id);
	}

	protected CheckAmmoInteraction() {
	}

	private static int resolveLoadedAmmo(@Nonnull WeaponInteractionContext weaponContext) {
		weaponContext.ensureTrackedHeldWeapon();
		AmmoDataComponent ammo = weaponContext.ensureAmmoForConfiguredGun().ammo();
		if (!ammo.initialized()) {
			return -1;
		}

		return ammo.effectiveAmmo();
	}

	private static int resolveAvailableAmmo(@Nonnull Player player, @Nonnull ItemStack itemStack) {
		return com.thescar.hygunsplugin.gameplay.ammo.AmmoInventoryAccess.countAvailableAmmo(itemStack, player);
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

		int amount = resolveAmount();
		boolean available = Boolean.TRUE.equals(this.availableValue.get());
		int ammo = available
		           ? resolveAvailableAmmo(weaponContext.player(), weaponContext.itemStack())
		           : resolveLoadedAmmo(weaponContext);
		DebugLogger.debug(
			"CheckAmmo", () -> "heldItem=" + weaponContext.itemStack().getItemId()
				+ ", available=" + available
				+ ", required=" + amount
				+ ", actual=" + ammo
		);
		if (ammo >= amount) {
			InteractionStateSupport.finish(interactionContext);
			return;
		}

		InteractionStateSupport.fail(interactionContext);
	}

	private int resolveAmount() {
		Integer amount = this.amountValue.get();
		return amount != null && amount > 0
		       ? amount
		       : 1;
	}
}
