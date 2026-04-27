package com.thescar.hygunsplugin.ui.hud;

import Tenzinn.Core.UI.GameHUD;

import com.thescar.hygunsplugin.content.settings.GunSettings;
import com.thescar.hygunsplugin.content.settings.WeaponAmmoSettings;
import com.thescar.hygunsplugin.content.weapon.WeaponContentApi;
import com.thescar.hygunsplugin.gameplay.ammo.AmmoService;
import com.thescar.hygunsplugin.gameplay.reload.ReloadManager;
import com.thescar.hygunsplugin.runtime.components.AmmoDataComponent;
import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeEcs;
import com.thescar.hygunsplugin.runtime.item.core.RuntimeItemIdentity;
import com.thescar.hygunsplugin.runtime.item.core.RuntimeItemRef;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OrbisOffensiveHudBridge {

    private static final long RESERVE_RECOUNT_INTERVAL_MS = 750L;

    private static final ConcurrentHashMap<UUID, String> LAST_VISUAL_KEY = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, String> LAST_AMMO_KEY = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, String> LAST_RESERVE_ITEM = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Integer> LAST_RESERVE_VALUE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> LAST_RESERVE_TIME = new ConcurrentHashMap<>();

    private OrbisOffensiveHudBridge() {
    }

    public static void resetPlayer(UUID uuid) {
        if (uuid == null) return;

        LAST_VISUAL_KEY.remove(uuid);
        LAST_AMMO_KEY.remove(uuid);
        LAST_RESERVE_ITEM.remove(uuid);
        LAST_RESERVE_VALUE.remove(uuid);
        LAST_RESERVE_TIME.remove(uuid);
    }

    public static void updateAmmo(PlayerRef playerRef, @Nullable Player player, @Nullable ItemStack itemStack) {
        if (playerRef == null || player == null || itemStack == null) {
            hideAmmo(player);
            return;
        }

        UUID uuid = playerRef.getUuid();
        String itemId = itemStack.getItemId();

        OrbisOffensiveWeaponVisuals.Visual visual = OrbisOffensiveWeaponVisuals.resolve(itemId);
        updateHeldWeaponVisualIfChanged(uuid, player, itemId, visual);

        if (visual != null && !visual.usesAmmo()) {
            hideAmmo(player);
            LAST_AMMO_KEY.remove(uuid);
            return;
        }

        RuntimeItemRef runtimeRef = RuntimeItemIdentity.resolve(itemStack);
        AmmoDataComponent ammoState = ItemRuntimeEcs.getComponent(runtimeRef, AmmoDataComponent.getComponentType());

        Integer maxAmmo = null;

        if (ammoState != null && ammoState.initialized()) {
            maxAmmo = ammoState.maxAmmo();
        }

        if (maxAmmo == null) {
            maxAmmo = WeaponContentApi.getDefaultMaxAmmo(itemId);
        }

        if (maxAmmo == null || maxAmmo <= 0) {
            hideAmmo(player);
            LAST_AMMO_KEY.remove(uuid);
            return;
        }

        int currentAmmo = ammoState != null && ammoState.initialized()
                ? ammoState.effectiveAmmo()
                : maxAmmo;

        if (currentAmmo < 0) currentAmmo = 0;
        if (currentAmmo > maxAmmo) currentAmmo = maxAmmo;

        int reserveAmmo = resolveReserveAmmo(uuid, player, itemStack);

        boolean reloading = ReloadManager.isReloading(playerRef);

        String ammoKey = itemId + "|" + currentAmmo + "|" + reserveAmmo + "|" + reloading;
        String previousAmmoKey = LAST_AMMO_KEY.get(uuid);

        if (ammoKey.equals(previousAmmoKey)) {
            return;
        }

        LAST_AMMO_KEY.put(uuid, ammoKey);

        CustomUIHud hud = player.getHudManager().getCustomHud();

        if (hud instanceof GameHUD gameHud) {
            gameHud.setHygunsAmmo(currentAmmo, reserveAmmo, reloading);
        }
    }

    public static void hideAmmo(@Nullable Player player) {
        if (player == null) return;

        CustomUIHud hud = player.getHudManager().getCustomHud();

        if (hud instanceof GameHUD gameHud) {
            gameHud.hideHygunsAmmo();
        }
    }

    private static void updateHeldWeaponVisualIfChanged(UUID uuid,
                                                        Player player,
                                                        String itemId,
                                                        @Nullable OrbisOffensiveWeaponVisuals.Visual visual) {
        if (uuid == null || player == null || visual == null) return;

        int activeSlot = player.getInventory().getActiveHotbarSlot() + 1;
        String visualKey = itemId + "|" + activeSlot + "|" + visual.image() + "|" + visual.crosshair() + "|" + visual.firemode();

        String previousVisualKey = LAST_VISUAL_KEY.get(uuid);

        if (visualKey.equals(previousVisualKey)) {
            return;
        }

        LAST_VISUAL_KEY.put(uuid, visualKey);

        CustomUIHud hud = player.getHudManager().getCustomHud();

        if (hud instanceof GameHUD gameHud) {
            gameHud.setHygunsWeaponVisual(
                    visual.image(),
                    visual.crosshair(),
                    visual.firemode(),
                    activeSlot
            );
        }
    }

    private static int resolveReserveAmmo(UUID uuid, Player player, ItemStack itemStack) {
        GunSettings gunSettings = WeaponContentApi.getSettings(itemStack.getItemId());
        WeaponAmmoSettings weaponAmmo = gunSettings != null ? gunSettings.ammo() : null;

        if (weaponAmmo == null) {
            return 0;
        }

        var container = AmmoService.getAmmoContainer(player);

        String ammoItemId = AmmoService.resolvePreferredAmmoItemId(
                itemStack,
                weaponAmmo,
                container
        );

        String reserveKey = itemStack.getItemId() + "|" + ammoItemId;
        long now = System.currentTimeMillis();

        String previousReserveKey = LAST_RESERVE_ITEM.get(uuid);
        Long previousTime = LAST_RESERVE_TIME.get(uuid);
        Integer previousValue = LAST_RESERVE_VALUE.get(uuid);

        if (reserveKey.equals(previousReserveKey)
                && previousTime != null
                && previousValue != null
                && now - previousTime < RESERVE_RECOUNT_INTERVAL_MS) {
            return previousValue;
        }

        int countedAmmo = AmmoService.countAmmo(container, ammoItemId);
        int reserveAmmo = Math.max(0, countedAmmo);

        LAST_RESERVE_ITEM.put(uuid, reserveKey);
        LAST_RESERVE_VALUE.put(uuid, reserveAmmo);
        LAST_RESERVE_TIME.put(uuid, now);

        return reserveAmmo;
    }
}