package com.thescar.hygunsplugin.ui.hud.screens;

import com.thescar.hygunsplugin.support.text.ValueUtils;
import com.thescar.hygunsplugin.ui.hud.core.HudScreenContract;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

import javax.annotation.Nullable;

public final class HeatScreenContract implements HudScreenContract<HeatScreenContract.State> {
	public static final String SCREEN_ID = "hyguns.heat";
	private static final String OVERHEAT_ROOT_VISIBLE_SELECTOR = "#OverheatRoot.Visible";
	private static final String OVERHEAT_PROGRESS_SELECTOR = "#OverheatProgress.Value";
	private static final String OVERHEAT_COLOR_SELECTOR = "#OverheatProgress.Color";

	private static final State HIDDEN = new State(false, 0.0F, HeatUiSettings.defaults());

	private static void apply(UICommandBuilder b, State state, boolean visible) {
		HeatUiSettings ui = state.uiSettings != null
		                    ? state.uiSettings
		                    : HeatUiSettings.defaults();
		boolean heatVisible = visible && state.visible;
		b.set(OVERHEAT_ROOT_VISIBLE_SELECTOR, heatVisible);
		if (!heatVisible) {
			b.set(OVERHEAT_PROGRESS_SELECTOR, 0.0F);
			b.set(OVERHEAT_COLOR_SELECTOR, "#FFFFFF");
			return;
		}

		float heatProgress = ValueUtils.Numbers.clamp(state.progress, 0.0F, 1.0F);
		b.set(OVERHEAT_PROGRESS_SELECTOR, ValueUtils.Numbers.remapClamped(heatProgress, 0.0F, 1.0F, ui.valueMin(), ui.valueMax()));
		b.set(OVERHEAT_COLOR_SELECTOR, ui.gradient().colorAt(heatProgress, 0.0F, 1.0F));
	}

	private static String uiDefinition(HeatUiSettings ui) {
		int rootLeft = Math.round(ui.rootLeft());
		int rootTop = Math.round(ui.rootTop());
		int width = Math.round(ui.width());
		int height = Math.round(ui.height());
		int heatLeft = Math.round(ui.heatLeft()) + 1;
		int heatTop = Math.round(ui.heatTop()) + 1;
		int outlineLeft = Math.round(ui.outlineLeft());
		int outlineTop = Math.round(ui.outlineTop());
		int overlayLeft = Math.round(ui.overlayLeft());
		int overlayTop = Math.round(ui.overlayTop());

		return """
			Group #OverheatRoot {
			  LayoutMode: Center;
			  Anchor: (Left: 0, Top: 0);
			  Visible: false;
			
			  Group #Indicator {
			    Anchor: (Width: %d, Height: %d);
			    Padding: (Left: %d, Top: %d);
			
			    Group #OverheatProgressOutline {
			      Anchor: (Width: %d, Height: %d, Left: %d, Top: %d);
			      Background: "%s";
			    }
			
			    CircularProgressBar #OverheatProgress {
			      Anchor: (Width: %d, Height: %d, Left: %d, Top: %d);
			      Background: #1a2030;
			      Color: #aa7c4a;
			      Value: 0.0;
			      MaskTexturePath: "%s";
			    }
			
			    Group #OverheatProgressOverlay {
			      Anchor: (Width: %d, Height: %d, Left: %d, Top: %d);
			      Background: "%s";
			    }
			  }
			}
			""".formatted(
			//Indicator
			width, height, rootLeft, rootTop,
			//Outline
			width, height, outlineLeft, outlineTop,
			escape(inlineTexturePath(ui.outlineTexturePath())),
			//Progress
			width - 1, height - 1, heatLeft, heatTop,
			escape(inlineTexturePath(ui.heatTexturePath())),
			//Overlay
			width, height, overlayLeft, overlayTop,
			escape(inlineTexturePath(ui.overlayTexturePath()))
		);
	}

	private static String inlineTexturePath(String value) {
		if (value == null || value.isBlank()) {
			return "";
		}
		String path = value.trim().replace('\\', '/');
		if (path.contains("/")) {
			return path;
		}
		return "Hud/" + path;
	}

	private static String escape(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	@Override
	public String id() {
		return SCREEN_ID;
	}

	@Override
	public int zIndex() {
		return 110;
	}

	@Override
	public Class<State> stateType() {
		return State.class;
	}

	@Override
	public void build(@Nullable State state, UICommandBuilder uiCommandBuilder) {
		State resolvedState = state == null
		                      ? HIDDEN
		                      : state;
		HeatUiSettings ui = resolvedState.uiSettings != null
		                    ? resolvedState.uiSettings
		                    : HeatUiSettings.defaults();
		uiCommandBuilder.appendInline(null, uiDefinition(ui));
		apply(uiCommandBuilder, resolvedState, state != null);
	}

	@Override
	public boolean patch(@Nullable State previousState, @Nullable State nextState, UICommandBuilder uiCommandBuilder) {
		if (nextState == null || !nextState.visible) {
			apply(
				uiCommandBuilder, nextState == null
				                  ? HIDDEN
				                  : nextState, nextState != null
			);
			return true;
		}

		HeatUiSettings previousUi = previousState != null && previousState.uiSettings != null
		                            ? previousState.uiSettings
		                            : HeatUiSettings.defaults();
		HeatUiSettings nextUi = nextState != null && nextState.uiSettings != null
		                        ? nextState.uiSettings
		                        : HeatUiSettings.defaults();
		if (!previousUi.equals(nextUi)) {
			return false;
		}

		apply(
			uiCommandBuilder, nextState == null
			                  ? HIDDEN
			                  : nextState, nextState != null
		);
		return true;
	}

	public record State(boolean visible, float progress, HeatUiSettings uiSettings) {
	}
}
