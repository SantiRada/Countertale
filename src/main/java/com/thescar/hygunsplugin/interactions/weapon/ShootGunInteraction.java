package com.thescar.hygunsplugin.interactions.weapon;

import com.thescar.hygunsplugin.HygunsPluginMain;
import com.thescar.hygunsplugin.content.ammo.AmmoDefinition;
import com.thescar.hygunsplugin.content.registry.AmmoRegistry;
import com.thescar.hygunsplugin.content.registry.GunRegistry;
import com.thescar.hygunsplugin.content.settings.*;
import com.thescar.hygunsplugin.debug.DebugLogger;
import com.thescar.hygunsplugin.gameplay.projectile.DamageModifier;
import com.thescar.hygunsplugin.gameplay.projectile.HitDamageModifiers;
import com.thescar.hygunsplugin.gameplay.projectile.ShootProjectile;
import com.thescar.hygunsplugin.gameplay.reload.ReloadManager;
import com.thescar.hygunsplugin.runtime.api.RuntimeWeaponStateAccess;
import com.thescar.hygunsplugin.runtime.components.AmmoDataComponent;
import com.thescar.hygunsplugin.runtime.components.FireDelayComponent;
import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeEcs;
import com.thescar.hygunsplugin.support.hytale.PlayerRefAccess;
import com.thescar.hygunsplugin.support.interaction.InteractionChain;
import com.thescar.hygunsplugin.support.interaction.InteractionValue;
import com.thescar.hygunsplugin.support.interaction.WeaponInteractionContext;
import com.thescar.hygunsplugin.support.interaction.WeaponInteractionSupport;
import com.thescar.hygunsplugin.support.text.ValueUtils;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ThreadLocalRandom;

