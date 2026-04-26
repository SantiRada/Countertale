package com.thescar.hygunsplugin.ui.hud.screens;

import com.thescar.hygunsplugin.ui.hud.core.HudScreenContract;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

import javax.annotation.Nullable;

public final class DebugScreenContract implements HudScreenContract<DebugScreenContract.State> {
	public static final String SCREEN_ID = "hyguns.debug";
	private static final String DEBUG_UI_PATH = "Hud/HygunsDebug.ui";
	private static final String ROOT_VISIBLE_SELECTOR = "#DebugRoot.Visible";
	private static final String INFO_TEXT_SELECTOR = "#DebugInfoText.TextSpans";
	private static final String METADATA_TEXT_SELECTOR = "#DebugMetadataText.TextSpans";

	private static void apply(UICommandBuilder b, @Nullable State state, boolean visible) {
		b.set(ROOT_VISIBLE_SELECTOR, visible);
		b.set(
			INFO_TEXT_SELECTOR, Message.raw(state != null
			                                ? state.infoText()
			                                : "")
		);
		b.set(
			METADATA_TEXT_SELECTOR, Message.raw(state != null
			                                    ? state.metadataText()
			                                    : "")
		);
	}

	@Override

	public String id() {
		return SCREEN_ID;
	}

	@Override
	public int zIndex() {
		return 1000;
	}

	@Override
	public Class<State> stateType() {
		return State.class;
	}

	@Override
	public void build(@Nullable State state, UICommandBuilder uiCommandBuilder) {
		uiCommandBuilder.append(DEBUG_UI_PATH);
		apply(uiCommandBuilder, state, state != null);
	}

	@Override
	public boolean patch(@Nullable State previousState, @Nullable State nextState, UICommandBuilder uiCommandBuilder) {
		apply(uiCommandBuilder, nextState, nextState != null);
		return true;
	}

	public record State(String infoText, String metadataText) {
	}
}
