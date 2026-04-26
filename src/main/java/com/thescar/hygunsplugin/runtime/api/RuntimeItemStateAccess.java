package com.thescar.hygunsplugin.runtime.api;

import com.thescar.hygunsplugin.runtime.components.HeatDataComponent;
import com.thescar.hygunsplugin.runtime.item.core.ItemRuntimeEcs;
import com.thescar.hygunsplugin.runtime.item.core.RuntimeItemIdentity;
import com.thescar.hygunsplugin.runtime.item.core.RuntimeItemRef;

import com.hypixel.hytale.server.core.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class RuntimeItemStateAccess {
	private RuntimeItemStateAccess() {
	}

	@Nonnull
	public static TrackedItem ensureTracked(@Nonnull ItemStack itemStack) {
		RuntimeItemIdentity.Assignment assignment = ItemRuntimeEcs.ensureTracked(itemStack);
		return new TrackedItem(assignment.stack(), assignment.ref(), assignment.created());
	}

	@Nullable
	public static RuntimeItemRef resolve(@Nullable ItemStack itemStack) {
		return RuntimeItemIdentity.resolve(itemStack);
	}

	@Nonnull
	public static HeatState ensureHeat(@Nonnull ItemStack itemStack) {
		TrackedItem tracked = ensureTracked(itemStack);
		HeatDataComponent heat = ItemRuntimeEcs.ensureComponent(tracked.ref(), HeatDataComponent.getComponentType());
		return new HeatState(tracked.stack(), tracked.ref(), tracked.created(), heat);
	}

	public record TrackedItem(@Nonnull ItemStack stack, @Nonnull RuntimeItemRef ref, boolean created) {
	}

	public record HeatState(
		@Nonnull ItemStack stack, @Nonnull RuntimeItemRef ref, boolean created, @Nonnull HeatDataComponent heat
	) {
	}
}