public class ShootGunInteraction extends SimpleInstantInteraction {
	public static final String KEY = HygunsPluginMain.key("Shoot");
	public static final BuilderCodec<ShootGunInteraction> CODEC = InteractionChain
		.of(
			ShootGunInteraction.class, ShootGunInteraction::new,
			BuilderCodec.builder(ShootGunInteraction.class, ShootGunInteraction::new, SimpleInstantInteraction.CODEC)
		)
		.nested(
			HygunsPluginMain.key("SettingsOverrides"),
			overrides -> overrides
				.group(WeaponProjectileSettings.GROUP, i -> i.projectileSettingsValue)
				.group(WeaponAmmoSettings.GROUP, i -> i.ammoSettingsValue)
				.group(WeaponFireSettings.GROUP, i -> i.fireSettingsValue)
				.field(GunSettings.DEAL_LETHAL_DAMAGE, i -> i.dealLethalDamageValue)
				.group(AutoGuidanceSettings.GROUP, i -> i.autoGuidanceSettingsValue)
				.group(WallPenetrationSettings.GROUP, i -> i.wallPenetrationSettingsValue)
		)
		.build();
	private final InteractionValue<WeaponProjectileSettings> projectileSettingsValue = new InteractionValue<>(
		WeaponProjectileSettings.EMPTY);
	private final InteractionValue<WeaponAmmoSettings> ammoSettingsValue = new InteractionValue<>(WeaponAmmoSettings.EMPTY);
	private final InteractionValue<WeaponFireSettings> fireSettingsValue = new InteractionValue<>(WeaponFireSettings.EMPTY);
	private final InteractionValue<Boolean> dealLethalDamageValue = new InteractionValue<>(false);
	private final InteractionValue<AutoGuidanceSettings> autoGuidanceSettingsValue = new InteractionValue<>(AutoGuidanceSettings.EMPTY);
	private final InteractionValue<WallPenetrationSettings> wallPenetrationSettingsValue = new InteractionValue<>(
		WallPenetrationSettings.EMPTY);

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
			interactionContext.getState().state = InteractionState.Failed;
			return;
		}

		CommandBuffer<EntityStore> commandBuffer = weaponContext.commandBuffer();
		Ref<EntityStore> ref = weaponContext.ref();
		Player player = weaponContext.player();
		ItemStack itemStack = weaponContext.ensureTrackedHeldWeapon();
		String initialItemId = itemStack.getItemId();
		DebugLogger.debug("Shoot", "Start heldItem=" + initialItemId);
		GunSettings baseSettings = GunRegistry.getSettings(itemStack.getItemId());
		RuntimeWeaponStateAccess.AmmoState ammoState = weaponContext.ensureAmmoForInteraction(RuntimeWeaponStateAccess.resolveMaxAmmo(baseSettings));
		itemStack = weaponContext.itemStack();

		AmmoDataComponent ammoData = ammoState.ammo();
		GunSettings effectiveSettings = resolveEffectiveSettings(ammoData, baseSettings);
		WeaponProjectileSettings projectileSettings = effectiveSettings != null
		                                              ? effectiveSettings.projectiles()
		                                              : null;
		AmmoItemInteractions ammoInteractions = resolveLoadedAmmoInteractions(ammoData);
		int effectiveDamage = projectileSettings != null && projectileSettings.damage() != null
		                      ? projectileSettings.damage()
		                      : 15;
		int effectiveNumProjectiles = projectileSettings != null && projectileSettings.count() != null
		                              ? projectileSettings.count()
		                              : 1;
		double effectiveSpread = projectileSettings != null && projectileSettings.spread() != null
		                         ? projectileSettings.spread()
		                         : 0.075D;
		int effectiveMaxAmmo = resolveMaxAmmo(effectiveSettings);
		String effectiveProjectileConfigId = projectileSettings != null
		                                     ? ValueUtils.Checks.nonBlankOrNull(projectileSettings.configId())
		                                     : null;
		String effectiveProjectileId = projectileSettings != null
		                               ? ValueUtils.Checks.nonBlankOrNull(projectileSettings.projectileId())
		                               : null;
		HitDamageModifiers effectiveDamageModifiers = (effectiveSettings != null && effectiveSettings.damageModifiers() != null)
		                                              ? effectiveSettings.damageModifiers()
		                                              : HitDamageModifiers.DEFAULT;
		DamageModifier effectiveDamageModifier = effectiveSettings != null
		                                         ? effectiveSettings.damageModifier()
		                                         : null;
		boolean effectiveDealLethalDamage = this.dealLethalDamageValue.get(effectiveSettings, GunSettings::dealLethalDamage);
		effectiveDamage = applyDamageModifier(effectiveDamage, effectiveDamageModifier);
		AmmoSaveSettings effectiveAmmoSaveSettings = resolveAmmoSaveSettings(effectiveSettings);
		AutoGuidanceSettings effectiveAmmoGuidanceSettings = resolveAmmoAutoGuidanceSettings(effectiveSettings);
		WallPenetrationSettings effectiveWallPenetrationSettings = resolveWallPenetrationSettings(effectiveSettings);
		ammoData.setMaxAmmo(effectiveMaxAmmo);
		int resolvedDamage = effectiveDamage;
		int resolvedCount = effectiveNumProjectiles;
		double resolvedSpread = effectiveSpread;
		String resolvedProjectileId = effectiveProjectileId;
		String resolvedProjectileConfigId = effectiveProjectileConfigId;
		DebugLogger.debug(
			"Shoot", () -> "Resolved settings: damage=" + resolvedDamage
				+ ", count=" + resolvedCount
				+ ", spread=" + resolvedSpread
				+ ", ammo=" + ammoData.effectiveAmmo() + "/" + ammoData.maxAmmo()
				+ ", projectileId=" + resolvedProjectileId
				+ ", projectileConfigId=" + resolvedProjectileConfigId
		);
		if (ammoData.ammo() > effectiveMaxAmmo) {
			ammoData.setAmmo(effectiveMaxAmmo);
			ammoData.markDirty();
		}

		PlayerRef playerRef = PlayerRefAccess.getValid(ref, commandBuffer);
		if (playerRef != null) {
			ReloadManager.cancel(playerRef, ReloadManager.CancelReason.SHOOT);
		} else {
			ReloadManager.cancel(player, ReloadManager.CancelReason.SHOOT);
		}

		FireDelayComponent fireDelay = ItemRuntimeEcs.getComponent(ammoState.ref(), FireDelayComponent.getComponentType());
		if (fireDelay != null && fireDelay.blocksAt(System.currentTimeMillis())) {
			DebugLogger.debug("Shoot", () -> "Blocked by FireDelay until=" + fireDelay.readyAtMs());
			interactionContext.getState().state = InteractionState.Failed;
			if (interactionContext.getClientState() != null) {
				interactionContext.getClientState().state = InteractionState.Failed;
			}

			WeaponInteractionSupport.updateAmmoHud(commandBuffer, ref, itemStack);
			return;
		}

		if (!ammoData.initialized() || ammoData.maxAmmo() <= 0) {
			DebugLogger.debug("Shoot", "Failed: ammo is not initialized or maxAmmo <= 0");
			interactionContext.getState().state = InteractionState.Failed;
			WeaponInteractionSupport.updateAmmoHud(commandBuffer, ref, itemStack);
			return;
		}

		double durability = itemStack.getDurability();
		double maxDurability = itemStack.getMaxDurability();
		if (maxDurability != 0.0D && durability <= 0.0D) {
			DebugLogger.debug("Shoot", "Failed: weapon durability is depleted");
			interactionContext.getState().state = InteractionState.Failed;
			WeaponInteractionSupport.updateAmmoHud(commandBuffer, ref, itemStack);
			return;
		}

		if (ammoData.effectiveAmmo() <= 0) {
			DebugLogger.debug("Shoot", "Failed: no ammo left");
			interactionContext.getState().state = InteractionState.Failed;
			if (interactionContext.getClientState() != null) {
				interactionContext.getClientState().state = InteractionState.Failed;
			}

			WeaponInteractionSupport.updateAmmoHud(commandBuffer, ref, itemStack);
			return;
		}

		java.util.List<Ref<EntityStore>> spawned;
		String projectileId = effectiveProjectileId != null
		                      ? effectiveProjectileId.trim()
		                      : null;
		if (projectileId != null && !projectileId.isEmpty()) {
			spawned = ShootProjectile.shootProjectiles(
				effectiveNumProjectiles, effectiveDamage, projectileId, effectiveSpread,
				ammoInteractions, effectiveDamageModifiers, effectiveDealLethalDamage, effectiveWallPenetrationSettings,
				effectiveAmmoGuidanceSettings, ref, commandBuffer
			);
		} else {
			spawned = ShootProjectile.shootBullets(
				effectiveNumProjectiles, effectiveDamage, effectiveSpread, effectiveProjectileConfigId,
				ammoInteractions, effectiveDamageModifiers, effectiveDealLethalDamage, effectiveWallPenetrationSettings,
				effectiveAmmoGuidanceSettings, ref, commandBuffer
			);
		}

		if (spawned == null || spawned.isEmpty()) {
			DebugLogger.debug("Shoot", "Failed: projectile spawn returned empty");
			interactionContext.getState().state = InteractionState.Failed;
			if (interactionContext.getClientState() != null) {
				interactionContext.getClientState().state = InteractionState.Failed;
			}

			WeaponInteractionSupport.updateAmmoHud(commandBuffer, ref, itemStack);
			return;
		}

		// Do not rewrite/sync the held ItemStack on every shot.
		// The runtime ammo component tracks the live magazine state.
		// Updating ItemStack metadata every bullet causes client hitching/freezing.
		if (shouldConsumeAmmo(effectiveAmmoSaveSettings)) {
			if (!ammoData.consume(1)) {
				DebugLogger.debug("Shoot", "Failed: ammo.consume(1) returned false");
				interactionContext.getState().state = InteractionState.Failed;
				WeaponInteractionSupport.updateAmmoHud(commandBuffer, ref, itemStack);
				return;
			}
		}

		applyAmmoFireDelayOverride(ammoState.ref(), ammoData);

		String heldItemIdForLog = itemStack.getItemId();
		DebugLogger.debug(
				"Shoot", () -> "Success: heldItem=" + heldItemIdForLog
						+ ", ammoNow=" + ammoData.effectiveAmmo());
		WeaponInteractionSupport.updateAmmoHud(commandBuffer, ref, itemStack);
	}

	private GunSettings resolveEffectiveSettings(@Nonnull AmmoDataComponent ammoData, @Nullable GunSettings baseSettings) {
		String loadedAmmoItemId = ammoData.loadedAmmoItemId();
		AmmoDefinition loadedAmmo = AmmoRegistry.getAmmo(loadedAmmoItemId);
		GunSettings ammoOverrides = loadedAmmo != null && loadedAmmo.settings() != null
		                            ? loadedAmmo.settings().settingsOverrides()
		                            : null;
		GunSettings interactionOverrides = buildInteractionOverrides();
		return GunSettingsMerger.merge(interactionOverrides, ammoOverrides, baseSettings);
	}

	private @Nullable AmmoItemInteractions resolveLoadedAmmoInteractions(@Nonnull AmmoDataComponent ammoData) {
		String loadedAmmoItemId = ammoData.loadedAmmoItemId();
		AmmoDefinition loadedAmmo = AmmoRegistry.getAmmo(loadedAmmoItemId);
		return loadedAmmo != null && loadedAmmo.settings() != null
		       ? loadedAmmo.settings().interactions()
		       : null;
	}

	private int resolveMaxAmmo(@Nullable GunSettings settings) {
		WeaponAmmoSettings ammo = settings != null
		                          ? settings.ammo()
		                          : null;
		Integer capacity = ammo != null
		                   ? ammo.capacity()
		                   : null;
		return capacity != null && capacity > 0
		       ? capacity
		       : 1;
	}

	private AmmoSaveSettings resolveAmmoSaveSettings(@Nullable GunSettings settings) {
		AmmoSaveSettings ammoSaveSettings = settings != null && settings.ammo() != null
		                                    ? settings.ammo().saveSettings()
		                                    : null;
		return AmmoSaveSettings.resolve(null, ammoSaveSettings, AmmoSaveSettings.DEFAULTS);
	}

	private int applyDamageModifier(int baseDamage, @Nullable DamageModifier damageModifier) {
		if (damageModifier == null) {
			return baseDamage;
		}

		return damageModifier.apply(baseDamage);
	}

	private boolean shouldConsumeAmmo(@Nullable AmmoSaveSettings settings) {
		if (settings == null || !settings.enabled()) {
			return true;
		}

		double chance = settings.chance();
		if (chance <= 0.0D) {
			return true;
		}

		if (chance >= 1.0D) {
			return false;
		}

		return ThreadLocalRandom.current().nextDouble() >= chance;
	}

	private AutoGuidanceSettings resolveAmmoAutoGuidanceSettings(@Nullable GunSettings settings) {
		AutoGuidanceSettings ammoSettings = settings != null
		                                    ? settings.autoGuidanceSettings()
		                                    : null;
		return AutoGuidanceSettings.resolve(null, ammoSettings, AutoGuidanceSettings.DEFAULTS);
	}

	private WallPenetrationSettings resolveWallPenetrationSettings(@Nullable GunSettings settings) {
		WallPenetrationSettings wallSettings = settings != null
		                                       ? settings.wallPenetrationSettings()
		                                       : null;
		return WallPenetrationSettings.resolve(null, wallSettings, WallPenetrationSettings.DEFAULTS);
	}

	private GunSettings buildInteractionOverrides() {
		GunSettings overrides = new GunSettings();
		WeaponProjectileSettings projectileOverrides = this.projectileSettingsValue.get();
		if (projectileOverrides != null && projectileOverrides.hasAnyValue()) {
			overrides.setProjectiles(projectileOverrides);
		}

		WeaponAmmoSettings ammoOverrides = this.ammoSettingsValue.get();
		if (ammoOverrides != null && ammoOverrides.hasAnyValue()) {
			overrides.setAmmo(ammoOverrides);
		}

		WeaponFireSettings fireOverrides = this.fireSettingsValue.get();
		if (fireOverrides != null && fireOverrides.hasAnyValue()) {
			overrides.setFire(fireOverrides);
		}

		if (this.dealLethalDamageValue.isSet()) {
			overrides.setDealLethalDamage(this.dealLethalDamageValue.get());
		}

		AutoGuidanceSettings guidanceOverrides = this.autoGuidanceSettingsValue.get();
		if (guidanceOverrides != null && guidanceOverrides.hasAnyValue()) {
			overrides.setAutoGuidanceSettings(guidanceOverrides);
		}

		WallPenetrationSettings wallOverrides = this.wallPenetrationSettingsValue.get();
		if (wallOverrides != null && wallOverrides.hasAnyValue()) {
			overrides.setWallPenetrationSettings(wallOverrides);
		}

		return overrides;
	}

	private void applyAmmoFireDelayOverride(@Nonnull com.thescar.hygunsplugin.runtime.item.core.RuntimeItemRef weaponRef,
	                                        @Nonnull AmmoDataComponent ammoData) {
		double ammoFireDelaySeconds = resolveAmmoOverrideFireCooldownSeconds(ammoData);
		if (ammoFireDelaySeconds <= 0.0D) {
			return;
		}

		FireDelayComponent fireDelay = ItemRuntimeEcs.ensureComponent(weaponRef, FireDelayComponent.getComponentType());
		fireDelay.setReadyAtMs(System.currentTimeMillis() + Math.max(1L, Math.round(ammoFireDelaySeconds * 1000.0D)));
	}

	private double resolveAmmoOverrideFireCooldownSeconds(@Nonnull AmmoDataComponent ammoData) {
		AmmoDefinition loadedAmmo = AmmoRegistry.getAmmo(ammoData.loadedAmmoItemId());
		if (loadedAmmo == null || loadedAmmo.settings() == null || loadedAmmo.settings().settingsOverrides() == null) {
			return 0.0D;
		}

		WeaponFireSettings fireSettings = loadedAmmo.settings().settingsOverrides().fire();
		Double cooldown = fireSettings != null
		                  ? fireSettings.cooldown()
		                  : null;
		return cooldown != null && cooldown > 0.0D
		       ? cooldown
		       : 0.0D;
	}

}
