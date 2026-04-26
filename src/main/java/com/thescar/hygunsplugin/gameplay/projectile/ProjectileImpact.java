package com.thescar.hygunsplugin.gameplay.projectile;

import com.thescar.hygunsplugin.content.settings.AmmoItemInteractions;
import com.thescar.hygunsplugin.gameplay.projectile.ammo.AmmoImpactInteractionContext;
import com.thescar.hygunsplugin.gameplay.projectile.ammo.AmmoImpactInteractions;
import com.thescar.hygunsplugin.support.text.ValueUtils;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.projectile.config.ImpactConsumer;
import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsProvider;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import com.google.gson.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A simple {@link ImpactConsumer} implementation for Hyguns projectiles.
 *
 * <p>
 * This class is intentionally kept generic and reusable so other gun mods can
 * use it as a drop-in impact handler (e.g. when spawning custom projectiles via
 * Hyguns' helper methods).
 *
 * <p>
 * Behavior:
 * <ul>
 * <li>Applies {@link Damage} to a hit target, attributing the hit to the
 * shooter where possible.</li>
 * <li>Plays an impact sound and spawns an impact particle effect.</li>
 * <li>Removes the projectile entity.</li>
 * </ul>
 */
public final class ProjectileImpact implements ImpactConsumer {
	private static final String DAMAGE_CAUSE_ID = "Projectile";
	private static final String IMPACT_SFX_ID = "SFX_GunPvP_Assault_Rifle_Bullet_Death";
	private static final String IMPACT_PARTICLE_ID = "Gun_Impact";
	// Cache asset lookups; these may be called frequently when many projectiles are
	// active.
	private static volatile DamageCause CACHED_CAUSE;
	private static volatile int CACHED_SFX_INDEX = Integer.MIN_VALUE;

	private final int damage;
	@Nullable
	private final Ref<EntityStore> shooter;
	@Nonnull
	private final HitDamageModifiers hitDamageModifiers;
	@Nullable
	private final AmmoItemInteractions ammoInteractions;
	@Nullable
	private final ImpactConsumer delegateImpactConsumer;

	private final boolean canPenetrateWalls;
	private final double wallPenetrationBlocks;
	private final boolean dealLethalDamage;
	private final double wallPenetrationDamageReductionModifier;
	private final double wallPenetrationDamageReductionDistance;
	private boolean penetratedObstacle;

	public ProjectileImpact(int damage, @Nullable Ref<EntityStore> shooter, @Nonnull HitDamageModifiers hitDamageModifiers) {
		this(damage, shooter, hitDamageModifiers, null, null, false, 1.0D, false, 0.0D, 0.5D);
	}

	public ProjectileImpact(int damage, @Nullable Ref<EntityStore> shooter, @Nonnull HitDamageModifiers hitDamageModifiers,
	                        boolean canPenetrateWalls) {
		this(damage, shooter, hitDamageModifiers, null, null, canPenetrateWalls, 1.0D, false, 0.0D, 0.5D);
	}

	public ProjectileImpact(int damage, @Nullable Ref<EntityStore> shooter, @Nonnull HitDamageModifiers hitDamageModifiers,
	                        boolean canPenetrateWalls, double wallPenetrationBlocks) {
		this(damage, shooter, hitDamageModifiers, null, null, canPenetrateWalls, wallPenetrationBlocks, false, 0.0D, 0.5D);
	}

	public ProjectileImpact(int damage, @Nullable Ref<EntityStore> shooter, @Nonnull HitDamageModifiers hitDamageModifiers,
	                        boolean canPenetrateWalls, double wallPenetrationBlocks, boolean dealLethalDamage) {
		this(damage, shooter, hitDamageModifiers, null, null, canPenetrateWalls, wallPenetrationBlocks, dealLethalDamage, 0.0D, 0.5D);
	}

	public ProjectileImpact(int damage, @Nullable Ref<EntityStore> shooter, @Nonnull HitDamageModifiers hitDamageModifiers,
	                        @Nullable AmmoItemInteractions ammoInteractions, @Nullable ImpactConsumer delegateImpactConsumer, boolean canPenetrateWalls,
	                        double wallPenetrationBlocks, boolean dealLethalDamage, double wallPenetrationDamageReductionModifier,
	                        double wallPenetrationDamageReductionDistance) {
		this.damage = damage;
		this.shooter = shooter;
		this.hitDamageModifiers = (hitDamageModifiers != null)
		                          ? hitDamageModifiers
		                          : HitDamageModifiers.DEFAULT;
		this.ammoInteractions = ammoInteractions;
		this.delegateImpactConsumer = delegateImpactConsumer;
		this.canPenetrateWalls = canPenetrateWalls;
		this.wallPenetrationBlocks = ValueUtils.Checks.positiveOrDefault(wallPenetrationBlocks, 1.0D);
		this.dealLethalDamage = dealLethalDamage;
		this.wallPenetrationDamageReductionModifier = ValueUtils.Checks.nonNegativeOrDefault(wallPenetrationDamageReductionModifier, 0.0D);
		this.wallPenetrationDamageReductionDistance = ValueUtils.Checks.positiveOrDefault(wallPenetrationDamageReductionDistance, 0.5D);
		this.penetratedObstacle = false;
	}

	@Nullable
	private static DamageCause projectileCause() {
		DamageCause c = CACHED_CAUSE;
		if (c != null) {
			return c;
		}
		c = DamageCause.getAssetMap().getAsset(DAMAGE_CAUSE_ID);
		if (c != null) {
			CACHED_CAUSE = c;
		}
		return c;
	}

	private static int impactSfxIndex() {
		int idx = CACHED_SFX_INDEX;
		if (idx != Integer.MIN_VALUE) {
			return idx;
		}
		idx = SoundEvent.getAssetMap().getIndex(IMPACT_SFX_ID);
		CACHED_SFX_INDEX = idx;
		return idx;
	}

	private static double computeSolidThicknessToTarget(@Nonnull Ref<EntityStore> projectileRef, @Nonnull Ref<EntityStore> targetRef,
	                                                    @Nullable Vector3d impactPosition, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
		World world = (commandBuffer.getExternalData() != null)
		              ? commandBuffer.getExternalData().getWorld()
		              : null;
		if (world == null) {
			return -1.0D;
		}
		TransformComponent projectileTransform = commandBuffer.getComponent(
			projectileRef, EntityModule
				.get()
				.getTransformComponentType()
		);
		if (projectileTransform == null) {
			return -1.0D;
		}
		Vector3d start = new Vector3d(projectileTransform.getPosition());
		Vector3d end;
		if (impactPosition != null) {
			end = new Vector3d(impactPosition);
		} else {
			TransformComponent targetTransform = commandBuffer.getComponent(
				targetRef, EntityModule
					.get()
					.getTransformComponentType()
			);
			if (targetTransform == null) {
				return -1.0D;
			}
			end = new Vector3d(targetTransform.getPosition());
		}

		double vx = end.x - start.x;
		double vy = end.y - start.y;
		double vz = end.z - start.z;
		double lenSq = vx * vx + vy * vy + vz * vz;
		if (lenSq <= 1.0E-8D) {
			return 0.0D;
		}
		double len = Math.sqrt(lenSq);
		double invLen = 1.0D / len;
		double dx = vx * invLen;
		double dy = vy * invLen;
		double dz = vz * invLen;
		// Measure continuous solid thickness along the ray.
		final double step = 0.05D; // ~20 samples per full block
		final int maxSamples = Math.max(1, (int) Math.ceil(len / step));
		double solidThickness = 0.0D;
		for (int i = 0; i <= maxSamples; i++) {
			double t = Math.min(len, i * step);
			double px = start.x + dx * t;
			double py = start.y + dy * t;
			double pz = start.z + dz * t;
			int bx = (int) Math.floor(px);
			int by = (int) Math.floor(py);
			int bz = (int) Math.floor(pz);
			if (isSolidMaterial(world, bx, by, bz)) {
				solidThickness += step;
			}
		}

		return solidThickness;
	}

	private static boolean isSolidMaterial(@Nonnull World world, int x, int y, int z) {
		int blockId = world.getBlock(x, y, z);
		if (blockId == 0) {
			return false;
		}

		var blockType = world.getBlockType(x, y, z);
		if (blockType == null) {
			return true;
		}

		return blockType.getMaterial() != BlockMaterial.Empty;
	}

	private static boolean tryPenetrateWall(@Nonnull Ref<EntityStore> projectileRef, @Nullable Vector3d impactPosition,
	                                        @Nonnull CommandBuffer<EntityStore> commandBuffer, double wallPenetrationBlocks) {
		if (!projectileRef.isValid()) {
			return false;
		}
		StandardPhysicsProvider physics = commandBuffer.getComponent(projectileRef, StandardPhysicsProvider.getComponentType());
		if (physics == null) {
			return false;
		}
		Vector3d velocity = getVelocity(physics);
		if (velocity == null) {
			return false;
		}
		double speedSq = (velocity.x * velocity.x) + (velocity.y * velocity.y) + (velocity.z * velocity.z);
		if (speedSq <= 1.0E-10D) {
			return false;
		}
		double invLen = 1.0D / Math.sqrt(speedSq);
		Vector3d direction = new Vector3d(velocity.x * invLen, velocity.y * invLen, velocity.z * invLen);
		TransformComponent transform = commandBuffer.getComponent(
			projectileRef, EntityModule
				.get()
				.getTransformComponentType()
		);
		if (transform == null) {
			return false;
		}
		double wallPenetrationDistance = wallPenetrationBlocks + 0.05D;
		Vector3d origin = impactPosition != null
		                  ? new Vector3d(impactPosition)
		                  : new Vector3d(transform.getPosition());
		Vector3d newPos = new Vector3d(origin).add(
			direction.x * wallPenetrationDistance, direction.y * wallPenetrationDistance,
			direction.z * wallPenetrationDistance
		);
		transform.setPosition(newPos);
		// Continue simulation after impact.
		invokeBooleanSetter(physics, "setImpacted", false);
		invokeBooleanSetter(physics, "setResting", false);
		invokeDoubleSetter(physics, "setMoveOutOfSolid", wallPenetrationDistance);
		return true;
	}

	private static int resolveLethalDamageFromTarget(@Nonnull Ref<EntityStore> targetRef,
	                                                 @Nonnull CommandBuffer<EntityStore> commandBuffer) {
		EntityStatMap statMap = commandBuffer.getComponent(targetRef, EntityStatMap.getComponentType());
		if (statMap == null) {
			return 0;
		}

		EntityStatValue health = statMap.get(EntityStatType.getAssetMap().getIndex("Health"));
		if (health == null) {
			return 0;
		}

		float hp = health.get();
		if (!Float.isFinite(hp)) {
			return 0;
		}

		return Math.max(1, (int) Math.ceil(hp + 1.0F));
	}

	@Nullable
	private static Vector3d getVelocity(@Nonnull StandardPhysicsProvider physics) {
		try {
			Object raw = physics.getClass().getMethod("getVelocity").invoke(physics);
			if (raw instanceof Vector3d velocity) {
				return new Vector3d(velocity);
			}

		} catch (Exception ignored) {
			return null;
		}

		return null;
	}

	private static void invokeBooleanSetter(@Nonnull Object target, @Nonnull String methodName, boolean value) {
		try {
			target.getClass().getMethod(methodName, boolean.class).invoke(target, value);
		} catch (Exception ignored) {
			// Optional API surface; ignore when unavailable.
		}
	}

	private static void invokeDoubleSetter(@Nonnull Object target, @Nonnull String methodName, double value) {
		try {
			target.getClass().getMethod(methodName, double.class).invoke(target, value);
		} catch (Exception ignored) {
			// Optional API surface; ignore when unavailable.
		}
	}

	/**
	 * @return the base damage this impact applies to the target.
	 */
	public int getDamage() {
		return damage;
	}

	/**
	 * @return the shooter reference used to attribute damage, or null if unknown.
	 */
	@Nullable
	public Ref<EntityStore> getShooter() {
		return shooter;
	}

	@Override
	public void onImpact(@Nonnull Ref<EntityStore> projectileRef, @Nullable Vector3d impactPosition, @Nullable Ref<EntityStore> targetRef,
	                     @Nullable String collisionDetail, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
		// Optional 1-block penetration for wall hits only.
		if (targetRef == null && this.canPenetrateWalls && !this.penetratedObstacle
			&& tryPenetrateWall(projectileRef, impactPosition, commandBuffer, this.wallPenetrationBlocks)) {
			this.penetratedObstacle = true;
			return;
		}

		// 1) Damage the target (if any).
		double solidThicknessToTarget = 0.0D;
		if (targetRef != null && targetRef.isValid()) {
			solidThicknessToTarget = computeSolidThicknessToTarget(projectileRef, targetRef, impactPosition, commandBuffer);
			if (solidThicknessToTarget < 0.0D) {
				solidThicknessToTarget = 0.0D;
			}

			double allowedSolidThickness = this.canPenetrateWalls
			                               ? this.wallPenetrationBlocks
			                               : 0.0D;
			if (solidThicknessToTarget > allowedSolidThickness + 1.0E-6D) {
				targetRef = null;
			}
		}

		if (targetRef != null && targetRef.isValid()) {
			Ref<EntityStore> shooterRef = this.shooter;
			Damage.EntitySource source = new Damage.EntitySource((shooterRef != null)
			                                                     ? shooterRef
			                                                     : projectileRef);
			DamageCause cause = projectileCause();
			if (cause != null) {
				int damageToMob = resolveDamageToMob(targetRef, collisionDetail, commandBuffer);
				int reducedDamage = applyWallPenetrationDamageReduction(damageToMob, solidThicknessToTarget);
				if (reducedDamage > 0) {
					Damage dmg = new Damage(source, cause, reducedDamage);
					DamageSystems.executeDamage(targetRef, commandBuffer, dmg);
				}
			}
		}

		runAmmoInteractions(projectileRef, impactPosition, targetRef, commandBuffer);
		if (this.delegateImpactConsumer != null) {
			this.delegateImpactConsumer.onImpact(projectileRef, impactPosition, targetRef, collisionDetail, commandBuffer);
			if (!projectileRef.isValid()) {
				return;
			}
		}

		// 2) Impact SFX + particles + cleanup.
		if (!projectileRef.isValid()) {
			return;
		}

		TransformComponent transform = commandBuffer.getComponent(
			projectileRef, EntityModule
				.get()
				.getTransformComponentType()
		);
		// Prefer the provided impact position; otherwise fall back to transform
		// position.
		Vector3d basePos;
		if (impactPosition != null) {
			basePos = new Vector3d(impactPosition);
		} else if (transform != null) {
			basePos = new Vector3d(transform.getPosition());
		} else {
			basePos = new Vector3d();
		}

		// IMPORTANT: never mutate transform.getPosition() directly.
		Vector3d soundPos = new Vector3d(basePos).add(0.0D, -0.25D, 0.0D);
		int sfxIndex = impactSfxIndex();
		if (sfxIndex >= 0) {
			SoundUtil.playSoundEvent3d(sfxIndex, SoundCategory.SFX, soundPos, commandBuffer);
		}

		ParticleUtil.spawnParticleEffect(IMPACT_PARTICLE_ID, basePos, commandBuffer);
		commandBuffer.removeEntity(projectileRef, RemoveReason.REMOVE);
	}

	private void runAmmoInteractions(@Nonnull Ref<EntityStore> projectileRef, @Nullable Vector3d impactPosition,
	                                 @Nullable Ref<EntityStore> targetRef, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
		AmmoItemInteractions configured = this.ammoInteractions;
		if (configured == null || !configured.hasAnyValue()) {
			return;
		}

		java.util.List<JsonObject> interactions = targetRef != null
		                                          ? configured.entityHit()
		                                          : configured.blockHit();
		AmmoImpactInteractionContext context = new AmmoImpactInteractionContext(
			projectileRef, impactPosition, targetRef, this.shooter,
			commandBuffer
		);
		for (JsonObject interaction : interactions) {
			AmmoImpactInteractions.execute(interaction, context);
		}
	}

	private int applyWallPenetrationDamageReduction(int baseDamage, double solidThicknessToTarget) {
		if (baseDamage <= 0) {
			return 0;
		}
		if (this.dealLethalDamage) {
			return baseDamage;
		}
		if (!this.canPenetrateWalls) {
			return baseDamage;
		}
		if (solidThicknessToTarget <= 0.0D) {
			return baseDamage;
		}
		if (this.wallPenetrationDamageReductionModifier <= 0.0D) {
			return baseDamage;
		}
		if (this.wallPenetrationDamageReductionDistance <= 0.0D) {
			return baseDamage;
		}
		double penalty = this.wallPenetrationDamageReductionModifier
			* (solidThicknessToTarget / this.wallPenetrationDamageReductionDistance);
		double multiplier = Math.max(0.0D, 1.0D - penalty);
		return Math.max(0, (int) Math.round(baseDamage * multiplier));
	}

	private int resolveDamageToMob(@Nonnull Ref<EntityStore> targetRef, @Nullable String collisionDetail,
	                               @Nonnull CommandBuffer<EntityStore> commandBuffer) {
		if (!this.dealLethalDamage) {
			return this.hitDamageModifiers.apply(this.damage, collisionDetail);
		}

		int lethalDamageAmount = resolveLethalDamageFromTarget(targetRef, commandBuffer);
		if (lethalDamageAmount > 0) {
			return lethalDamageAmount;
		}

		return this.hitDamageModifiers.apply(this.damage, collisionDetail);
	}
}
