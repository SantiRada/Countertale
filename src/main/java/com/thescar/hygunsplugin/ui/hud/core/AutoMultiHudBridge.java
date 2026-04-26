package com.thescar.hygunsplugin.ui.hud.core;

/**
 * Lightweight compatibility probe for AutoMultiHud. No direct API calls are
 * needed: if the plugin is present, it will intercept outgoing CustomHud
 * packets.
 */
public final class AutoMultiHudBridge {
	private static final String AUTO_MULTI_HUD_MAIN_CLASS = "com.dairymoose.auto_multi_hud.AutoMultiHud";

	private static final Object LOCK = new Object();
	private static volatile boolean resolved;
	private static volatile boolean available;

	private AutoMultiHudBridge() {
	}

	public static boolean isAvailable() {
		resolveIfNeeded();
		return available;
	}

	private static void resolveIfNeeded() {
		if (resolved) {
			return;
		}
		synchronized (LOCK) {
			if (resolved) {
				return;
			}
			try {
				Class.forName(AUTO_MULTI_HUD_MAIN_CLASS);
				available = true;
			} catch (Throwable ignored) {
				available = false;
			} finally {
				resolved = true;
			}
		}
	}
}
