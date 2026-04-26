package com.thescar.hygunsplugin.runtime.components;

import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeEcs;
import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeStore;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeComponentType;
import com.thescar.hygunsplugin.support.text.StringUtil;

import com.hypixel.hytale.component.Component;

import javax.annotation.Nullable;
import java.util.Objects;

public final class AmmoDataComponent implements Component<ItemRuntimeStore> {
	private int ammo;
	private int maxAmmo;
	private boolean initialized;
	private boolean dirty;
	private long dirtySinceMs;
	private @Nullable String selectedAmmoItemId;
	private @Nullable String loadedAmmoItemId;
	private @Nullable String loadedAmmoIcon;

	public AmmoDataComponent() {
	}

	public AmmoDataComponent(int ammo, int maxAmmo) {
		this.ammo = Math.max(0, ammo);
		this.maxAmmo = Math.max(0, maxAmmo);
		this.initialized = true;
	}

	public static RuntimeComponentType<ItemRuntimeStore, AmmoDataComponent> getComponentType() {
		return ItemRuntimeEcs.AMMO_DATA_TYPE;
	}

	public int ammo() {
		return ammo;
	}

	public void setAmmo(int ammo) {
		int next = Math.max(0, ammo);
		if (this.ammo != next) {
			this.ammo = next;
			markDirty();
		}
	}

	public int maxAmmo() {
		return maxAmmo;
	}

	public void setMaxAmmo(int maxAmmo) {
		int next = Math.max(0, maxAmmo);
		if (this.maxAmmo != next) {
			this.maxAmmo = next;
			markDirty();
		}
	}

	public boolean initialized() {
		return initialized;
	}

	public void setInitialized(boolean initialized) {
		if (this.initialized != initialized) {
			this.initialized = initialized;
			markDirty();
		}
	}

	public boolean consume(int amount) {
		if (amount <= 0) {
			return true;
		}

		if (this.ammo < amount) {
			return false;
		}

		this.ammo -= amount;
		markDirty();
		return true;
	}

	public void reload(int amount) {
		if (amount <= 0) {
			return;
		}

		int cappedMax = Math.max(0, this.maxAmmo);
		this.ammo = Math.min(cappedMax, this.ammo + amount);
		markDirty();
	}

	public int effectiveAmmo() {
		return Math.max(0, this.ammo);
	}

	public boolean dirty() {
		return dirty;
	}

	public long dirtySinceMs() {
		return dirtySinceMs;
	}

	@Nullable
	public String selectedAmmoItemId() {
		return selectedAmmoItemId;
	}

	public void setSelectedAmmoItemId(@Nullable String selectedAmmoItemId) {
		String next = StringUtil.normalize(selectedAmmoItemId);
		if (!Objects.equals(this.selectedAmmoItemId, next)) {
			this.selectedAmmoItemId = next;
			markDirty();
		}
	}

	@Nullable
	public String loadedAmmoItemId() {
		return loadedAmmoItemId;
	}

	public void setLoadedAmmoItemId(@Nullable String loadedAmmoItemId) {
		String next = StringUtil.normalize(loadedAmmoItemId);
		if (!Objects.equals(this.loadedAmmoItemId, next)) {
			this.loadedAmmoItemId = next;
			markDirty();
		}
	}

	@Nullable
	public String loadedAmmoIcon() {
		return loadedAmmoIcon;
	}

	public void setLoadedAmmoIcon(@Nullable String loadedAmmoIcon) {
		String next = StringUtil.normalize(loadedAmmoIcon);
		if (!Objects.equals(this.loadedAmmoIcon, next)) {
			this.loadedAmmoIcon = next;
			markDirty();
		}
	}

	public void markDirty() {
		this.dirty = true;
		this.dirtySinceMs = System.currentTimeMillis();
	}

	public void clearDirty() {
		this.dirty = false;
		this.dirtySinceMs = 0L;
	}

	@Override
	public AmmoDataComponent clone() {
		AmmoDataComponent copy = new AmmoDataComponent();
		copy.ammo = this.ammo;
		copy.maxAmmo = this.maxAmmo;
		copy.initialized = this.initialized;
		copy.dirty = this.dirty;
		copy.dirtySinceMs = this.dirtySinceMs;
		copy.selectedAmmoItemId = this.selectedAmmoItemId;
		copy.loadedAmmoItemId = this.loadedAmmoItemId;
		copy.loadedAmmoIcon = this.loadedAmmoIcon;
		return copy;
	}
}
