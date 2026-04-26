package com.thescar.hygunsplugin.runtime.components;

import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeEcs;
import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeStore;
import com.thescar.hygunsplugin.runtime.item.ecs.RuntimeComponentType;

import com.hypixel.hytale.component.Component;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

public final class WeaponReloadComponent implements Component<ItemRuntimeStore> {
	private boolean active;
	private @Nullable UUID playerUuid;
	private @Nullable String ammoItemId;
	private @Nullable String ammoIcon;
	private int ammoBeforeReload;
	private int maxAmmo;
	private int reloadAmountPerInteraction;
	private long startedAtMs;
	private long reloadDurationMs;

	public static RuntimeComponentType<ItemRuntimeStore, WeaponReloadComponent> getComponentType() {
		return ItemRuntimeEcs.WEAPON_RELOAD_TYPE;
	}

	@Nullable
	private static String normalize(@Nullable String value) {
		if (value == null) {
			return null;
		}

		String trimmed = value.trim();
		return trimmed.isEmpty()
		       ? null
		       : trimmed;
	}

	public boolean active() {
		return this.active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	@Nullable
	public UUID playerUuid() {
		return this.playerUuid;
	}

	public void setPlayerUuid(@Nullable UUID playerUuid) {
		this.playerUuid = playerUuid;
	}

	@Nullable
	public String ammoItemId() {
		return this.ammoItemId;
	}

	public void setAmmoItemId(@Nullable String ammoItemId) {
		this.ammoItemId = normalize(ammoItemId);
	}

	@Nullable
	public String ammoIcon() {
		return this.ammoIcon;
	}

	public void setAmmoIcon(@Nullable String ammoIcon) {
		this.ammoIcon = normalize(ammoIcon);
	}

	public int ammoBeforeReload() {
		return this.ammoBeforeReload;
	}

	public void setAmmoBeforeReload(int ammoBeforeReload) {
		this.ammoBeforeReload = Math.max(0, ammoBeforeReload);
	}

	public int maxAmmo() {
		return this.maxAmmo;
	}

	public void setMaxAmmo(int maxAmmo) {
		this.maxAmmo = Math.max(0, maxAmmo);
	}

	public int reloadAmountPerInteraction() {
		return this.reloadAmountPerInteraction;
	}

	public void setReloadAmountPerInteraction(int reloadAmountPerInteraction) {
		this.reloadAmountPerInteraction = Math.max(0, reloadAmountPerInteraction);
	}

	public long startedAtMs() {
		return this.startedAtMs;
	}

	public void setStartedAtMs(long startedAtMs) {
		this.startedAtMs = Math.max(0L, startedAtMs);
	}

	public long reloadDurationMs() {
		return this.reloadDurationMs;
	}

	public void setReloadDurationMs(long reloadDurationMs) {
		this.reloadDurationMs = Math.max(0L, reloadDurationMs);
	}

	public long readyAtMs() {
		return this.startedAtMs + this.reloadDurationMs;
	}

	public void clear() {
		this.active = false;
		this.playerUuid = null;
		this.ammoItemId = null;
		this.ammoIcon = null;
		this.ammoBeforeReload = 0;
		this.maxAmmo = 0;
		this.reloadAmountPerInteraction = 0;
		this.startedAtMs = 0L;
		this.reloadDurationMs = 0L;
	}

	@Override
	public WeaponReloadComponent clone() {
		WeaponReloadComponent copy = new WeaponReloadComponent();
		copy.active = this.active;
		copy.playerUuid = this.playerUuid;
		copy.ammoItemId = this.ammoItemId;
		copy.ammoIcon = this.ammoIcon;
		copy.ammoBeforeReload = this.ammoBeforeReload;
		copy.maxAmmo = this.maxAmmo;
		copy.reloadAmountPerInteraction = this.reloadAmountPerInteraction;
		copy.startedAtMs = this.startedAtMs;
		copy.reloadDurationMs = this.reloadDurationMs;
		return copy;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (!(obj instanceof WeaponReloadComponent other)) {
			return false;
		}

		return this.active == other.active && Objects.equals(this.playerUuid, other.playerUuid)
			&& Objects.equals(this.ammoItemId, other.ammoItemId) && Objects.equals(this.ammoIcon, other.ammoIcon)
			&& this.ammoBeforeReload == other.ammoBeforeReload && this.maxAmmo == other.maxAmmo
			&& this.reloadAmountPerInteraction == other.reloadAmountPerInteraction && this.startedAtMs == other.startedAtMs
			&& this.reloadDurationMs == other.reloadDurationMs;
	}

	@Override
	public int hashCode() {
		return Objects.hash(
			this.active, this.playerUuid, this.ammoItemId, this.ammoIcon, this.ammoBeforeReload, this.maxAmmo,
			this.reloadAmountPerInteraction, this.startedAtMs, this.reloadDurationMs
		);
	}
}
