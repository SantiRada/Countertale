package com.thescar.hygunsplugin.ui.hud.core;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

import javax.annotation.Nullable;

/**
 * Generic contract for a single HUD screen/layer.
 *
 * @param <T> state type for the screen
 */
public interface HudScreenContract<T> {
	String id();

	int zIndex();

	Class<T> stateType();

	/**
	 * Build full UI for the screen.
	 */
	void build(@Nullable T state, UICommandBuilder uiCommandBuilder);

	/**
	 * Apply incremental update for the already built screen. Return true when patch
	 * was applied, false to request full rebuild.
	 */
	default boolean patch(@Nullable T previousState, @Nullable T nextState, UICommandBuilder uiCommandBuilder) {
		return false;
	}
}
