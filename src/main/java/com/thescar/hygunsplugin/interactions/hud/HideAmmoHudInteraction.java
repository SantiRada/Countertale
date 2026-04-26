package com.thescar.hygunsplugin.interactions.hud;

import com.thescar.hygunsplugin.HygunsPluginMain;
import com.thescar.hygunsplugin.support.hytale.PlayerRefAccess;
import com.thescar.hygunsplugin.ui.hud.HudCoordinator;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class HideAmmoHudInteraction extends SimpleInstantInteraction {
	public static final String KEY = HygunsPluginMain.key("HideHud");
	public static final BuilderCodec<HideAmmoHudInteraction> CODEC = BuilderCodec
		.builder(HideAmmoHudInteraction.class, HideAmmoHudInteraction::new, SimpleInstantInteraction.CODEC).build();

	@Nonnull
	@Override

	public WaitForDataFrom getWaitForDataFrom() {
		return WaitForDataFrom.Server;
	}

	@Override
	protected void firstRun(@Nonnull InteractionType interactionType, @Nonnull InteractionContext interactionContext,
	                        @Nonnull CooldownHandler cooldownHandler) {
		CommandBuffer<EntityStore> cb = interactionContext.getCommandBuffer();
		Ref<EntityStore> ref = interactionContext.getEntity();
		PlayerRef playerRef = PlayerRefAccess.getValid(ref, cb);
		if (playerRef == null) {
			return;
		}
		HudCoordinator.hideAmmo(playerRef);
	}
}
