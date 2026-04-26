package com.thescar.hygunsplugin.support.interaction;

import com.thescar.hygunsplugin.debug.DebugLogger;
import com.thescar.hygunsplugin.runtime.components.AmmoDataComponent;
import com.thescar.hygunsplugin.runtime.persistence.RuntimeWeaponMetadataCommit;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class HeldAmmoSyncOnCompletion {
	private static final Field ON_COMPLETION_FIELD = resolveOnCompletionField();
	private static final Set<com.hypixel.hytale.server.core.entity.InteractionChain> SCHEDULED_CHAINS = Collections.newSetFromMap(
		new WeakHashMap<>()
	);

	private HeldAmmoSyncOnCompletion() {
	}

	public static void schedule(@Nonnull InteractionContext interactionContext, @Nonnull Ref<EntityStore> ref,
	                            @Nonnull AmmoDataComponent ammo) {
		if (!ammo.dirty()) {
			return;
		}

		com.hypixel.hytale.server.core.entity.InteractionChain chain = interactionContext.getChain();
		if (chain == null) {
			commit(interactionContext, ref, ammo);
			return;
		}

		synchronized (SCHEDULED_CHAINS) {
			if (!SCHEDULED_CHAINS.add(chain)) {
				return;
			}
		}

		Runnable previous = readOnCompletion(chain);
		chain.setOnCompletion(() -> runWrapped(chain, previous, interactionContext, ref, ammo));
		DebugLogger.debug("AmmoSync", () -> "Scheduled held ammo sync on chain completion: chainId=" + chain.getChainId());
	}

	private static void runWrapped(@Nonnull com.hypixel.hytale.server.core.entity.InteractionChain chain,
	                               @Nullable Runnable previous, @Nonnull InteractionContext interactionContext,
	                               @Nonnull Ref<EntityStore> ref, @Nonnull AmmoDataComponent ammo) {
		Throwable failure = null;
		try {
			if (previous != null) {
				previous.run();
			}
		} catch (Throwable throwable) {
			failure = throwable;
		}

		try {
			commit(interactionContext, ref, ammo);
		} catch (Throwable throwable) {
			if (failure != null) {
				failure.addSuppressed(throwable);
			} else {
				failure = throwable;
			}
		} finally {
			synchronized (SCHEDULED_CHAINS) {
				SCHEDULED_CHAINS.remove(chain);
			}
		}

		if (failure instanceof RuntimeException runtimeException) {
			throw runtimeException;
		}

		if (failure != null) {
			throw new IllegalStateException("Failed to run held ammo chain completion sync", failure);
		}
	}

	private static void commit(@Nonnull InteractionContext interactionContext, @Nonnull Ref<EntityStore> ref,
	                           @Nonnull AmmoDataComponent ammo) {
		if (!ammo.dirty()) {
			return;
		}

		ItemStack heldItem = interactionContext.getHeldItem();
		if (heldItem == null || heldItem.isEmpty()) {
			return;
		}

		RuntimeWeaponMetadataCommit.commitHeldAmmo(interactionContext, ref, heldItem, heldItem, ammo);
		DebugLogger.debug("AmmoSync", () -> "Committed held ammo on chain completion: heldItem=" + heldItem.getItemId());
	}

	private static @Nullable Runnable readOnCompletion(@Nonnull com.hypixel.hytale.server.core.entity.InteractionChain chain) {
		try {
			return (Runnable) ON_COMPLETION_FIELD.get(chain);
		} catch (IllegalAccessException exception) {
			throw new IllegalStateException("Failed to read InteractionChain.onCompletion", exception);
		}
	}

	private static @Nonnull Field resolveOnCompletionField() {
		try {
			Field field = com.hypixel.hytale.server.core.entity.InteractionChain.class.getDeclaredField("onCompletion");
			field.setAccessible(true);
			return field;
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("Failed to resolve InteractionChain.onCompletion", exception);
		}
	}
}
