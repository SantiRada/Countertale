/*
 * Copyright (c) 2026 Alan Franzin
 * SPDX-License-Identifier: MIT
 */
package com.thescar.hygunsplugin.ui.hud.scope;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import org.jetbrains.annotations.NotNull;

public class EmptyHud extends CustomUIHud {
	public EmptyHud(@NotNull PlayerRef playerRef) {
		super(playerRef);
	}

	@Override
	protected void build(@NotNull UICommandBuilder uiCommandBuilder) {
		uiCommandBuilder.append("Scope/Clean.ui");
	}
}
