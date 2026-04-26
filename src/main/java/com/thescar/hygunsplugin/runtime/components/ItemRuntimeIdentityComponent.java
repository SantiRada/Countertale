package com.thescar.hygunsplugin.runtime.components;

import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeEcs;
import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeKind;
import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeStore;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeComponentType;
import com.thescar.hygunsplugin.support.text.StringUtil;

import com.hypixel.hytale.component.Component;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

public final class ItemRuntimeIdentityComponent implements Component<ItemRuntimeStore> {
	private @Nullable UUID runtimeId;
	private @Nullable String itemId;
	private ItemRuntimeKind kind = ItemRuntimeKind.WEAPON;

	public ItemRuntimeIdentityComponent() {
	}

	public ItemRuntimeIdentityComponent(@Nullable UUID runtimeId, @Nullable String itemId, @Nullable ItemRuntimeKind kind) {
		this.runtimeId = runtimeId;
		this.itemId = StringUtil.normalize(itemId);
		if (kind != null) {
			this.kind = kind;
		}
	}

	public static RuntimeComponentType<ItemRuntimeStore, ItemRuntimeIdentityComponent> getComponentType() {
		return ItemRuntimeEcs.IDENTITY_TYPE;
	}

	@Nullable
	public UUID runtimeId() {
		return this.runtimeId;
	}

	public void setRuntimeId(@Nullable UUID runtimeId) {
		this.runtimeId = runtimeId;
	}

	@Nullable
	public String itemId() {
		return this.itemId;
	}

	public void setItemId(@Nullable String itemId) {
		this.itemId = StringUtil.normalize(itemId);
	}

	public ItemRuntimeKind kind() {
		return this.kind;
	}

	public void setKind(@Nullable ItemRuntimeKind kind) {
		this.kind = kind != null
		            ? kind
		            : ItemRuntimeKind.WEAPON;
	}

	@Override
	public ItemRuntimeIdentityComponent clone() {
		return new ItemRuntimeIdentityComponent(this.runtimeId, this.itemId, this.kind);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof ItemRuntimeIdentityComponent other)) {
			return false;
		}

		return Objects.equals(this.runtimeId, other.runtimeId) && Objects.equals(this.itemId, other.itemId) && this.kind == other.kind;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.runtimeId, this.itemId, this.kind);
	}
}
