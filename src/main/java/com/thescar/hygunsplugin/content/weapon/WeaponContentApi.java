package com.thescar.hygunsplugin.content.weapon;

import com.thescar.hygunsplugin.content.registry.GunRegistry;
import com.thescar.hygunsplugin.content.settings.GunSettings;
import com.thescar.hygunsplugin.content.settings.GunSettingsMerger;

import javax.annotation.Nullable;

public final class WeaponContentApi {
	private WeaponContentApi() {
	}

	@Nullable
	public static GunSettings getSettings(@Nullable String itemId) {
		return GunRegistry.getSettings(itemId);
	}

	@Nullable
	public static Integer getDefaultMaxAmmo(@Nullable String itemId) {
		return GunRegistry.getDefaultMaxAmmo(itemId);
	}

	@Nullable
	public static GunSettings mergeSettings(@Nullable GunSettings... settings) {
		if (settings == null || settings.length == 0) {
			return null;
		}

		if (settings.length == 1) {
			return settings[0];
		}

		if (settings.length == 2) {
			return GunSettingsMerger.merge(settings[0], settings[1]);
		}

		return GunSettingsMerger.merge(settings[0], settings[1], settings[2]);
	}
}
