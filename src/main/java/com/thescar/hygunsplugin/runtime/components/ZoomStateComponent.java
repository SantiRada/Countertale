package com.thescar.hygunsplugin.runtime.components;

import com.thescar.hygunsplugin.HygunsPluginMain;
import com.thescar.hygunsplugin.support.text.StringUtil;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ZoomStateComponent implements Component<EntityStore> {
	public static final String KEY = HygunsPluginMain.key("ZoomState");
	public static final BuilderCodec<ZoomStateComponent> CODEC = BuilderCodec
		.builder(ZoomStateComponent.class, ZoomStateComponent::new)
		.append(
			new KeyedCodec<>("ScopedItemId", Codec.STRING), (component, value) -> component.scopedItemId = StringUtil.normalize(value),
			component -> component.scopedItemId
		)
		.add()
		.append(
			new KeyedCodec<>("OverlayTexturePath", Codec.STRING),
			(component, value) -> component.overlayTexturePath = StringUtil.normalize(value), component -> component.overlayTexturePath
		)
		.add()
		.append(
			new KeyedCodec<>("MaxDistance", Codec.FLOAT), (component, value) -> component.maxDistance = value,
			component -> component.maxDistance
		)
		.add()
		.append(
			new KeyedCodec<>("MinDistance", Codec.FLOAT), (component, value) -> component.minDistance = value,
			component -> component.minDistance
		)
		.add()
		.append(
			new KeyedCodec<>("DefaultZoomMultiplier", Codec.FLOAT),
			(component, value) -> component.defaultZoomMultiplier = value, component -> component.defaultZoomMultiplier
		)
		.add()
		.append(
			new KeyedCodec<>("MaxZoomMultiplier", Codec.FLOAT),
			(component, value) -> component.maxZoomMultiplier = value, component -> component.maxZoomMultiplier
		)
		.add()
		.append(
			new KeyedCodec<>("ZoomMultiplierStep", Codec.FLOAT),
			(component, value) -> component.zoomMultiplierStep = value, component -> component.zoomMultiplierStep
		)
		.add()
		.append(
			new KeyedCodec<>("CurrentDistance", Codec.FLOAT), (component, value) -> component.currentDistance = value,
			component -> component.currentDistance
		)
		.add()
		.append(
			new KeyedCodec<>("ZoomMultiplier", Codec.FLOAT), (component, value) -> component.zoomMultiplier = value,
			component -> component.zoomMultiplier
		)
		.add()
		.build();

	private static ComponentType<EntityStore, ZoomStateComponent> componentType;

	private @Nullable String scopedItemId;
	private @Nullable String overlayTexturePath;
	private float maxDistance;
	private float minDistance;
	private float defaultZoomMultiplier;
	private float maxZoomMultiplier;
	private float zoomMultiplierStep;
	private float currentDistance;
	private float zoomMultiplier;

	public ZoomStateComponent() {
	}

	public static void registerComponent(ComponentRegistryProxy<EntityStore> registry) {
		ZoomStateComponent.componentType = registry.registerComponent(ZoomStateComponent.class, KEY, ZoomStateComponent.CODEC);
	}

	@Nonnull
	public static ComponentType<EntityStore, ZoomStateComponent> getComponentType() {
		ComponentType<EntityStore, ZoomStateComponent> type = componentType;
		if (type == null) {
			throw new IllegalStateException("ZoomStateComponent type is not registered.");
		}

		return type;
	}

	@Nullable
	public String scopedItemId() {
		return this.scopedItemId;
	}

	public void setScopedItemId(@Nullable String scopedItemId) {
		this.scopedItemId = StringUtil.normalize(scopedItemId);
	}

	@Nullable
	public String overlayTexturePath() {
		return this.overlayTexturePath;
	}

	public void setOverlayTexturePath(@Nullable String overlayTexturePath) {
		this.overlayTexturePath = StringUtil.normalize(overlayTexturePath);
	}

	public float maxDistance() {
		return this.maxDistance;
	}

	public void setMaxDistance(float maxDistance) {
		this.maxDistance = Math.max(0.1F, maxDistance);
	}

	public float minDistance() {
		return this.minDistance;
	}

	public void setMinDistance(float minDistance) {
		this.minDistance = Math.max(0.1F, minDistance);
	}

	public float defaultZoomMultiplier() {
		return this.defaultZoomMultiplier;
	}

	public void setDefaultZoomMultiplier(float defaultZoomMultiplier) {
		this.defaultZoomMultiplier = Math.max(0.1F, defaultZoomMultiplier);
	}

	public float maxZoomMultiplier() {
		return this.maxZoomMultiplier;
	}

	public void setMaxZoomMultiplier(float maxZoomMultiplier) {
		this.maxZoomMultiplier = Math.max(this.defaultZoomMultiplier, maxZoomMultiplier);
	}

	public float zoomMultiplierStep() {
		return this.zoomMultiplierStep;
	}

	public void setZoomMultiplierStep(float zoomMultiplierStep) {
		this.zoomMultiplierStep = Math.max(0.01F, zoomMultiplierStep);
	}

	public float currentDistance() {
		return this.currentDistance;
	}

	public void setCurrentDistance(float currentDistance) {
		this.currentDistance = Math.max(0.0F, currentDistance);
	}

	public float zoomMultiplier() {
		return this.zoomMultiplier;
	}

	public void setZoomMultiplier(float zoomMultiplier) {
		this.zoomMultiplier = Math.max(0.1F, zoomMultiplier);
	}

	public float currentMaxDistance() {
		return this.maxDistance * this.zoomMultiplier;
	}

	public void applySettings(@Nonnull String scopedItemId, @Nonnull com.thescar.hygunsplugin.gameplay.zoom.ZoomManager.ZoomSettings settings) {
		this.setScopedItemId(scopedItemId);
		this.setOverlayTexturePath(settings.overlayTexturePath());
		this.setMaxDistance(settings.maxDistance());
		this.setMinDistance(settings.minDistance());
		this.setDefaultZoomMultiplier(settings.defaultZoomMultiplier());
		this.setMaxZoomMultiplier(settings.maxZoomMultiplier());
		this.setZoomMultiplierStep(settings.zoomMultiplierStep());
		this.setZoomMultiplier(settings.defaultZoomMultiplier());
		this.setCurrentDistance(settings.maxDistance());
	}

	@Override
	public ZoomStateComponent clone() {
		ZoomStateComponent copy = new ZoomStateComponent();
		copy.scopedItemId = this.scopedItemId;
		copy.overlayTexturePath = this.overlayTexturePath;
		copy.maxDistance = this.maxDistance;
		copy.minDistance = this.minDistance;
		copy.defaultZoomMultiplier = this.defaultZoomMultiplier;
		copy.maxZoomMultiplier = this.maxZoomMultiplier;
		copy.zoomMultiplierStep = this.zoomMultiplierStep;
		copy.currentDistance = this.currentDistance;
		copy.zoomMultiplier = this.zoomMultiplier;
		return copy;
	}
}
