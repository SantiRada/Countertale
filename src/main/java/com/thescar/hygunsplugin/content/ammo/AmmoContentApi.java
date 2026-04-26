package com.thescar.hygunsplugin.content.ammo;

import com.thescar.hygunsplugin.content.registry.AmmoRegistry;
import com.thescar.hygunsplugin.content.settings.WeaponAmmoSettings;

import javax.annotation.Nullable;
import java.util.Map;

public final class AmmoContentApi {
	private AmmoContentApi() {
	}

	@Nullable
	public static AmmoDefinition getAmmo(@Nullable String itemId) {
		return AmmoRegistry.getAmmo(itemId);
	}

	public static boolean isCompatible(@Nullable WeaponAmmoSettings weaponAmmo, @Nullable AmmoDefinition ammo) {
		return AmmoRegistry.isCompatible(weaponAmmo, ammo);
	}

	@Nullable
	public static String resolveDefaultAmmoItemId(@Nullable Map<String, AmmoDefinition> compatibleAmmo) {
		return AmmoRegistry.resolveDefaultAmmoItemId(compatibleAmmo);
	}
}
