package com.thescar.hygunsplugin.support.interaction;

import com.thescar.hygunsplugin.debug.DebugLogger;

import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.entity.InteractionContext;

import javax.annotation.Nonnull;

public final class InteractionStateSupport {
	private InteractionStateSupport() {
	}

	public static void fail(@Nonnull InteractionContext interactionContext) {
		DebugLogger.debug(
			"InteractionState", () -> "Failed interaction for heldItem="
				+ (interactionContext.getHeldItem() != null
				   ? interactionContext.getHeldItem().getItemId()
				   : "null")
		);
		interactionContext.getState().state = InteractionState.Failed;
		if (interactionContext.getClientState() != null) {
			interactionContext.getClientState().state = InteractionState.Failed;
		}
	}

	public static void finish(@Nonnull InteractionContext interactionContext) {
		DebugLogger.debug(
			"InteractionState", () -> "Finished interaction for heldItem="
				+ (interactionContext.getHeldItem() != null
				   ? interactionContext.getHeldItem().getItemId()
				   : "null")
		);
		interactionContext.getState().state = InteractionState.Finished;
	}
}
