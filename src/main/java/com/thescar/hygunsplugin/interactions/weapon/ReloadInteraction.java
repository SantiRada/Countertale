package com.thescar.hygunsplugin.interactions.weapon;

import com.thescar.hygunsplugin.HygunsPluginMain;
import com.thescar.hygunsplugin.content.ammo.AmmoDefinition;
import com.thescar.hygunsplugin.content.settings.GunSettings;
import com.thescar.hygunsplugin.content.settings.GunSettingsMerger;
import com.thescar.hygunsplugin.content.settings.WeaponAmmoSettings;
import com.thescar.hygunsplugin.content.settings.WeaponReloadSettings;
import com.thescar.hygunsplugin.content.weapon.WeaponContentApi;
import com.thescar.hygunsplugin.gameplay.ammo.AmmoService;
import com.thescar.hygunsplugin.gameplay.reload.ReloadManager;
import com.thescar.hygunsplugin.runtime.api.RuntimeItems;
import com.thescar.hygunsplugin.runtime.api.RuntimeWeaponStateAccess;
import com.thescar.hygunsplugin.runtime.components.AmmoDataComponent;
import com.thescar.hygunsplugin.support.hytale.PlayerInventoryAccess;
import com.thescar.hygunsplugin.support.interaction.*;
import com.thescar.hygunsplugin.support.text.ValueUtils;
import com.thescar.hygunsplugin.ui.hud.HudCoordinator;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ReloadInteraction extends SimpleInstantInteraction {
	public static final String KEY = HygunsPluginMain.key("Reload");
	public static final BuilderCodec<ReloadInteraction> CODEC = InteractionChain
		.of(
			ReloadInteraction.class, ReloadInteraction::new,
			BuilderCodec.builder(ReloadInteraction.class, ReloadInteraction::new, SimpleInstantInteraction.CODEC)
		)
		.nested(HygunsPluginMain.key("SettingsOverrides"), overrides -> overrides.group(WeaponAmmoSettings.GROUP, i -> i.ammoSettingsValue))
		.build();
	private final InteractionValue<WeaponAmmoSettings> ammoSettingsValue = new InteractionValue<>(WeaponAmmoSettings.EMPTY);

	@Nonnull
	@Override

	public WaitForDataFrom getWaitForDataFrom() {
		return WaitForDataFrom.Server;
	}

	@Override
	protected void firstRun(@Nonnull InteractionType interactionType, @Nonnull InteractionContext interactionContext,
	                        @Nonnull CooldownHandler cooldownHandler) {
		WeaponInteractionContext weaponContext = WeaponInteractionContext.resolve(interactionContext);
		if (weaponContext == null || PlayerInventoryAccess.getReference(weaponContext.player()) == null) {
			InteractionStateSupport.fail(interactionContext);
			return;
		}

		CommandBuffer<EntityStore> commandBuffer = weaponContext.commandBuffer();
		PlayerRef playerRef = weaponContext.playerRef();
		if (playerRef == null) {
			InteractionStateSupport.fail(interactionContext);
			return;
		}

		var player = weaponContext.player();
		var itemStack = weaponContext.ensureTrackedHeldWeapon();
		GunSettings settings = WeaponContentApi.getSettings(itemStack.getItemId());
		GunSettings effectiveSettings = GunSettingsMerger.merge(buildInteractionOverrides(), settings);
		WeaponAmmoSettings weaponAmmo = effectiveSettings != null
		                                ? effectiveSettings.ammo()
		                                : null;
		int effectiveMaxAmmo = RuntimeWeaponStateAccess.resolveMaxAmmo(weaponAmmo);
		WeaponReloadSettings reloadSettings = weaponAmmo != null
		                                      ? weaponAmmo.reload()
		                                      : null;
		int effectiveReloadAmount = reloadSettings != null && reloadSettings.amount() != null
		                            ? reloadSettings.amount()
		                            : effectiveMaxAmmo;
		double effectiveReloadTime = reloadSettings != null && reloadSettings.time() != null
		                             ? reloadSettings.time()
		                             : 1.5D;
		if (ReloadManager.isReloading(playerRef)) {
			InteractionStateSupport.fail(interactionContext);
			return;
		}

		RuntimeWeaponStateAccess.AmmoState ensured = weaponContext.ensureAmmoForReloadApply(effectiveMaxAmmo);
		itemStack = weaponContext.itemStack();
		AmmoDataComponent ammo = ensured.ammo();
		ammo.setMaxAmmo(effectiveMaxAmmo);
		if (!ammo.initialized() || ammo.ammo() >= ammo.maxAmmo()) {
			InteractionStateSupport.fail(interactionContext);
			return;
		}

		CombinedItemContainer combined = AmmoService.getAmmoContainer(player);
		if (combined == null) {
			InteractionStateSupport.fail(interactionContext);
			return;
		}

		int reloadPerInteraction = effectiveReloadAmount <= 0
		                           ? ammo.maxAmmo()
		                           : effectiveReloadAmount;
		String effectiveAmmoItemId = resolveAmmoItemId(itemStack, weaponAmmo, combined);
		if (effectiveAmmoItemId == null) {
			InteractionStateSupport.fail(interactionContext);
			return;
		}

		ensureSelectedAmmo(ammo, effectiveAmmoItemId);
		HeldAmmoSyncOnCompletion.schedule(interactionContext, weaponContext.ref(), ammo);
		var ammoDefinition = com.thescar.hygunsplugin.content.registry.AmmoRegistry.getAmmo(effectiveAmmoItemId);
		String effectiveAmmoIcon = resolveAmmoIcon(ammoDefinition, weaponAmmo);
		if (AmmoService.countAmmo(combined, effectiveAmmoItemId) <= 0) {
			InteractionStateSupport.fail(interactionContext);
			return;
		}

		var runtime = RuntimeItems.ensure(itemStack);
		var reload = runtime.ensureReload();
		reload.clear();
		reload.setActive(true);
		reload.setPlayerUuid(playerRef.getUuid());
		reload.setAmmoItemId(effectiveAmmoItemId);
		reload.setAmmoIcon(effectiveAmmoIcon);
		reload.setAmmoBeforeReload(ammo.ammo());
		reload.setMaxAmmo(ammo.maxAmmo());
		reload.setReloadAmountPerInteraction(reloadPerInteraction);
		reload.setStartedAtMs(System.currentTimeMillis());
		reload.setReloadDurationMs(Math.max(0L, Math.round(effectiveReloadTime * 1000.0D)));
		ReloadManager.register(playerRef.getUuid(), runtime.ref());
		HudCoordinator.updateAmmo(playerRef, itemStack);
	}

	private void ensureSelectedAmmo(AmmoDataComponent ammo, @Nonnull String ammoItemId) {
		String selectedAmmoItemId = ammo.selectedAmmoItemId();
		if (ammoItemId.equalsIgnoreCase(selectedAmmoItemId)) {
			return;
		}

		ammo.setSelectedAmmoItemId(ammoItemId);
		ammo.markDirty();
	}

	private GunSettings buildInteractionOverrides() {
		GunSettings overrides = new GunSettings();
		WeaponAmmoSettings ammoOverrides = this.ammoSettingsValue.get();
		if (ammoOverrides != null && ammoOverrides.hasAnyValue()) {
			overrides.setAmmo(ammoOverrides);
		}

		return overrides;
	}

	@Nullable
	private String resolveAmmoIcon(@Nullable AmmoDefinition ammoDefinition, @Nullable WeaponAmmoSettings weaponAmmo) {
		if (ammoDefinition != null && ammoDefinition.settings() != null) {
			String icon = ValueUtils.Checks.nonBlankOrNull(ammoDefinition.settings().icon());
			return icon;
		}

		return null;
	}

	@Nullable
	private String resolveAmmoItemId(@Nonnull ItemStack gunStack, @Nullable WeaponAmmoSettings weaponAmmo,
	                                 @Nonnull CombinedItemContainer combined) {
		if (weaponAmmo == null) {
			return null;
		}

		String exactItemId = ValueUtils.Checks.nonBlankOrNull(weaponAmmo.itemId());
		if (exactItemId != null) {
			return exactItemId;
		}

		return AmmoService.resolvePreferredAmmoItemId(gunStack, weaponAmmo, combined);
	}
}
