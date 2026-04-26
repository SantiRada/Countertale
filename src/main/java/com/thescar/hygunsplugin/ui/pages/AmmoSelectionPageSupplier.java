package com.thescar.hygunsplugin.ui.pages;

import com.thescar.hygunsplugin.gameplay.ammo.AmmoInventoryAccess;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class AmmoSelectionPageSupplier implements OpenCustomUIInteraction.CustomPageSupplier {
	@Override
	public CustomUIPage tryCreate(Ref<EntityStore> ref, ComponentAccessor<EntityStore> accessor, PlayerRef playerRef,
	                              InteractionContext interactionContext) {
		Player player = accessor.getComponent(ref, Player.getComponentType());
		if (player == null) {
			return null;
		}

		var heldItemContext = interactionContext.createHeldItemContext();
		if (heldItemContext == null) {
			return null;
		}

		var ammoContainer = AmmoInventoryAccess.getAmmoContainer(player);
		if (ammoContainer == null) {
			return null;
		}

		return new AmmoSelectionPage(playerRef, ammoContainer, heldItemContext);
	}
}
