package com.thescar.hygunsplugin.ui.hud.core;

import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;

public final class HudMultiplexerBackend {
	private HudMultiplexerBackend() {
	}

	public static Type resolve() {
		// 1) Priority: MultipleHUD.
		if (isEnabledPlugin("buuz135", "multiplehud") || MultipleHudBridge.isAvailable()) {
			return Type.MULTIPLE_HUD;
		}

		// 2) Then AutoMultiHud.
		if (isEnabledPlugin("dairymoose", "automultihud") || AutoMultiHudBridge.isAvailable()) {
			return Type.AUTO_MULTI_HUD;
		}

		// 3) Fallback: our universal multiplexer.
		return Type.UNIVERSAL;
	}

	private static boolean isEnabledPlugin(String group, String name) {
		if (group == null || name == null) {
			return false;
		}
		try {
			PluginManager manager = PluginManager.get();
			if (manager == null) {
				return false;
			}
			for (PluginBase plugin : manager.getPlugins()) {
				if (plugin == null || !plugin.isEnabled() || plugin.getIdentifier() == null) {
					continue;
				}
				String pluginGroup = plugin.getIdentifier().getGroup();
				String pluginName = plugin.getIdentifier().getName();
				if (pluginGroup == null || pluginName == null) {
					continue;
				}
				// Some distributions repackage group ids (e.g. AutoMultiHud:AutoMultiHud).
				// Match by plugin name first, with strict group+name as a secondary exact form.
				if (pluginName.equalsIgnoreCase(name) || (pluginGroup.equalsIgnoreCase(group) && pluginName.equalsIgnoreCase(name))) {
					return true;
				}
			}

		} catch (Throwable ignored) {
			return false;
		}

		return false;
	}

	public enum Type {
		MULTIPLE_HUD,
		AUTO_MULTI_HUD,
		UNIVERSAL
	}
}
