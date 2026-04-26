package com.thescar.hygunsplugin.ui.hud.screens;

import com.thescar.hygunsplugin.ui.hud.core.HudScreenContract;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

import javax.annotation.Nullable;

public final class ScopeOverlayScreenContract implements HudScreenContract<ScopeOverlayScreenContract.State> {
	public static final String SCREEN_ID = "hyguns.scope.overlay";
	private static final String SCOPE_UI_PATH = "Scope/Scope_Overlay.ui";
	private static final String SCOPE_BACKGROUND_SELECTOR = "#HygunsScopeOverlayRoot.Background";
	private static final String DEFAULT_SCOPE_TEXTURE = "Scope/Scope_1.png";

	@Nullable
	private static String normalizedTexturePath(@Nullable State state) {
		if (state == null) {
			return null;
		}
		if (state.overlayTexturePath == null || state.overlayTexturePath.isBlank()) {
			return null;
		}
		return state.overlayTexturePath.trim().replace("\\", "/");
	}

	@Override

	public String id() {
		return SCREEN_ID;
	}

	@Override
	public int zIndex() {
		return 10;
	}

	@Override
	public Class<State> stateType() {
		return State.class;
	}

	@Override
	public void build(@Nullable State state, UICommandBuilder uiCommandBuilder) {
		uiCommandBuilder.append(SCOPE_UI_PATH);
		String texturePath = normalizedTexturePath(state);
		if (texturePath == null) {
			return;
		}
		uiCommandBuilder.set(SCOPE_BACKGROUND_SELECTOR, texturePath);
	}

	@Override
	public boolean patch(@Nullable State previousState, @Nullable State nextState, UICommandBuilder uiCommandBuilder) {
		String previousTexturePath = normalizedTexturePath(previousState);
		String nextTexturePath = normalizedTexturePath(nextState);
		if (previousTexturePath == null && nextTexturePath == null) {
			return true;
		}
		if (previousTexturePath != null && previousTexturePath.equals(nextTexturePath)) {
			return true;
		}
		if (nextTexturePath == null) {
			uiCommandBuilder.set(SCOPE_BACKGROUND_SELECTOR, DEFAULT_SCOPE_TEXTURE);
			return true;
		}

		uiCommandBuilder.set(SCOPE_BACKGROUND_SELECTOR, nextTexturePath);
		return true;
	}

	public record State(@Nullable String overlayTexturePath) {
	}
}
