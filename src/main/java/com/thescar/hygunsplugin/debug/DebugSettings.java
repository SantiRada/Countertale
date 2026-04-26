package com.thescar.hygunsplugin.debug;

import com.thescar.hygunsplugin.config.HygunsConfig;

import com.hypixel.hytale.server.core.util.Config;

import java.util.concurrent.atomic.AtomicBoolean;

public final class DebugSettings {
	private static final AtomicBoolean MOD_DEBUG_ENABLED = new AtomicBoolean(false);
	private static volatile Config<HygunsConfig> config;

	private DebugSettings() {
	}

	public static void initialize(Config<HygunsConfig> loadedConfig) {
		loadedConfig.load().join();
		HygunsConfig state = loadedConfig.get();
		if (state == null) {
			state = new HygunsConfig();
		}

		MOD_DEBUG_ENABLED.set(state.isDebug());
		config = loadedConfig;
		loadedConfig.save();
	}

	public static boolean isEnabled() {
		return MOD_DEBUG_ENABLED.get();
	}

	public static void setEnabled(boolean enabled) {
		MOD_DEBUG_ENABLED.set(enabled);
		Config<HygunsConfig> currentConfig = config;
		if (currentConfig == null) {
			return;
		}

		HygunsConfig state = currentConfig.get();
		if (state == null) {
			state = new HygunsConfig();
		}

		state.setDebug(enabled);
		currentConfig.save();
	}
}
