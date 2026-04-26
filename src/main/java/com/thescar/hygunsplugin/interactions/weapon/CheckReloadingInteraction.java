package com.thescar.hygunsplugin.interactions.weapon;

import com.thescar.hygunsplugin.HygunsPluginMain;
import com.thescar.hygunsplugin.gameplay.reload.ReloadManager;
import com.thescar.hygunsplugin.support.hytale.PlayerRefAccess;
import com.thescar.hygunsplugin.support.interaction.InteractionChain;
import com.thescar.hygunsplugin.support.interaction.InteractionStateSupport;
import com.thescar.hygunsplugin.support.interaction.InteractionValue;

import com.hypixel.hytale.codec.Codec;
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

public class CheckReloadingInteraction extends SimpleInstantInteraction {
	public static final String KEY = HygunsPluginMain.key("CheckReloading");
	public static final BuilderCodec<CheckReloadingInteraction> CODEC = InteractionChain
		.of(
			CheckReloadingInteraction.class, CheckReloadingInteraction::new,
			BuilderCodec.builder(CheckReloadingInteraction.class, CheckReloadingInteraction::new, SimpleInstantInteraction.CODEC)
		)
		.inheritedField("Reloading", Codec.BOOLEAN, interaction -> interaction.reloadingValue)
		.documentation("Checks whether the current player is reloading.")
		.build();

	private final InteractionValue<Boolean> reloadingValue = new InteractionValue<>(true);

	public CheckReloadingInteraction(String id) {
		super(id);
	}

	protected CheckReloadingInteraction() {
	}

	@Nonnull
	@Override
	public WaitForDataFrom getWaitForDataFrom() {
		return WaitForDataFrom.Server;
	}

	@Override
	protected void firstRun(@Nonnull InteractionType interactionType, @Nonnull InteractionContext interactionContext,
	                        @Nonnull CooldownHandler cooldownHandler) {
		CommandBuffer<EntityStore> commandBuffer = interactionContext.getCommandBuffer();
		Ref<EntityStore> ref = interactionContext.getEntity();
		PlayerRef playerRef = PlayerRefAccess.getValid(ref, commandBuffer);
		if (playerRef == null) {
			InteractionStateSupport.fail(interactionContext);
			return;
		}

		boolean expected = Boolean.TRUE.equals(this.reloadingValue.get());
		boolean actual = ReloadManager.isReloading(playerRef);
		if (expected != actual) {
			InteractionStateSupport.fail(interactionContext);
		}

	}
}
