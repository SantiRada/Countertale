package com.thescar.hygunsplugin.runtime.components;

import com.thescar.hygunsplugin.HygunsPluginMain;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AutoGuidanceDataComponent implements Component<EntityStore> {
	public static final String KEY = HygunsPluginMain.key("AutoGuidanceData");
	public static final BuilderCodec<AutoGuidanceDataComponent> CODEC = BuilderCodec
		.builder(AutoGuidanceDataComponent.class, AutoGuidanceDataComponent::new)
		.append(new KeyedCodec<>("TurnRate", Codec.FLOAT), (c, v) -> c.turnRate = v, c -> c.turnRate)
		.add()
		.append(new KeyedCodec<>("DetectionRange", Codec.FLOAT), (c, v) -> c.detectionRange = v, c -> c.detectionRange)
		.add()
		.append(new KeyedCodec<>("Delay", Codec.FLOAT), (c, v) -> c.autoGuidanceDelay = v, c -> c.autoGuidanceDelay)
		.add()
		.append(new KeyedCodec<>("MaxLifeTime", Codec.FLOAT), (c, v) -> c.maxLifeTime = v, c -> c.maxLifeTime)
		.add()
		.append(new KeyedCodec<>("MaxLockAngleDegrees", Codec.FLOAT), (c, v) -> c.maxLockAngleDegrees = v, c -> c.maxLockAngleDegrees)
		.add()
		.append(new KeyedCodec<>("AffectsPlayers", Codec.BOOLEAN), (c, v) -> c.affectsPlayers = v, c -> c.affectsPlayers)
		.add()
		.append(new KeyedCodec<>("EffectId", Codec.STRING), (c, v) -> c.effectId = normalizeEffectId(v), c -> c.effectId)
		.add()
		.build();

	private static ComponentType<EntityStore, AutoGuidanceDataComponent> componentType;

	public float turnRate = 8.0F;

	public float detectionRange = 80.0F;
	public float autoGuidanceDelay = 0.0F;
	public float maxLifeTime = 10.0F;
	public float maxLockAngleDegrees = 180.0F;
	public boolean affectsPlayers;
	public @Nullable String effectId;
	public float lifeTime;
	public boolean hasHit;
	public float timeSinceHit;
	public Ref<EntityStore> targetRef;

	public AutoGuidanceDataComponent() {
	}

	public AutoGuidanceDataComponent(float turnRate, float detectionRange, float autoGuidanceDelay, float maxLifeTime,
	                                 float maxLockAngleDegrees, boolean affectsPlayers, @Nullable String effectId) {
		this.turnRate = turnRate;
		this.detectionRange = detectionRange;
		this.autoGuidanceDelay = autoGuidanceDelay;
		this.maxLifeTime = maxLifeTime;
		this.maxLockAngleDegrees = maxLockAngleDegrees;
		this.affectsPlayers = affectsPlayers;
		this.effectId = normalizeEffectId(effectId);
	}

	public static void registerComponent(ComponentRegistryProxy<EntityStore> registry) {
		AutoGuidanceDataComponent.componentType = registry.registerComponent(AutoGuidanceDataComponent.class, KEY, AutoGuidanceDataComponent.CODEC);
	}

	@Nonnull
	public static ComponentType<EntityStore, AutoGuidanceDataComponent> getComponentType() {
		ComponentType<EntityStore, AutoGuidanceDataComponent> type = componentType;
		if (type == null) {
			throw new IllegalStateException("AutoGuidanceDataComponent type is not registered.");
		}

		return type;
	}

	private static @Nullable String normalizeEffectId(@Nullable String effectId) {
		if (effectId == null) {
			return null;
		}

		String trimmed = effectId.trim();
		return trimmed.isEmpty()
		       ? null
		       : trimmed;
	}

	@Override
	public AutoGuidanceDataComponent clone() {
		AutoGuidanceDataComponent copy = new AutoGuidanceDataComponent(
			this.turnRate, this.detectionRange, this.autoGuidanceDelay,
			this.maxLifeTime, this.maxLockAngleDegrees, this.affectsPlayers, this.effectId
		);
		copy.lifeTime = this.lifeTime;
		copy.hasHit = this.hasHit;
		copy.timeSinceHit = this.timeSinceHit;
		copy.targetRef = this.targetRef;
		return copy;
	}
}
