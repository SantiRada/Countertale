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

public final class CountertaleHudBridge {

    private CountertaleHudBridge() {
    }

    public static void updateAmmo(PlayerRef playerRef, @Nullable Player player, @Nullable ItemStack itemStack) {
        if (playerRef == null || player == null || itemStack == null) {
            hideAmmo(player);
            return;
        }

        String itemId = itemStack.getItemId();
        updateHeldWeaponVisual(player, itemId);
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
            return;
        }

        int currentAmmo = ammoState != null && ammoState.initialized()
                ? ammoState.effectiveAmmo()
                : maxAmmo;

        if (currentAmmo < 0) currentAmmo = 0;
        if (currentAmmo > maxAmmo) currentAmmo = maxAmmo;

        int reserveAmmo = 0;

        GunSettings gunSettings = WeaponContentApi.getSettings(itemId);
        WeaponAmmoSettings weaponAmmo = gunSettings != null ? gunSettings.ammo() : null;

        if (weaponAmmo != null) {
            String ammoItemId = AmmoService.resolvePreferredAmmoItemId(
                    itemStack,
                    weaponAmmo,
                    AmmoService.getAmmoContainer(player)
            );

            int countedAmmo = AmmoService.countAmmo(
                    AmmoService.getAmmoContainer(player),
                    ammoItemId
            );

            reserveAmmo = Math.max(0, countedAmmo);
        }

        boolean reloading = ReloadManager.isReloading(playerRef);

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
    private static void updateHeldWeaponVisual(Player player, String itemId) {
        CustomUIHud hud = player.getHudManager().getCustomHud();

        if (!(hud instanceof GameHUD gameHud)) {
            return;
        }

        CountertaleWeaponVisuals.Visual visual = CountertaleWeaponVisuals.resolve(itemId);
        if (visual == null) {
            return;
        }

        int activeSlot = player.getInventory().getActiveHotbarSlot() + 1;

        gameHud.setHygunsWeaponVisual(
                visual.image(),
                visual.crosshair(),
                visual.firemode(),
                activeSlot
        );
    }
}