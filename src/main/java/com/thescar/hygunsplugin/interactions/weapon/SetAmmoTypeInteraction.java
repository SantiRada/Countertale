package com.thescar.hygunsplugin.interactions.weapon;

import com.thescar.hygunsplugin.HygunsPluginMain;
import com.thescar.hygunsplugin.content.ammo.AmmoDefinition;
import com.thescar.hygunsplugin.content.registry.AmmoRegistry;
import com.thescar.hygunsplugin.content.registry.GunRegistry;
import com.thescar.hygunsplugin.content.settings.AmmoItemSettings;
import com.thescar.hygunsplugin.content.settings.GunSettings;
import com.thescar.hygunsplugin.debug.DebugLogger;
import com.thescar.hygunsplugin.runtime.api.RuntimeWeaponStateAccess;
import com.thescar.hygunsplugin.runtime.components.AmmoDataComponent;
import com.thescar.hygunsplugin.support.interaction.*;
import com.thescar.hygunsplugin.support.text.StringUtil;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SetAmmoTypeInteraction extends SimpleInstantInteraction {
	public static final String KEY = HygunsPluginMain.key("SetAmmoType");
	public static final BuilderCodec<SetAmmoTypeInteraction> CODEC = InteractionChain
		.of(
			SetAmmoTypeInteraction.class, SetAmmoTypeInteraction::new,
			BuilderCodec.builder(SetAmmoTypeInteraction.class, SetAmmoTypeInteraction::new, SimpleInstantInteraction.CODEC)
		)
		.inheritedField("AmmoItemId", Codec.STRING, interaction -> interaction.ammoItemIdValue)
		.documentation("Sets loaded and selected ammo type on the held weapon from the ammo registry if it is compatible.")
		.build();
	private final InteractionValue<String> ammoItemIdValue = new InteractionValue<>("");

	public SetAmmoTypeInteraction(String id) {
		super(id);
	}

	protected SetAmmoTypeInteraction() {
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

		ResolvedAmmo resolvedAmmo = resolveAmmo();
		if (resolvedAmmo == null) {
			DebugLogger.debug("SetAmmoType", "Failed to resolve ammo from registry");
			InteractionStateSupport.fail(interactionContext);
			return;
		}

		ItemStack itemStack = weaponContext.itemStack();
		GunSettings gunSettings = GunRegistry.getSettings(itemStack.getItemId());
		DebugLogger.debug(
			"SetAmmoType", () -> "heldItem=" + itemStack.getItemId()
				+ ", targetAmmo=" + resolvedAmmo.itemId()
		);
		if (!AmmoRegistry.isCompatible(
			gunSettings != null
			? gunSettings.ammo()
			: null, resolvedAmmo.definition()
		)) {
			DebugLogger.debug("SetAmmoType", "Ammo is incompatible with held weapon");
			InteractionStateSupport.fail(interactionContext);
			return;
		}

		weaponContext.ensureTrackedHeldWeapon();
		RuntimeWeaponStateAccess.AmmoState state = weaponContext.ensureAmmoForConfiguredGun();
		AmmoDataComponent ammo = state.ammo();
		ammo.setSelectedAmmoItemId(resolvedAmmo.itemId());
		ammo.setLoadedAmmoItemId(resolvedAmmo.itemId());
		ammo.setLoadedAmmoIcon(resolvedAmmo.icon());
		DebugLogger.debug(
			"SetAmmoType", () -> "Applied ammo type: selected=" + ammo.selectedAmmoItemId()
				+ ", loaded=" + ammo.loadedAmmoItemId()
				+ ", icon=" + ammo.loadedAmmoIcon()
		);
		HeldAmmoSyncOnCompletion.schedule(interactionContext, weaponContext.ref(), ammo);
		WeaponInteractionSupport.updateAmmoHud(commandBuffer, weaponContext.ref(), weaponContext.itemStack());
	}

	private @Nullable ResolvedAmmo resolveAmmo() {
		String raw = StringUtil.normalize(this.ammoItemIdValue.get());
		if (raw == null) {
			return null;
		}

		AmmoDefinition definition = AmmoRegistry.getAmmo(raw);
		if (definition == null) {
			return null;
		}

		AmmoItemSettings settings = definition.settings();
		return new ResolvedAmmo(
			definition, definition.itemId(), settings != null
			                                 ? settings.icon()
			                                 : null
		);
	}

	private record ResolvedAmmo(@Nonnull AmmoDefinition definition, String itemId, @Nullable String icon) {
	}
}
