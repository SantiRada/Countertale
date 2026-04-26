package com.thescar.hygunsplugin.runtime.components;

import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeEcs;
import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeStore;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeComponentType;

import com.hypixel.hytale.component.Component;

public final class AmmoItemComponent implements Component<ItemRuntimeStore> {
	public static RuntimeComponentType<ItemRuntimeStore, AmmoItemComponent> getComponentType() {
		return ItemRuntimeEcs.AMMO_TYPE;
	}

	@Override
	public AmmoItemComponent clone() {
		return new AmmoItemComponent();
	}
}
