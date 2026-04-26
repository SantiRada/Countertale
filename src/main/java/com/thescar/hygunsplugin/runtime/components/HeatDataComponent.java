package com.thescar.hygunsplugin.runtime.components;

import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeEcs;
import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeStore;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeComponentType;
import com.thescar.hygunsplugin.ui.hud.screens.HeatUiSettings;

import com.hypixel.hytale.component.Component;

public final class HeatDataComponent implements Component<ItemRuntimeStore> {
	private float heat;
	private int overheatTimeMs;
	private int cooldownTimeMs;
	private int lastUseMs;
	private int fireStartMs;
	private float fireBaseHeat;
	private boolean overheated;
	private boolean dirty;
	private HeatUiSettings uiSettings = HeatUiSettings.defaults();

	public HeatDataComponent() {
	}

	public static RuntimeComponentType<ItemRuntimeStore, HeatDataComponent> getComponentType() {
		return ItemRuntimeEcs.HEAT_DATA_TYPE;
	}

	public float heat() {
		return heat;
	}

	public void setHeat(float heat) {
		this.heat = Math.max(0.0F, Math.min(1.0F, heat));
	}

	public int overheatTimeMs() {
		return overheatTimeMs;
	}

	public void setOverheatTimeMs(int overheatTimeMs) {
		this.overheatTimeMs = Math.max(0, overheatTimeMs);
	}

	public int cooldownTimeMs() {
		return cooldownTimeMs;
	}

	public void setCooldownTimeMs(int cooldownTimeMs) {
		this.cooldownTimeMs = Math.max(0, cooldownTimeMs);
	}

	public int lastUseMs() {
		return lastUseMs;
	}

	public void setLastUseMs(int lastUseMs) {
		this.lastUseMs = Math.max(0, lastUseMs);
	}

	public int fireStartMs() {
		return fireStartMs;
	}

	public void setFireStartMs(int fireStartMs) {
		this.fireStartMs = Math.max(0, fireStartMs);
	}

	public float fireBaseHeat() {
		return fireBaseHeat;
	}

	public void setFireBaseHeat(float fireBaseHeat) {
		this.fireBaseHeat = Math.max(0.0F, Math.min(1.0F, fireBaseHeat));
	}

	public boolean overheated() {
		return overheated;
	}

	public void setOverheated(boolean overheated) {
		this.overheated = overheated;
	}

	public boolean dirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	public HeatUiSettings uiSettings() {
		return uiSettings;
	}

	public void setUiSettings(HeatUiSettings uiSettings) {
		this.uiSettings = uiSettings != null
		                  ? uiSettings
		                  : HeatUiSettings.defaults();
	}

	public float progress() {
		if (this.overheatTimeMs <= 0 || this.heat <= 0.0F) {
			return 0.0F;
		}

		return this.heat;
	}

	public void clear() {
		this.heat = 0.0F;
		this.lastUseMs = 0;
		this.fireStartMs = 0;
		this.fireBaseHeat = 0.0F;
		this.overheated = false;
		this.dirty = false;
	}

	@Override
	public HeatDataComponent clone() {
		HeatDataComponent copy = new HeatDataComponent();
		copy.heat = this.heat;
		copy.overheatTimeMs = this.overheatTimeMs;
		copy.cooldownTimeMs = this.cooldownTimeMs;
		copy.lastUseMs = this.lastUseMs;
		copy.fireStartMs = this.fireStartMs;
		copy.fireBaseHeat = this.fireBaseHeat;
		copy.overheated = this.overheated;
		copy.dirty = this.dirty;
		copy.uiSettings = this.uiSettings;
		return copy;
	}
}
