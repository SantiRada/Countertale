package com.thescar.hygunsplugin.gameplay.projectile;

import com.thescar.hygunsplugin.runtime.components.AutoGuidanceDataComponent;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.util.ForceProviderStandardState;
import com.hypixel.hytale.server.core.modules.projectile.ProjectileModule;
import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsProvider;
import com.hypixel.hytale.server.core.modules.projectile.system.StandardPhysicsTickSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

public final class AutoGuidanceSystem extends EntityTickingSystem<EntityStore> {
	private static final float HIT_REMOVE_DELAY_SECONDS = 0.5F;
	private static final double HIT_DISTANCE = 0.25D;
	private static final float MAX_TURN_RATE = 60.0F;

	private static void markAsHit(AutoGuidanceDataComponent autoGuidance, StandardPhysicsProvider physics) {
		autoGuidance.hasHit = true;
		autoGuidance.timeSinceHit = 0.0F;
		physics.setState(StandardPhysicsProvider.STATE.RESTING);
	}

	@Nonnull
	@Override

	public Set<Dependency<EntityStore>> getDependencies() {
		return Set.of(new SystemDependency<>(Order.BEFORE, StandardPhysicsTickSystem.class));
	}

	@Nonnull
	@Override
	public Query<EntityStore> getQuery() {
		var autoGuidanceType = AutoGuidanceDataComponent.getComponentType();
		var projectileType = ProjectileModule.get().getProjectileComponentType();
		if (autoGuidanceType == null || projectileType == null) {
			return Query.any();
		}

		return Query.and(projectileType, autoGuidanceType);
	}

