package com.thescar.hygunsplugin.runtime.components;

import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeEcs;
import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeStore;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeComponentType;

import com.hypixel.hytale.component.Component;

public final class FireDelayComponent implements Component<ItemRuntimeStore> {
	private long readyAtMs;

	public static RuntimeComponentType<ItemRuntimeStore, FireDelayComponent> getComponentType() {
		return ItemRuntimeEcs.FIRE_DELAY_TYPE;
	}

	public long readyAtMs() {
		return this.readyAtMs;
	}

	public void setReadyAtMs(long readyAtMs) {
		this.readyAtMs = Math.max(0L, readyAtMs);
	}

	public boolean blocksAt(long nowMs) {
		return nowMs < this.readyAtMs;
	}

	@Override
	public FireDelayComponent clone() {
		FireDelayComponent copy = new FireDelayComponent();
		copy.readyAtMs = this.readyAtMs;
		return copy;
	}
}
