package com.thescar.hygunsplugin.ui.hud.features;

import com.thescar.hygunsplugin.ui.hud.core.HudScreenRuntime;
import com.thescar.hygunsplugin.ui.hud.screens.ScopeOverlayScreenContract;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ScopeOverlayHudFeature {
	private final HudScreenRuntime runtime;

	private final ConcurrentHashMap<UUID, ScopeOverlayScreenContract.State> stateCache = new ConcurrentHashMap<>();

	public ScopeOverlayHudFeature(HudScreenRuntime runtime) {
		this.runtime = runtime;
	}

	public void register() {
		runtime.register(new ScopeOverlayScreenContract());
	}

	public void resetPlayerState(@Nullable UUID playerId) {
		if (playerId == null) {
			return;
		}
		stateCache.remove(playerId);
	}

	public boolean show(@Nullable PlayerRef playerRef, @Nullable String overlayTexturePath) {
		if (playerRef == null) {
			return false;
		}
		ScopeOverlayScreenContract.State state = new ScopeOverlayScreenContract.State(overlayTexturePath);
		stateCache.put(playerRef.getUuid(), state);
		runtime.show(playerRef, ScopeOverlayScreenContract.SCREEN_ID);
		runtime.setState(playerRef, ScopeOverlayScreenContract.SCREEN_ID, state);
		return true;
	}

	public void hide(@Nullable PlayerRef playerRef) {
		if (playerRef == null) {
			return;
		}
		runtime.hide(playerRef, ScopeOverlayScreenContract.SCREEN_ID);
	}

	public void show(@Nullable PlayerRef playerRef) {
		if (playerRef == null) {
			return;
		}
		ScopeOverlayScreenContract.State state = stateCache.computeIfAbsent(
			playerRef.getUuid(),
			ignored -> new ScopeOverlayScreenContract.State(null)
		);
		runtime.show(playerRef, ScopeOverlayScreenContract.SCREEN_ID);
		runtime.setState(playerRef, ScopeOverlayScreenContract.SCREEN_ID, state);
	}

	public void setTexture(@Nullable PlayerRef playerRef, @Nullable String overlayTexturePath) {
		if (playerRef == null) {
			return;
		}
		ScopeOverlayScreenContract.State state = new ScopeOverlayScreenContract.State(overlayTexturePath);
		stateCache.put(playerRef.getUuid(), state);
		runtime.setState(playerRef, ScopeOverlayScreenContract.SCREEN_ID, state);
	}
}
