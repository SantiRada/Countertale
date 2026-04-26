package com.thescar.hygunsplugin.interactions.hud;

import com.thescar.hygunsplugin.HygunsPluginMain;
import com.thescar.hygunsplugin.support.hytale.PlayerInventoryAccess;
import com.thescar.hygunsplugin.support.hytale.PlayerRefAccess;
import com.thescar.hygunsplugin.ui.hud.HudCoordinator;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class UpdateAmmoHudInteraction extends SimpleInstantInteraction {
	public static final String KEY = HygunsPluginMain.key("UpdateHud");
	public static final BuilderCodec<UpdateAmmoHudInteraction> CODEC = BuilderCodec
		.builder(UpdateAmmoHudInteraction.class, UpdateAmmoHudInteraction::new, SimpleInstantInteraction.CODEC).build();

	@Nonnull
	@Override

	public WaitForDataFrom getWaitForDataFrom() {
		return WaitForDataFrom.Server;
	}

	@Override
	protected void firstRun(@Nonnull InteractionType interactionType, @Nonnull InteractionContext interactionContext,
	                        @Nonnull CooldownHandler cooldownHandler) {
		CommandBuffer<EntityStore> cb = interactionContext.getCommandBuffer();
		Ref<EntityStore> entityRef = interactionContext.getEntity();
		Player player = cb.getComponent(entityRef, Player.getComponentType());
		if (player == null) {
			return;
		}
		PlayerRef playerRef = PlayerRefAccess.getValid(entityRef, cb);
		if (playerRef == null) {
			return;
		}
		ItemStack currentHeld = PlayerInventoryAccess.getItemInHand(player);
		if (currentHeld == null) {
			HudCoordinator.hideAmmo(playerRef);
			return;
		}

		HudCoordinator.updateAmmo(playerRef, currentHeld);
	}
}
