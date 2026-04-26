package com.thescar.hygunsplugin.ui.hud.features;

import com.thescar.hygunsplugin.content.ammo.AmmoContentApi;
import com.thescar.hygunsplugin.content.ammo.AmmoDefinition;
import com.thescar.hygunsplugin.content.settings.GunSettings;
import com.thescar.hygunsplugin.content.settings.WeaponAmmoSettings;
import com.thescar.hygunsplugin.content.weapon.WeaponContentApi;
import com.thescar.hygunsplugin.gameplay.ammo.AmmoService;
import com.thescar.hygunsplugin.gameplay.reload.ReloadManager;
import com.thescar.hygunsplugin.runtime.components.AmmoDataComponent;
import com.thescar.hygunsplugin.runtime.components.HeatDataComponent;
import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeEcs;
import com.thescar.hygunsplugin.runtime.item.core.RuntimeItemIdentity;
import com.thescar.hygunsplugin.runtime.item.core.RuntimeItemRef;
import com.thescar.hygunsplugin.runtime.logic.RuntimeHeatLogic;
import com.thescar.hygunsplugin.support.hytale.PlayerInventoryAccess;
import com.thescar.hygunsplugin.ui.hud.core.HudScreenRuntime;
import com.thescar.hygunsplugin.ui.hud.screens.AmmoScreenContract;
import com.thescar.hygunsplugin.ui.hud.screens.HeatScreenContract;
import com.thescar.hygunsplugin.ui.hud.screens.HeatUiSettings;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AmmoHudFeature {
	private final HudScreenRuntime runtime;

	private final ConcurrentHashMap<UUID, AmmoScreenContract.State> stateCache = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<UUID, HeatScreenContract.State> heatStateCache = new ConcurrentHashMap<>();

	public AmmoHudFeature(HudScreenRuntime runtime) {
		this.runtime = runtime;
	}

	@Nullable
	private static String resolveAmmoIcon(@Nullable Player player, @Nullable ItemStack stack, @Nullable String resolvedAmmoItemId) {
		if (stack == null) {
			return null;
		}
		RuntimeItemRef runtimeRef = RuntimeItemIdentity.resolve(stack);
		AmmoDataComponent ammo = ItemRuntimeEcs.getComponent(runtimeRef, AmmoDataComponent.getComponentType());
		String icon = ammo != null && ammo.initialized()
		              ? ammo.loadedAmmoIcon()
		              : null;
		if (icon != null && !icon.isBlank()) {
			return icon.trim();
		}
		String selectedAmmoItemId = ammo != null && ammo.initialized()
		                            ? ammo.selectedAmmoItemId()
		                            : null;
		AmmoDefinition selectedAmmo = AmmoContentApi.getAmmo(selectedAmmoItemId);
		if (selectedAmmo != null && selectedAmmo.settings() != null) {
			String selectedIcon = selectedAmmo.settings().icon();
			if (selectedIcon != null && !selectedIcon.isBlank()) {
				return selectedIcon.trim();
			}
		}

		String ammoItemId = resolvedAmmoItemId;
		if ((ammoItemId == null || ammoItemId.isBlank()) && player != null) {
			ammoItemId = resolveTrackedAmmoItemId(player, stack);
		}

		AmmoDefinition resolvedAmmo = AmmoContentApi.getAmmo(ammoItemId);
		if (resolvedAmmo != null && resolvedAmmo.settings() != null) {
			String resolvedIcon = resolvedAmmo.settings().icon();
			if (resolvedIcon != null && !resolvedIcon.isBlank()) {
				return resolvedIcon.trim();
			}
		}

		return null;
	}

	@Nullable
	private static String resolveWeaponIcon(@Nullable ItemStack stack) {
		if (stack == null) {
			return null;
		}
		GunSettings settings = WeaponContentApi.getSettings(stack.getItemId());
		if (settings != null && settings.weaponIcon() != null && !settings.weaponIcon().isBlank()) {
			return settings.weaponIcon().trim();
		}

		return null;
	}

	@Nullable
	private static String resolveTrackedAmmoItemId(@Nullable Player player, @Nullable ItemStack stack) {
		if (stack == null) {
			return null;
		}
		return resolveTrackedAmmoItemId(player, stack.getItemId(), stack);
	}

	@Nullable
	private static String resolveTrackedAmmoItemId(@Nullable Player player, @Nullable String itemId, @Nullable ItemStack gunStack) {
		if (itemId == null || itemId.isBlank()) {
			return null;
		}
		GunSettings settings = WeaponContentApi.getSettings(itemId);
		WeaponAmmoSettings weaponAmmo = settings != null
		                                ? settings.ammo()
		                                : null;
		if (weaponAmmo == null) {
			return null;
		}

		return AmmoService.resolvePreferredAmmoItemId(gunStack, weaponAmmo, AmmoService.getAmmoContainer(player));
	}

	private static int countAmmoInInventory(@Nullable Player player, @Nullable String ammoItemId) {
		return AmmoService.countAmmo(AmmoService.getAmmoContainer(player), ammoItemId);
	}

	@Nullable
	private static Integer resolveMaxAmmo(@Nullable String itemId, @Nullable AmmoDataComponent ammoState) {
		if (ammoState != null && ammoState.initialized()) {
			return ammoState.maxAmmo();
		}

		return WeaponContentApi.getDefaultMaxAmmo(itemId);
	}

	public void register() {
		runtime.register(new AmmoScreenContract());
		runtime.register(new HeatScreenContract());
	}

	public void resetPlayerState(@Nullable UUID playerId) {
		if (playerId == null) {
			return;
		}
		stateCache.remove(playerId);
		heatStateCache.remove(playerId);
	}

	public long computeSignature(@Nullable AmmoSnapshot snapshot) {
		if (snapshot == null || !snapshot.visible) {
			return 0L;
		}
		long h = 1125899906842597L;
		h = 31L * h + (snapshot.itemId == null
		               ? 0
		               : snapshot.itemId.hashCode());
		h = 31L * h + snapshot.ammo;
		h = 31L * h + snapshot.reserveAmmo;
		h = 31L * h + (snapshot.reloading
		               ? 1
		               : 0);
		h = 31L * h + (snapshot.ammoIcon == null
		               ? 0
		               : snapshot.ammoIcon.hashCode());
		h = 31L * h + (snapshot.weaponIcon == null
		               ? 0
		               : snapshot.weaponIcon.hashCode());
		return h;
	}

	public long computeHeatSignature(@Nullable HeatSnapshot snapshot) {
		if (snapshot == null || !snapshot.visible) {
			return 0L;
		}
		long h = 1125899906842597L;
		h = 31L * h + Float.floatToIntBits(snapshot.progress);
		h = 31L * h + snapshot.uiSettings.hashCode();
		return h;
	}

	public AmmoSnapshot snapshotForPlayer(@Nullable Player player, @Nullable PlayerRef playerRef) {
		if (player == null || playerRef == null) {
			return AmmoSnapshot.hidden();
		}

		ItemStack held = PlayerInventoryAccess.getItemInHand(player);
		if (held == null) {
			return AmmoSnapshot.hidden();
		}
		String itemId = held.getItemId();
		RuntimeItemRef runtimeRef = RuntimeItemIdentity.resolve(held);
		AmmoDataComponent ammoState = ItemRuntimeEcs.getComponent(runtimeRef, AmmoDataComponent.getComponentType());
		Integer maxAmmo = resolveMaxAmmo(itemId, ammoState);
		if (maxAmmo == null || maxAmmo <= 0) {
			return AmmoSnapshot.hidden();
		}
		Integer ammo = ammoState != null && ammoState.initialized()
		               ? ammoState.effectiveAmmo()
		               : maxAmmo;
		if (ammo < 0) {
			ammo = 0;
		} else if (ammo > maxAmmo) {
			ammo = maxAmmo;
		}
		String ammoItemId = resolveTrackedAmmoItemId(player, held);
		String ammoIcon = resolveAmmoIcon(player, held, ammoItemId);
		String weaponIcon = resolveWeaponIcon(held);
		int reserveAmmo = countAmmoInInventory(player, ammoItemId);
		if (reserveAmmo < 0) {
			reserveAmmo = 0;
		}
		return new AmmoSnapshot(
			true, itemId, ammo, maxAmmo, reserveAmmo, ReloadManager.isReloading(playerRef), ammoIcon, weaponIcon
		);
	}

	public HeatSnapshot heatSnapshotForPlayer(@Nullable Player player) {
		if (player == null) {
			return HeatSnapshot.hidden();
		}

		ItemStack held = PlayerInventoryAccess.getItemInHand(player);
		if (held == null) {
			return HeatSnapshot.hidden();
		}

		RuntimeItemRef runtimeRef = RuntimeItemIdentity.resolve(held);
		HeatDataComponent heatState = ItemRuntimeEcs.getComponent(runtimeRef, HeatDataComponent.getComponentType());
		int nowMs = RuntimeHeatLogic.wrappedNowMillis();
		float overheatProgress = RuntimeHeatLogic.currentHeatProgress(heatState, nowMs);
		HeatUiSettings uiSettings = heatState != null
		                            ? heatState.uiSettings()
		                            : HeatUiSettings.defaults();
		return new HeatSnapshot(overheatProgress > 0.0F, overheatProgress, uiSettings);
	}

	public void updateFromSnapshot(@Nullable PlayerRef playerRef, AmmoSnapshot snapshot) {
		if (playerRef == null) {
			return;
		}
		if (snapshot == null || !snapshot.visible) {
			hide(playerRef);
			return;
		}

		AmmoScreenContract.State state = new AmmoScreenContract.State(
			snapshot.itemId, snapshot.ammo, snapshot.maxAmmo, snapshot.reserveAmmo,
			snapshot.reloading, snapshot.ammoIcon, snapshot.weaponIcon
		);
		stateCache.put(playerRef.getUuid(), state);
		runtime.show(playerRef, AmmoScreenContract.SCREEN_ID);
		runtime.setState(playerRef, AmmoScreenContract.SCREEN_ID, state);
	}

	public void updateHeatFromSnapshot(@Nullable PlayerRef playerRef, HeatSnapshot snapshot) {
		if (playerRef == null) {
			return;
		}
		if (snapshot == null || !snapshot.visible) {
			hideHeat(playerRef);
			return;
		}

		HeatScreenContract.State state = new HeatScreenContract.State(snapshot.visible, snapshot.progress, snapshot.uiSettings);
		heatStateCache.put(playerRef.getUuid(), state);
		runtime.show(playerRef, HeatScreenContract.SCREEN_ID);
		runtime.setState(playerRef, HeatScreenContract.SCREEN_ID, state);
	}

	public void updateFromValues(@Nullable PlayerRef playerRef, @Nullable Player player, @Nullable String itemId, int ammo, int maxAmmo,
	                             boolean reloading, @Nullable String ammoIcon, @Nullable String weaponIcon) {
		if (playerRef == null) {
			return;
		}
		if (maxAmmo <= 0 || itemId == null || itemId.isBlank()) {
			hide(playerRef);
			return;
		}

		int safeAmmo = Math.max(0, Math.min(ammo, maxAmmo));
		int reserveAmmo = 0;
		if (player != null) {
			String ammoItemId = resolveTrackedAmmoItemId(player, itemId, null);
			int computed = countAmmoInInventory(player, ammoItemId);
			reserveAmmo = (computed >= 0)
			              ? computed
			              : 0;
		}

		updateFromSnapshot(
			playerRef, new AmmoSnapshot(true, itemId, safeAmmo, maxAmmo, reserveAmmo, reloading, ammoIcon, weaponIcon)
		);
		updateHeatFromSnapshot(playerRef, HeatSnapshot.hidden());
	}

	public void updateFromItemStack(@Nullable PlayerRef playerRef, @Nullable Player player, @Nullable ItemStack itemStack) {
		if (playerRef == null) {
			return;
		}
		if (itemStack == null) {
			hide(playerRef);
			return;
		}

		String itemId = itemStack.getItemId();
		RuntimeItemRef runtimeRef = RuntimeItemIdentity.resolve(itemStack);
		AmmoDataComponent ammoState = ItemRuntimeEcs.getComponent(runtimeRef, AmmoDataComponent.getComponentType());
		Integer maxAmmo = resolveMaxAmmo(itemId, ammoState);
		if (maxAmmo == null || maxAmmo <= 0) {
			hide(playerRef);
			return;
		}

		Integer ammo = ammoState != null && ammoState.initialized()
		               ? ammoState.effectiveAmmo()
		               : maxAmmo;
		if (ammo < 0) {
			ammo = 0;
		} else if (ammo > maxAmmo) {
			ammo = maxAmmo;
		}
		int reserveAmmo = 0;
		String ammoItemId = null;
		if (player != null) {
			ammoItemId = resolveTrackedAmmoItemId(player, itemStack);
			int computed = countAmmoInInventory(player, ammoItemId);
			reserveAmmo = (computed >= 0)
			              ? computed
			              : 0;
		}

		updateFromSnapshot(
			playerRef, new AmmoSnapshot(
				true, itemId, ammo, maxAmmo, reserveAmmo, ReloadManager.isReloading(playerRef),
				resolveAmmoIcon(player, itemStack, ammoItemId), resolveWeaponIcon(itemStack)
			)
		);
		updateHeatFromSnapshot(playerRef, heatSnapshotForPlayer(player));
	}

	public void hide(@Nullable PlayerRef playerRef) {
		if (playerRef == null) {
			return;
		}
		runtime.hide(playerRef, AmmoScreenContract.SCREEN_ID);
		hideHeat(playerRef);
	}

	public void hideHeat(@Nullable PlayerRef playerRef) {
		if (playerRef == null) {
			return;
		}
		HeatScreenContract.State previous = heatStateCache.get(playerRef.getUuid());
		HeatUiSettings uiSettings = previous != null && previous.uiSettings() != null
		                            ? previous.uiSettings()
		                            : HeatUiSettings.defaults();
		HeatScreenContract.State hidden = new HeatScreenContract.State(false, 0.0F, uiSettings);
		heatStateCache.put(playerRef.getUuid(), hidden);
		if (runtime.isScreenVisible(playerRef, HeatScreenContract.SCREEN_ID)) {
			runtime.setState(playerRef, HeatScreenContract.SCREEN_ID, hidden);
		}
	}

	public boolean isHeatVisible(@Nullable PlayerRef playerRef) {
		if (playerRef == null) {
			return false;
		}
		HeatScreenContract.State state = heatStateCache.get(playerRef.getUuid());
		return state != null && state.visible();
	}

	public void show(@Nullable PlayerRef playerRef) {
		if (playerRef == null) {
			return;
		}
		runtime.show(playerRef, AmmoScreenContract.SCREEN_ID);
		runtime.setState(playerRef, AmmoScreenContract.SCREEN_ID, getOrCreateState(playerRef));
	}

	public AmmoScreenContract.State getOrCreateState(@Nullable PlayerRef playerRef) {
		if (playerRef == null) {
			return new AmmoScreenContract.State("", 0, 0, 0, false, null, null);
		}

		return stateCache.computeIfAbsent(
			playerRef.getUuid(),
			ignored -> new AmmoScreenContract.State("", 0, 0, 0, false, null, null)
		);
	}

	public void setState(@Nullable PlayerRef playerRef, AmmoScreenContract.State state) {
		if (playerRef == null || state == null) {
			return;
		}
		stateCache.put(playerRef.getUuid(), state);
		runtime.setState(playerRef, AmmoScreenContract.SCREEN_ID, state);
	}

	public void setItemId(@Nullable PlayerRef playerRef, @Nullable String itemId) {
		if (playerRef == null) {
			return;
		}
		AmmoScreenContract.State s = getOrCreateState(playerRef);
		setState(
			playerRef, new AmmoScreenContract.State(
				itemId == null
				? ""
				: itemId, s.ammo(), s.maxAmmo(), s.reserveAmmo(), s.reloading(),
				s.ammoIcon(), s.weaponIcon()
			)
		);
	}

	public void setAmmo(@Nullable PlayerRef playerRef, int ammo) {
		if (playerRef == null) {
			return;
		}
		AmmoScreenContract.State s = getOrCreateState(playerRef);
		setState(
			playerRef, new AmmoScreenContract.State(
				s.itemId(), Math.max(0, ammo), s.maxAmmo(), s.reserveAmmo(), s.reloading(), s.ammoIcon(),
				s.weaponIcon()
			)
		);
	}

	public void setMaxAmmo(@Nullable PlayerRef playerRef, int maxAmmo) {
		if (playerRef == null) {
			return;
		}
		AmmoScreenContract.State s = getOrCreateState(playerRef);
		setState(
			playerRef, new AmmoScreenContract.State(
				s.itemId(), s.ammo(), Math.max(0, maxAmmo), s.reserveAmmo(), s.reloading(), s.ammoIcon(),
				s.weaponIcon()
			)
		);
	}

	public void setReserveAmmo(@Nullable PlayerRef playerRef, int reserveAmmo) {
		if (playerRef == null) {
			return;
		}
		AmmoScreenContract.State s = getOrCreateState(playerRef);
		setState(
			playerRef, new AmmoScreenContract.State(
				s.itemId(), s.ammo(), s.maxAmmo(), Math.max(0, reserveAmmo), s.reloading(), s.ammoIcon(),
				s.weaponIcon()
			)
		);
	}

	public void setReloading(@Nullable PlayerRef playerRef, boolean reloading) {
		if (playerRef == null) {
			return;
		}
		AmmoScreenContract.State s = getOrCreateState(playerRef);
		setState(
			playerRef, new AmmoScreenContract.State(
				s.itemId(), s.ammo(), s.maxAmmo(), s.reserveAmmo(), reloading, s.ammoIcon(), s.weaponIcon()
			)
		);
	}

	public void setAmmoIcon(@Nullable PlayerRef playerRef, @Nullable String ammoIcon) {
		if (playerRef == null) {
			return;
		}
		AmmoScreenContract.State s = getOrCreateState(playerRef);
		setState(
			playerRef, new AmmoScreenContract.State(
				s.itemId(), s.ammo(), s.maxAmmo(), s.reserveAmmo(), s.reloading(), ammoIcon, s.weaponIcon()
			)
		);
	}

	public void setWeaponIcon(@Nullable PlayerRef playerRef, @Nullable String weaponIcon) {
		if (playerRef == null) {
			return;
		}
		AmmoScreenContract.State s = getOrCreateState(playerRef);
		setState(
			playerRef, new AmmoScreenContract.State(
				s.itemId(), s.ammo(), s.maxAmmo(), s.reserveAmmo(), s.reloading(), s.ammoIcon(), weaponIcon
			)
		);
	}

	public record AmmoSnapshot(
		boolean visible, @Nullable String itemId, int ammo, int maxAmmo, int reserveAmmo, boolean reloading,
		@Nullable String ammoIcon, @Nullable String weaponIcon
	) {
		public static AmmoSnapshot hidden() {
			return new AmmoSnapshot(false, null, 0, 0, 0, false, null, null);
		}
	}

	public record HeatSnapshot(boolean visible, float progress, HeatUiSettings uiSettings) {
		public static HeatSnapshot hidden() {
			return new HeatSnapshot(false, 0.0F, HeatUiSettings.defaults());
		}
	}
}
