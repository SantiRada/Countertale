package com.thescar.hygunsplugin.gameplay.projectile;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public final class EntityEffects {
	private EntityEffects() {
	}

	public static boolean hasEffect(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> entityRef,
	                                @Nonnull String effectId) {
		int effectIndex = EntityEffect.getAssetMap().getIndex(effectId);
		if (effectIndex < 0) {
			return false;
		}

		return hasEffect(accessor, entityRef, effectIndex);
	}

	public static boolean hasEffect(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> entityRef,
	                                int effectIndex) {
		if (effectIndex < 0 || !entityRef.isValid()) {
			return false;
		}

		EffectControllerComponent effects = accessor.getComponent(entityRef, EffectControllerComponent.getComponentType());
		return effects != null && effects.getActiveEffects().containsKey(effectIndex);
	}
}