	@Override
	public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store,
	                 @Nonnull CommandBuffer<EntityStore> commandBuffer) {
		Ref<EntityStore> selfRef = archetypeChunk.getReferenceTo(index);
		if (selfRef == null || !selfRef.isValid()) {
			return;
		}

		StandardPhysicsProvider physics = archetypeChunk.getComponent(index, StandardPhysicsProvider.getComponentType());
		var autoGuidanceType = AutoGuidanceDataComponent.getComponentType();
		AutoGuidanceDataComponent autoGuidance = autoGuidanceType == null
		                                         ? null
		                                         : archetypeChunk.getComponent(index, autoGuidanceType);
		TransformComponent transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
		if (physics == null || autoGuidance == null || transform == null) {
			return;
		}

		if (autoGuidance.hasHit) {
			autoGuidance.timeSinceHit += dt;
			if (physics.getState() != StandardPhysicsProvider.STATE.RESTING) {
				physics.setState(StandardPhysicsProvider.STATE.RESTING);
			}

			if (autoGuidance.timeSinceHit >= HIT_REMOVE_DELAY_SECONDS) {
				commandBuffer.removeEntity(selfRef, RemoveReason.REMOVE);
			}

			return;
		}

		if (autoGuidance.lifeTime >= autoGuidance.maxLifeTime) {
			commandBuffer.removeEntity(selfRef, RemoveReason.REMOVE);
			return;
		}

		autoGuidance.lifeTime += dt;
		if (physics.getState() != StandardPhysicsProvider.STATE.ACTIVE || physics.isOnGround() || physics.isBounced()) {
			markAsHit(autoGuidance, physics);
			return;
		}

		Vector3d velocity = physics.getVelocity();
		if (velocity == null) {
			return;
		}

		double speed = velocity.length();
		if (speed < 0.1D) {
			return;
		}

		if (autoGuidance.lifeTime < autoGuidance.autoGuidanceDelay) {
			return;
		}

		if (autoGuidance.targetRef == null || !autoGuidance.targetRef.isValid()) {
			autoGuidance.targetRef = findTarget(
				store, transform.getPosition(), velocity
					.clone()
					.normalize(), autoGuidance, selfRef
			);
		}

		if (autoGuidance.targetRef == null || !autoGuidance.targetRef.isValid()) {
			return;
		}

		TransformComponent targetTransform = store.getComponent(autoGuidance.targetRef, TransformComponent.getComponentType());
		if (targetTransform == null) {
			autoGuidance.targetRef = null;
			return;
		}

		Vector3d from = transform.getPosition();
		Vector3d to = targetTransform.getPosition().clone().add(0.0D, 0.6D, 0.0D);
		double distance = from.distanceTo(to);
		if (distance < HIT_DISTANCE) {
			markAsHit(autoGuidance, physics);
			return;
		}

		Vector3d desiredDirection = to.subtract(from).normalize();
		Vector3d currentDirection = velocity.clone().normalize();
		double alignment = currentDirection.dot(desiredDirection);
		float correctionBoost = (float) Math.max(1.0D, (1.0D - alignment) * 5.0D);
		float turnPerSecond = Math.min(MAX_TURN_RATE, autoGuidance.turnRate * correctionBoost);
		double alpha = Math.min(1.0D, dt * turnPerSecond);
		Vector3d newDirection = Vector3d.lerp(currentDirection, desiredDirection, alpha).normalize();
		ForceProviderStandardState forceState = physics.getForceProviderStandardState();
		if (forceState != null && forceState.nextTickVelocity != null) {
			forceState.nextTickVelocity.assign(newDirection.scale(speed));
		}

		transform.getRotation().setYaw((float) Math.atan2(newDirection.x, newDirection.z));
	}

	@Nullable
	@SuppressWarnings({"rawtypes", "unchecked"})
	private Ref<EntityStore> findTarget(@Nonnull Store<EntityStore> store, @Nonnull Vector3d projectilePosition, @Nonnull Vector3d forward,
	                                    @Nonnull AutoGuidanceDataComponent autoGuidance, @Nonnull Ref<EntityStore> selfRef) {
		SpatialResource spatial = store.getResource(EntityModule.get().getEntitySpatialResourceType());
		if (spatial == null) {
			return null;
		}

		double maxDistanceSq = autoGuidance.detectionRange * autoGuidance.detectionRange;
		double minDot = Math.cos(Math.toRadians(Math.max(0.0F, Math.min(180.0F, autoGuidance.maxLockAngleDegrees))));
		String markedEffectId = autoGuidance.effectId;
		Ref<EntityStore> nearestMarked = null;
		double nearestMarkedDistSq = Double.MAX_VALUE;
		double bestDot = -Double.MAX_VALUE;
		double bestDistSq = Double.MAX_VALUE;
		ObjectArrayList nearby = new ObjectArrayList();
		spatial.getSpatialStructure().collect(projectilePosition, autoGuidance.detectionRange, nearby);
		Ref<EntityStore> best = null;
		for (Object obj : nearby) {
			if (!(obj instanceof Ref<?>)) {
				continue;
			}

			Ref<EntityStore> candidate = (Ref<EntityStore>) obj;
			if (candidate == null || !candidate.isValid() || candidate.equals(selfRef)) {
				continue;
			}

			if (store.getComponent(candidate, ProjectileModule.get().getProjectileComponentType()) != null) {
				continue;
			}

			if (!autoGuidance.affectsPlayers && store.getComponent(candidate, Player.getComponentType()) != null) {
				continue;
			}

			TransformComponent targetTransform = store.getComponent(candidate, TransformComponent.getComponentType());
			if (targetTransform == null) {
				continue;
			}

			Vector3d toTarget = targetTransform
				.getPosition()
				.clone()
				.add(0.0D, 0.6D, 0.0D)
				.subtract(projectilePosition);
			double distSq = (toTarget.x * toTarget.x) + (toTarget.y * toTarget.y) + (toTarget.z * toTarget.z);
			if (distSq <= 0.5D || distSq >= maxDistanceSq) {
				continue;
			}

			if (markedEffectId != null && EntityEffects.hasEffect(store, candidate, markedEffectId) && distSq < nearestMarkedDistSq) {
				nearestMarkedDistSq = distSq;
				nearestMarked = candidate;
			}

			Vector3d toTargetDir = toTarget.normalize();
			double dot = forward.dot(toTargetDir);
			if (dot < minDot) {
				continue;
			}

			if (dot > bestDot + 1.0E-9D || (Math.abs(dot - bestDot) <= 1.0E-9D && distSq < bestDistSq)) {
				bestDot = dot;
				bestDistSq = distSq;
				best = candidate;
			}
		}

		if (nearestMarked != null) {
			return nearestMarked;
		}

		return best;
	}
}
