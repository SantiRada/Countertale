package com.thescar.hygunsplugin.gameplay.projectile.ammo;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public record AmmoImpactInteractionContext(
	@Nonnull Ref<EntityStore> projectileRef, @Nullable Vector3d impactPosition,
	@Nullable Ref<EntityStore> targetRef, @Nullable Ref<EntityStore> shooterRef,
	@Nonnull CommandBuffer<EntityStore> commandBuffer
) {
}
