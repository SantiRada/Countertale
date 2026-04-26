package com.thescar.hygunsplugin.ui.hud.core;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

final class HudScreenStackHud extends CustomUIHud {
	private final List<RenderEntry<?>> entries;

	HudScreenStackHud(@NotNull PlayerRef playerRef, @NotNull List<RenderEntry<?>> entries) {
		super(playerRef);
		this.entries = List.copyOf(entries);
	}

	@Override

	protected void build(@NotNull UICommandBuilder uiCommandBuilder) {
		for (RenderEntry<?> entry : entries) {
			entry.build(uiCommandBuilder);
		}
	}

	record RenderEntry<T>(HudScreenContract<T> contract, @Nullable T state) {
		void build(UICommandBuilder uiCommandBuilder) {
			contract.build(state, uiCommandBuilder);
		}
	}
}
