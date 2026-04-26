package com.thescar.hygunsplugin.gameplay.reload;

import com.thescar.hygunsplugin.runtime.components.WeaponReloadComponent;
import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeEcs;
import com.thescar.hygunsplugin.runtime.item.core.RuntimeItemRef;
import com.thescar.hygunsplugin.support.hytale.PlayerRefAccess;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ReloadManager {
	private static final ConcurrentHashMap<UUID, Set<RuntimeItemRef>> ACTIVE_RELOADS_BY_PLAYER = new ConcurrentHashMap<>();

	private ReloadManager() {
	}

	public static boolean isReloading(@Nullable PlayerRef playerRef) {
		return playerRef != null && hasActiveReload(playerRef.getUuid());
	}

	public static void register(@Nullable UUID playerUuid, @Nullable RuntimeItemRef runtimeRef) {
		if (playerUuid == null || runtimeRef == null) {
			return;
		}

		ACTIVE_RELOADS_BY_PLAYER.computeIfAbsent(playerUuid, ignored -> ConcurrentHashMap.newKeySet()).add(runtimeRef);
	}

	public static void unregister(@Nullable UUID playerUuid, @Nullable RuntimeItemRef runtimeRef) {
		if (playerUuid == null || runtimeRef == null) {
			return;
		}

		Set<RuntimeItemRef> refs = ACTIVE_RELOADS_BY_PLAYER.get(playerUuid);
		if (refs == null) {
			return;
		}

		refs.remove(runtimeRef);
		if (refs.isEmpty()) {
			ACTIVE_RELOADS_BY_PLAYER.remove(playerUuid, refs);
		}
	}

	public static void shutdown() {
		ACTIVE_RELOADS_BY_PLAYER.clear();
	}

	public static void cancel(@Nullable PlayerRef playerRef, @Nullable CancelReason reason) {
		if (playerRef == null) {
			return;
		}

		clearReloads(playerRef.getUuid());
	}

	public static void cancel(@Nullable Player player, @Nullable CancelReason reason) {
		UUID playerUuid = resolvePlayerUuid(player);
		if (playerUuid == null) {
			return;
		}

		clearReloads(playerUuid);
	}

	public static void cancelByPlayerUuid(@Nullable UUID playerUuid) {
		if (playerUuid == null) {
			return;
		}

		clearReloads(playerUuid);
	}

	private static boolean hasActiveReload(@Nonnull UUID playerUuid) {
		Set<RuntimeItemRef> refs = ACTIVE_RELOADS_BY_PLAYER.get(playerUuid);
		return refs != null && !refs.isEmpty();
	}

	private static void clearReloads(@Nullable UUID playerUuid) {
		if (playerUuid == null) {
			return;
		}

		Set<RuntimeItemRef> refs = ACTIVE_RELOADS_BY_PLAYER.remove(playerUuid);
		if (refs == null || refs.isEmpty()) {
			return;
		}

		for (RuntimeItemRef runtimeRef : refs) {
			ItemRuntimeEcs.removeComponent(runtimeRef, WeaponReloadComponent.getComponentType());
		}
	}

	private static @Nullable UUID resolvePlayerUuid(@Nullable Player player) {
		if (player == null) {
			return null;
		}

		var playerRef = player.getReference();
		if (playerRef == null || playerRef.getStore() == null) {
			return null;
		}

		var universePlayerRef = PlayerRefAccess.getValid(playerRef, playerRef.getStore());
		return universePlayerRef != null
		       ? universePlayerRef.getUuid()
		       : null;
	}

	public enum CancelReason {
		SHOOT,
		SWITCHED_ITEM,
		DISCONNECT,
		STARTED_NEW_RELOAD,
		OTHER
	}
}
