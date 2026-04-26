package com.thescar.hygunsplugin.runtime.components;

import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeEcs;
import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeStore;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeComponentType;

import com.hypixel.hytale.component.Component;

public final class WeaponItemComponent implements Component<ItemRuntimeStore> {
	public static RuntimeComponentType<ItemRuntimeStore, WeaponItemComponent> getComponentType() {
		return ItemRuntimeEcs.WEAPON_TYPE;
	}

	@Override
	public WeaponItemComponent clone() {
		return new WeaponItemComponent();
	}
}
