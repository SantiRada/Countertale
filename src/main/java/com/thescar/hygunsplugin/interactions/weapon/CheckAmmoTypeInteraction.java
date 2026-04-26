package com.thescar.hygunsplugin.interactions.weapon;

import com.thescar.hygunsplugin.HygunsPluginMain;
import com.thescar.hygunsplugin.content.ammo.AmmoDefinition;
import com.thescar.hygunsplugin.content.registry.AmmoRegistry;
import com.thescar.hygunsplugin.runtime.api.RuntimeWeaponStateAccess;
import com.thescar.hygunsplugin.runtime.components.AmmoDataComponent;
import com.thescar.hygunsplugin.support.interaction.InteractionChain;
import com.thescar.hygunsplugin.support.interaction.InteractionStateSupport;
import com.thescar.hygunsplugin.support.interaction.InteractionValue;
import com.thescar.hygunsplugin.support.interaction.WeaponInteractionContext;
import com.thescar.hygunsplugin.support.text.StringUtil;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CheckAmmoTypeInteraction extends SimpleInstantInteraction {
	public static final String KEY = HygunsPluginMain.key("CheckAmmoType");
	public static final BuilderCodec<CheckAmmoTypeInteraction> CODEC = InteractionChain
		.of(
			CheckAmmoTypeInteraction.class, CheckAmmoTypeInteraction::new,
			BuilderCodec.builder(CheckAmmoTypeInteraction.class, CheckAmmoTypeInteraction::new, SimpleInstantInteraction.CODEC)
		)
		.inheritedField("AmmoItemId", Codec.STRING, interaction -> interaction.ammoItemIdValue)
		.inheritedField("Selected", Codec.BOOLEAN, interaction -> interaction.selectedValue)
		.documentation("Checks the loaded or selected ammo type on the held weapon.").build();
	private final InteractionValue<String> ammoItemIdValue = new InteractionValue<>("");
	private final InteractionValue<Boolean> selectedValue = new InteractionValue<>(false);

	public CheckAmmoTypeInteraction(String id) {
		super(id);
	}

	protected CheckAmmoTypeInteraction() {
	}

	private static boolean equalsNormalized(@Nullable String left, @Nullable String right) {
		String normalizedLeft = StringUtil.normalize(left);
		String normalizedRight = StringUtil.normalize(right);
		return normalizedLeft != null && normalizedLeft.equalsIgnoreCase(normalizedRight);
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

		String expectedAmmoItemId = resolveAmmoItemId();
		if (expectedAmmoItemId == null) {
			InteractionStateSupport.fail(interactionContext);
			return;
		}

		weaponContext.ensureTrackedHeldWeapon();
		RuntimeWeaponStateAccess.AmmoState state = weaponContext.ensureAmmoForConfiguredGun();
		AmmoDataComponent ammo = state.ammo();
		String currentAmmoItemId = resolveSelected()
		                           ? ammo.selectedAmmoItemId()
		                           : ammo.loadedAmmoItemId();
		if (equalsNormalized(currentAmmoItemId, expectedAmmoItemId)) {
			return;
		}

		InteractionStateSupport.fail(interactionContext);
	}

	private @Nullable String resolveAmmoItemId() {
		String raw = StringUtil.normalize(this.ammoItemIdValue.get());
		if (raw == null) {
			return null;
		}

		AmmoDefinition definition = AmmoRegistry.getAmmo(raw);
		return definition != null
		       ? StringUtil.normalize(definition.itemId())
		       : raw;
	}

	private boolean resolveSelected() {
		return Boolean.TRUE.equals(this.selectedValue.get());
	}
}
