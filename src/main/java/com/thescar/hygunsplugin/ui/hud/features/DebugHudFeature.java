package com.thescar.hygunsplugin.ui.hud.features;

import com.thescar.hygunsplugin.debug.DebugSettings;
import com.thescar.hygunsplugin.gameplay.ammo.AmmoInventoryAccess;
import com.thescar.hygunsplugin.gameplay.reload.ReloadManager;
import com.thescar.hygunsplugin.runtime.components.AmmoDataComponent;
import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeEcs;
import com.thescar.hygunsplugin.runtime.item.core.RuntimeItemIdentity;
import com.thescar.hygunsplugin.runtime.item.core.RuntimeItemRef;
import com.thescar.hygunsplugin.support.hytale.PlayerInventoryAccess;
import com.thescar.hygunsplugin.support.hytale.ItemStackUtils;
import com.thescar.hygunsplugin.ui.hud.core.HudScreenRuntime;
import com.thescar.hygunsplugin.ui.hud.screens.DebugScreenContract;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import org.bson.BsonDocument;
import org.bson.json.JsonWriterSettings;

import javax.annotation.Nullable;

public final class DebugHudFeature {
	private static final JsonWriterSettings JSON_WRITER_SETTINGS = JsonWriterSettings.builder().indent(true).build();

	private final HudScreenRuntime runtime;

	public DebugHudFeature(HudScreenRuntime runtime) {
		this.runtime = runtime;
	}

	private static DebugScreenContract.State buildState(@Nullable PlayerRef playerRef, @Nullable Player player) {
		if (playerRef == null || player == null) {
			return new DebugScreenContract.State("HyGuns Debug\nNo player context", "{}");
		}

		ItemStack held = PlayerInventoryAccess.getItemInHand(player);
		RuntimeItemRef runtimeRef = RuntimeItemIdentity.resolve(held);
		AmmoDataComponent ammo = ItemRuntimeEcs.getComponent(runtimeRef, AmmoDataComponent.getComponentType());
		String heldItemId = held != null && held.getItemId() != null
		                    ? held.getItemId()
		                    : "<none>";
		String runtimeItemId = runtimeRef != null
		                       ? runtimeRef.asString()
		                       : "<none>";
		String selectedAmmo = ammo != null && ammo.initialized()
		                      ? nullToPlaceholder(ammo.selectedAmmoItemId())
		                      : "<none>";
		String loadedAmmo = ammo != null && ammo.initialized()
		                    ? nullToPlaceholder(ammo.loadedAmmoItemId())
		                    : "<none>";
		int ammoValue = ammo != null && ammo.initialized()
		                ? ammo.effectiveAmmo()
		                : -1;
		int maxAmmo = ammo != null && ammo.initialized()
		              ? ammo.maxAmmo()
		              : -1;
		int reserveAmmo = AmmoInventoryAccess.countAvailableAmmo(held, player);
		boolean reloading = ReloadManager.isReloading(playerRef);
		String metadataJson = formatMetadata(held);
		String infoText = """
			playerUUID=%s
			heldItem=%s
			itemRuntimeUUID=%s
			ammo=%s
			reserve=%s
			selectedAmmo=%s
			loadedAmmo=%s
			reloading=%s
			""".formatted(
			playerRef.getUuid(), heldItemId, runtimeItemId, formatAmmo(ammoValue, maxAmmo),
			reserveAmmo >= 0
			? String.valueOf(reserveAmmo)
			: "<n/a>", selectedAmmo, loadedAmmo, reloading
		);
		return new DebugScreenContract.State(infoText, metadataJson);
	}

	private static String formatAmmo(int ammo, int maxAmmo) {
		if (ammo < 0 || maxAmmo < 0) {
			return "<n/a>";
		}

		return ammo + "/" + maxAmmo;
	}

	private static String nullToPlaceholder(@Nullable String value) {
		return value == null || value.isBlank()
		       ? "<none>"
		       : value;
	}

	private static String formatMetadata(@Nullable ItemStack held) {
		if (held == null) {
			return "<none>";
		}

		BsonDocument metadata = ItemStackUtils.getMetadataDocument(held);
		if (metadata == null || metadata.isEmpty()) {
			return "{}";
		}

		return metadata.toJson(JSON_WRITER_SETTINGS);
	}

	public void register() {
		runtime.register(new DebugScreenContract());
	}

	public void sync(@Nullable PlayerRef playerRef, @Nullable Player player) {
		if (playerRef == null) {
			return;
		}

		if (!DebugSettings.isEnabled()) {
			hide(playerRef);
			return;
		}

		DebugScreenContract.State state = buildState(playerRef, player);
		runtime.show(playerRef, DebugScreenContract.SCREEN_ID);
		runtime.setState(playerRef, DebugScreenContract.SCREEN_ID, state);
	}

	public void hide(@Nullable PlayerRef playerRef) {
		if (playerRef == null) {
			return;
		}
		runtime.hide(playerRef, DebugScreenContract.SCREEN_ID);
	}
}
