package com.thescar.hygunsplugin.interactions.weapon;

import com.thescar.hygunsplugin.HygunsPluginMain;
import com.thescar.hygunsplugin.runtime.api.RuntimeWeaponStateAccess;
import com.thescar.hygunsplugin.runtime.components.AmmoDataComponent;
import com.thescar.hygunsplugin.support.interaction.*;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class SetAmmoInteraction extends SimpleInstantInteraction {
	public static final String KEY = HygunsPluginMain.key("SetAmmo");
	public static final BuilderCodec<SetAmmoInteraction> CODEC = InteractionChain
		.of(
			SetAmmoInteraction.class, SetAmmoInteraction::new,
			BuilderCodec.builder(SetAmmoInteraction.class, SetAmmoInteraction::new, SimpleInstantInteraction.CODEC)
		)
		.inheritedField("Amount", Codec.INTEGER, interaction -> interaction.amountValue)
		.inheritedField("ToMax", Codec.BOOLEAN, interaction -> interaction.toMaxValue)
		.documentation("Sets the held weapon ammo directly without touching inventory.").build();
	private final InteractionValue<Integer> amountValue = new InteractionValue<>(0);
	private final InteractionValue<Boolean> toMaxValue = new InteractionValue<>(false);

	public SetAmmoInteraction(String id) {
		super(id);
	}

	protected SetAmmoInteraction() {
	}

	@Nonnull
	@Override
	public WaitForDataFrom getWaitForDataFrom() {
		return WaitForDataFrom.Server;
	}

	@Override
	protected void firstRun(@Nonnull InteractionType interactionType, @Nonnull InteractionContext interactionContext,
	                        @Nonnull CooldownHandler cooldownHandler) {
		CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
		WeaponInteractionContext weaponContext = WeaponInteractionContext.resolve(interactionContext);
		if (weaponContext == null) {
			InteractionStateSupport.fail(interactionContext);
			return;
		}

		weaponContext.ensureTrackedHeldWeapon();
		RuntimeWeaponStateAccess.AmmoState state = weaponContext.ensureAmmoForConfiguredGun();
		AmmoDataComponent ammo = state.ammo();
		int maxAmmo = Math.max(0, ammo.maxAmmo());
		int requestedAmmo = resolveToMax()
		                    ? maxAmmo
		                    : Math.max(0, resolveAmount());
		ammo.setAmmo(Math.min(requestedAmmo, maxAmmo));
		HeldAmmoSyncOnCompletion.schedule(interactionContext, weaponContext.ref(), ammo);
		WeaponInteractionSupport.updateAmmoHud(commandBuffer, weaponContext.ref(), weaponContext.itemStack());
	}

	private int resolveAmount() {
		Integer amount = this.amountValue.get();
		return amount != null
		       ? amount
		       : 0;
	}

	private boolean resolveToMax() {
		return Boolean.TRUE.equals(this.toMaxValue.get());
	}
}
