package com.thescar.hygunsplugin.runtime.api;

import com.thescar.hygunsplugin.runtime.components.AmmoDataComponent;
import com.thescar.hygunsplugin.runtime.components.FireDelayComponent;
import com.thescar.hygunsplugin.runtime.components.HeatDataComponent;
import com.thescar.hygunsplugin.runtime.components.WeaponReloadComponent;
import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeEcs;
import com.thescar.hygunsplugin.runtime.item.core.RuntimeItemIdentity;
import com.thescar.hygunsplugin.runtime.item.core.RuntimeItemRef;

import com.hypixel.hytale.server.core.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class RuntimeItems {
	private RuntimeItems() {
	}

	@Nullable
	public static RuntimeItemHandle resolve(@Nullable ItemStack itemStack) {
		RuntimeItemRef ref = RuntimeItemIdentity.resolve(itemStack);
		return ref != null
		       ? new RuntimeItemHandle(ref, false)
		       : null;
	}

	@Nonnull
	public static RuntimeItemHandle ensure(@Nonnull ItemStack itemStack) {
		RuntimeItemIdentity.Assignment assignment = ItemRuntimeEcs.ensureTracked(itemStack);
		return new RuntimeItemHandle(assignment.ref(), assignment.created());
	}

	public static final class RuntimeItemHandle {
		private final RuntimeItemRef ref;

		private final boolean created;

		private RuntimeItemHandle(@Nonnull RuntimeItemRef ref, boolean created) {
			this.ref = ref;
			this.created = created;
		}

		@Nonnull
		public RuntimeItemRef ref() {
			return this.ref;
		}

		public boolean created() {
			return this.created;
		}

		@Nullable
		public AmmoDataComponent ammo() {
			return ItemRuntimeEcs.getComponent(this.ref, AmmoDataComponent.getComponentType());
		}

		@Nonnull
		public AmmoDataComponent ensureAmmo() {
			return ItemRuntimeEcs.ensureComponent(this.ref, AmmoDataComponent.getComponentType());
		}

		@Nullable
		public HeatDataComponent heat() {
			return ItemRuntimeEcs.getComponent(this.ref, HeatDataComponent.getComponentType());
		}

		@Nonnull
		public HeatDataComponent ensureHeat() {
			return ItemRuntimeEcs.ensureComponent(this.ref, HeatDataComponent.getComponentType());
		}

		@Nullable
		public FireDelayComponent fireDelay() {
			return ItemRuntimeEcs.getComponent(this.ref, FireDelayComponent.getComponentType());
		}

		@Nonnull
		public FireDelayComponent ensureFireDelay() {
			return ItemRuntimeEcs.ensureComponent(this.ref, FireDelayComponent.getComponentType());
		}

		@Nullable
		public WeaponReloadComponent reload() {
			return ItemRuntimeEcs.getComponent(this.ref, WeaponReloadComponent.getComponentType());
		}

		@Nonnull
		public WeaponReloadComponent ensureReload() {
			return ItemRuntimeEcs.ensureComponent(this.ref, WeaponReloadComponent.getComponentType());
		}
	}
}
