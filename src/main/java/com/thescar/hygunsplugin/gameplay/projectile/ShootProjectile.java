package com.thescar.hygunsplugin.gameplay.projectile;

import com.thescar.hygunsplugin.HygunsPluginMain;
import com.thescar.hygunsplugin.content.settings.AmmoItemInteractions;
import com.thescar.hygunsplugin.content.settings.AutoGuidanceSettings;
import com.thescar.hygunsplugin.content.settings.WallPenetrationSettings;
import com.thescar.hygunsplugin.runtime.components.AutoGuidanceDataComponent;
import com.thescar.hygunsplugin.support.text.ValueUtils;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.projectile.ProjectileModule;
import com.hypixel.hytale.server.core.modules.projectile.config.ImpactConsumer;
import com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig;
import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsProvider;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Projectile spawning utilities for Hyguns.
 *
 * <p>
 * This class bridges gun interactions to Hytale's projectile/physics systems.
 * It supports two modes:
 * <ul>
 * <li><b>ProjectileConfig</b> (recommended): driven by a ProjectileConfig
 * id.</li>
 * <li><b>Legacy Projectile asset</b>: driven by a Server/Projectiles/*.json id
 * for older content packs.</li>
 * </ul>
 *
 * <p>
 * Compatibility note: some server builds expose projectile impact hooks
 * differently. We use reflection to attach an
 * {@link ImpactConsumer}
 * without hard-failing if the underlying API changes.
 */
public class ShootProjectile {
	// Increase projectile bounding box thickness to prevent entity LOD culling at
	// long range.
	// Legacy LOD cull uses maximumThickness < ratio * distance^2; larger thickness
	// => visible farther.
	private static final double VISIBILITY_BOX_THICKNESS = 2.0D; // ~239 blocks with default ratio

	private static final Set<String> WARNED_CONFIG_IDS = ConcurrentHashMap.newKeySet();
	private static final Set<String> WARNED_HOMING = ConcurrentHashMap.newKeySet();
	private static final Map<String, ProjectileConfig> PROJECTILE_CONFIG_CACHE = new ConcurrentHashMap<>();
	private static final Set<String> PROJECTILE_CONFIG_MISS_CACHE = ConcurrentHashMap.newKeySet();
	private static final Map<String, String> PROJECTILE_ASSET_CACHE = new ConcurrentHashMap<>();
	private static final Set<String> PROJECTILE_ASSET_MISS_CACHE = ConcurrentHashMap.newKeySet();
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	/**
	 * Default projectile config used when a user-provided ID cannot be resolved.
	 * This should point to a valid ProjectileConfig asset key (usually the filename
	 * without .json).
	 */
	private static final String DEFAULT_PROJECTILE_CONFIG_ID = "Hyguns_Projectile_Config_Bullet";
	private static final double PROJECTILE_SPAWN_FORWARD_OFFSET = 0.05D;
	private static final Object IMPACT_LOCK = new Object();
	private static final Set<String> WARNED_IMPACT = ConcurrentHashMap.newKeySet();
	@Nullable
	private static volatile java.lang.reflect.Method LEGACY_PROJECTILE_GET_ASSET;
	@Nullable
	private static volatile Object LEGACY_PROJECTILE_ASSET_MAP;
	private static volatile boolean LEGACY_PROJECTILE_LOOKUP_DONE;
	@Nullable
	private static volatile Field IMPACT_CONSUMER_FIELD;
	@Nullable
	private static volatile java.lang.reflect.Method IMPACT_CONSUMER_SETTER;
	private static volatile boolean IMPACT_FIELD_LOOKUP_DONE;
	private static volatile boolean IMPACT_SETTER_LOOKUP_DONE;

	public static void clearAssetCaches() {
		PROJECTILE_CONFIG_CACHE.clear();
		PROJECTILE_CONFIG_MISS_CACHE.clear();
		PROJECTILE_ASSET_CACHE.clear();
		PROJECTILE_ASSET_MISS_CACHE.clear();
		WARNED_CONFIG_IDS.clear();
		LEGACY_PROJECTILE_ASSET_MAP = null;
		LEGACY_PROJECTILE_GET_ASSET = null;
		LEGACY_PROJECTILE_LOOKUP_DONE = false;
	}

	@Nullable

	private static ProjectileConfig resolveProjectileConfig(@Nullable String rawId) {
		String id = normalizeId(rawId);
		if (id == null) {
			return null;
		}

		ProjectileConfig cached = PROJECTILE_CONFIG_CACHE.get(id);
		if (cached != null) {
			return cached;
		}

		if (PROJECTILE_CONFIG_MISS_CACHE.contains(id)) {
			return null;
		}

		ProjectileConfig resolved = resolveProjectileConfigUncached(id);
		if (resolved != null) {
			PROJECTILE_CONFIG_CACHE.put(id, resolved);
			return resolved;
		}

		PROJECTILE_CONFIG_MISS_CACHE.add(id);
		return null;
	}

	@Nullable
	private static ProjectileConfig resolveProjectileConfigUncached(@Nonnull String id) {
		// 1) Exact match
		ProjectileConfig config = ProjectileConfig.getAssetMap().getAsset(id);
		if (config != null) {
			return config;
		}

		// 2) Strip pack prefix: "pack:key" -> "key"
		int colon = id.indexOf(':');
		if (colon > 0 && colon < id.length() - 1) {
			String candidate = id.substring(colon + 1).trim();
			config = ProjectileConfig.getAssetMap().getAsset(candidate);
			if (config != null) {
				return config;
			}

			id = candidate;
		}

		// 3) Strip directories: "Server/ProjectileConfigs/Foo" -> "Foo"
		int slash = Math.max(id.lastIndexOf('/'), id.lastIndexOf('\\'));
		if (slash >= 0 && slash < id.length() - 1) {
			String candidate = id.substring(slash + 1).trim();
			config = ProjectileConfig.getAssetMap().getAsset(candidate);
			if (config != null) {
				return config;
			}

			id = candidate;
		}

		// 4) Strip .json extension (some tools include it)
		String lower = id.toLowerCase(Locale.ROOT);
		if (lower.endsWith(".json")) {
			String candidate = id.substring(0, id.length() - 5);
			config = ProjectileConfig.getAssetMap().getAsset(candidate);
			return config;
		}

		return null;
	}

	@Nullable
	private static String resolveProjectileAssetId(@Nullable String rawId) {
		String id = normalizeId(rawId);
		if (id == null) {
			return null;
		}

		String cached = PROJECTILE_ASSET_CACHE.get(id);
		if (cached != null) {
			return cached;
		}

		if (PROJECTILE_ASSET_MISS_CACHE.contains(id)) {
			return null;
		}

		String resolved = resolveProjectileAssetIdUncached(id);
		if (resolved != null) {
			PROJECTILE_ASSET_CACHE.put(id, resolved);
			return resolved;
		}

		PROJECTILE_ASSET_MISS_CACHE.add(id);
		return null;
	}

	@Nullable
	private static String resolveProjectileAssetIdUncached(@Nonnull String id) {
		// 1) Exact match
		if (legacyProjectileAssetExists(id)) {
			return id;
		}

		// 2) Strip pack prefix: "pack:key" -> "key"
		int colon = id.indexOf(':');
		if (colon > 0 && colon < id.length() - 1) {
			String candidate = id.substring(colon + 1).trim();
			if (legacyProjectileAssetExists(candidate)) {
				return candidate;
			}

			id = candidate;
		}

		// 3) Strip directories: "Server/Projectiles/Foo" -> "Foo"
		int slash = Math.max(id.lastIndexOf('/'), id.lastIndexOf('\\'));
		if (slash >= 0 && slash < id.length() - 1) {
			String candidate = id.substring(slash + 1).trim();
			if (legacyProjectileAssetExists(candidate)) {
				return candidate;
			}

			id = candidate;
		}

		// 4) Strip .json extension (some tools include it)
		String lower = id.toLowerCase(Locale.ROOT);
		if (lower.endsWith(".json")) {
			String candidate = id.substring(0, id.length() - 5);
			if (legacyProjectileAssetExists(candidate)) {
				return candidate;
			}
		}

		return null;
	}

	private static boolean legacyProjectileAssetExists(@Nonnull String id) {
		java.lang.reflect.Method getAsset = getLegacyProjectileGetAsset();
		if (getAsset == null) {
			return false;
		}

		try {
			Object assetMap = LEGACY_PROJECTILE_ASSET_MAP;
			return assetMap != null && getAsset.invoke(assetMap, id) != null;
		} catch (ReflectiveOperationException | RuntimeException ignored) {
			return false;
		}
	}

	@Nullable
	private static java.lang.reflect.Method getLegacyProjectileGetAsset() {
		if (LEGACY_PROJECTILE_LOOKUP_DONE) {
			return LEGACY_PROJECTILE_GET_ASSET;
		}

		synchronized (ShootProjectile.class) {
			if (LEGACY_PROJECTILE_LOOKUP_DONE) {
				return LEGACY_PROJECTILE_GET_ASSET;
			}

			try {
				Class<?> projectileClass = Class.forName("com.hypixel.hytale.server.core.asset.type.projectile.config.Projectile");
				Object assetMap = projectileClass.getMethod("getAssetMap").invoke(null);
				java.lang.reflect.Method getAsset = assetMap.getClass().getMethod("getAsset", Object.class);
				LEGACY_PROJECTILE_ASSET_MAP = assetMap;
				LEGACY_PROJECTILE_GET_ASSET = getAsset;
			} catch (Throwable ignored) {
				LEGACY_PROJECTILE_ASSET_MAP = null;
				LEGACY_PROJECTILE_GET_ASSET = null;
			} finally {
				LEGACY_PROJECTILE_LOOKUP_DONE = true;
			}

			return LEGACY_PROJECTILE_GET_ASSET;
		}
	}

	@Nullable
	private static String normalizeId(@Nullable String rawId) {
		if (rawId == null) {
			return null;
		}
		String id = rawId.trim();
		return id.isEmpty()
		       ? null
		       : id;
	}
	// Log reflection failures once; otherwise this can spam on every shot.

	/**
	 * Spawns legacy projectiles (Server/Projectiles/*.json) using the same look-ray
	 * + spread logic as {@link #shootBullets}. This is how vanilla
	 * {@code LaunchProjectileInteraction} works.
	 * <p>
	 * Note: when using ProjectileId, damage is taken from the Projectile asset
	 * itself.
	 */
	@Nonnull
	public static List<Ref<EntityStore>> shootProjectiles(int projectileCount, int damage, @Nonnull String projectileId, double spreadAngle,
	                                                      @Nullable AmmoItemInteractions ammoInteractions, @Nonnull HitDamageModifiers hitDamageModifiers, boolean dealLethalDamage,
	                                                      @Nonnull WallPenetrationSettings wallPenetrationSettings, @Nonnull AutoGuidanceSettings autoGuidanceSettings,
	                                                      @Nonnull Ref<EntityStore> shooter, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
		String resolved = resolveProjectileAssetId(projectileId);
		if (resolved == null) {
			LOGGER.atWarning().log("Failed to resolve projectile asset: %s", projectileId);
			return Collections.emptyList();
		}

		UUIDComponent uuidComponent = commandBuffer.getComponent(shooter, UUIDComponent.getComponentType());
		if (uuidComponent == null) {
			LOGGER.atWarning().log("Shooter UUIDComponent missing; cannot launch legacy projectiles");
			return Collections.emptyList();
		}

		TimeResource timeResource = commandBuffer.getResource(TimeResource.getResourceType());
		if (timeResource == null) {
			LOGGER.atWarning().log("TimeResource missing; cannot launch legacy projectiles");
			return Collections.emptyList();
		}

		Transform look = TargetUtil.getLook(shooter, commandBuffer);
		Vector3d rawPosition = look.getPosition();
		Vector3d rawForward = look.getDirection();
		double fx = rawForward.x;
		double fy = rawForward.y;
		double fz = rawForward.z;
		double fl = Math.sqrt(fx * fx + fy * fy + fz * fz);
		if (fl <= 1.0E-12D) {
			return Collections.emptyList();
		}

		double finv = 1.0D / fl;
		fx *= finv;
		fy *= finv;
		fz *= finv;
		double px = rawPosition.x + fx * PROJECTILE_SPAWN_FORWARD_OFFSET;
		double py = rawPosition.y + fy * PROJECTILE_SPAWN_FORWARD_OFFSET;
		double pz = rawPosition.z + fz * PROJECTILE_SPAWN_FORWARD_OFFSET;
		double rx = -fz;
		double ry = 0.0D;
		double rz = fx;
		double rl2 = rx * rx + ry * ry + rz * rz;
		if (rl2 < 1.0E-12D) {
			rx = 1.0D;
			ry = 0.0D;
			rz = 0.0D;
		} else {
			double rinv = 1.0D / Math.sqrt(rl2);
			rx *= rinv;
			ry *= rinv;
			rz *= rinv;
		}

		double ux = ry * fz - rz * fy;
		double uy = rz * fx - rx * fz;
		double uz = rx * fy - ry * fx;
		double ul = Math.sqrt(ux * ux + uy * uy + uz * uz);
		if (ul > 1.0E-12D) {
			double uinv = 1.0D / ul;
			ux *= uinv;
			uy *= uinv;
			uz *= uinv;
		}

		ThreadLocalRandom rng = ThreadLocalRandom.current();
		List<Ref<EntityStore>> projectiles = new ArrayList<>(Math.max(1, projectileCount));
		UUID creatorUuid = uuidComponent.getUuid();
		for (int i = 0; i < projectileCount; i++) {
			double sx = (rng.nextDouble() - 0.5D) * 2.0D * spreadAngle;
			double sy = (rng.nextDouble() - 0.5D) * 2.0D * spreadAngle;
			double dx = fx + rx * sx + ux * sy;
			double dy = fy + ry * sx + uy * sy;
			double dz = fz + rz * sx + uz * sy;
			double dl = Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (dl <= 1.0E-12D) {
				continue;
			}

			double dinv = 1.0D / dl;
			dx *= dinv;
			dy *= dinv;
			dz *= dinv;
			// Hytale uses radians for yaw/pitch (see PhysicsMath.vectorFromAngles).
			float yaw = (float) Math.atan2(-dx, -dz);
			float pitch = (float) Math.asin(dy);
			Vector3f rotation = new Vector3f();
			rotation.setYaw(yaw);
			rotation.setPitch(pitch);
			rotation.setRoll(0.0F);
			Holder<EntityStore> holder = ProjectileComponent.assembleDefaultProjectile(
				timeResource, resolved, new Vector3d(px, py, pz),
				rotation
			);
			ProjectileComponent projectileComponent = holder.getComponent(ProjectileComponent.getComponentType());
			if (projectileComponent == null) {
				LOGGER.atWarning().log("Assembled projectile is missing ProjectileComponent: %s", resolved);
				continue;
			}

			holder.ensureComponent(Intangible.getComponentType());
			if (projectileComponent.getProjectile() == null) {
				projectileComponent.initialize();
				if (projectileComponent.getProjectile() == null) {
					LOGGER.atWarning().log("Projectile asset failed to initialize: %s", resolved);
					continue;
				}
			}

			projectileComponent.shoot(holder, creatorUuid, px, py, pz, yaw, pitch);
			Ref<EntityStore> projectileRef = commandBuffer.addEntity(holder, AddReason.SPAWN);
			projectiles.add(projectileRef);
		}

		applyCustomProjectileBehavior(
			commandBuffer, projectiles, damage, shooter, ammoInteractions, hitDamageModifiers, dealLethalDamage,
			wallPenetrationSettings, autoGuidanceSettings
		);
		applyVisibilityBoundingBox(commandBuffer, projectiles);
		return projectiles;
	}

	private static void applyVisibilityBoundingBox(@Nonnull CommandBuffer<EntityStore> commandBuffer,
	                                               @Nonnull List<Ref<EntityStore>> refs) {
		if (refs.isEmpty()) {
			return;
		}
		final double half = VISIBILITY_BOX_THICKNESS * 0.5D;
		final Box box = new Box(-half, -half, -half, half, half, half);
		commandBuffer.run(store -> {
			for (Ref<EntityStore> ref : refs) {
				if (ref == null || !ref.isValid()) {
					continue;
				}
				BoundingBox bb = store.getComponent(ref, BoundingBox.getComponentType());
				if (bb != null) {
					bb.setBoundingBox(box);
				} else {
					store.addComponent(ref, BoundingBox.getComponentType(), new BoundingBox(box));
				}
			}

		});
	}

	@Nullable
	private static java.lang.reflect.Method getImpactConsumerSetter() {
		if (IMPACT_SETTER_LOOKUP_DONE) {
			return IMPACT_CONSUMER_SETTER;
		}

		synchronized (IMPACT_LOCK) {
			if (IMPACT_SETTER_LOOKUP_DONE) {
				return IMPACT_CONSUMER_SETTER;
			}

			try {
				// Newer builds may expose a setter method instead of a writable field.
				for (String name : new String[]{"setImpactConsumer", "impactConsumer"}) {
					try {
						java.lang.reflect.Method m = StandardPhysicsProvider.class.getMethod(name, ImpactConsumer.class);
						m.setAccessible(true);
						IMPACT_CONSUMER_SETTER = m;
						break;
					} catch (NoSuchMethodException ignored) {
						// try next
					}
				}

				if (IMPACT_CONSUMER_SETTER == null) {
					for (String name : new String[]{"setImpactConsumer", "impactConsumer"}) {
						try {
							java.lang.reflect.Method m = StandardPhysicsProvider.class.getDeclaredMethod(name, ImpactConsumer.class);
							m.setAccessible(true);
							IMPACT_CONSUMER_SETTER = m;
							break;
						} catch (NoSuchMethodException ignored) {
							// try next
						}
					}
				}

			} catch (Exception t) {
				if (WARNED_IMPACT.add("setter")) {
					LOGGER.atWarning().log("Failed to find a StandardPhysicsProvider ImpactConsumer setter: %s", t);
				}

				IMPACT_CONSUMER_SETTER = null;
			} finally {
				IMPACT_SETTER_LOOKUP_DONE = true;
			}

			return IMPACT_CONSUMER_SETTER;
		}
	}

	@Nonnull
	public static List<Ref<EntityStore>> shootBullets(int pelletCount, int damage, double spreadAngle, String projectileConfigID,
	                                                  @Nullable AmmoItemInteractions ammoInteractions, @Nonnull HitDamageModifiers hitDamageModifiers, boolean dealLethalDamage,
	                                                  @Nonnull WallPenetrationSettings wallPenetrationSettings, @Nonnull AutoGuidanceSettings autoGuidanceSettings,
	                                                  Ref<EntityStore> shooter, CommandBuffer<EntityStore> commandBuffer) {
		ProjectileConfig config = resolveProjectileConfig(projectileConfigID);
		if (config == null) {
			if (WARNED_CONFIG_IDS.add(String.valueOf(projectileConfigID))) {
				LOGGER
					.atWarning()
					.log("Failed to resolve projectile config: %s (falling back to default)", projectileConfigID);
			}

			config = resolveProjectileConfig(DEFAULT_PROJECTILE_CONFIG_ID);
		}

		if (config == null) {
			if (WARNED_CONFIG_IDS.add(DEFAULT_PROJECTILE_CONFIG_ID)) {
				LOGGER.atWarning().log("Default projectile config missing too: %s", DEFAULT_PROJECTILE_CONFIG_ID);
			}

			return Collections.emptyList();
		}

		Transform look = TargetUtil.getLook(shooter, commandBuffer);
		Vector3d rawPosition = look.getPosition();
		Vector3d rawForward = look.getDirection();
		Vector3d forward = normalizeDirection(rawForward);
		if (forward == null) {
			return Collections.emptyList();
		}
		Vector3d origin = new Vector3d(rawPosition).add(
			forward.x * PROJECTILE_SPAWN_FORWARD_OFFSET,
			forward.y * PROJECTILE_SPAWN_FORWARD_OFFSET,
			forward.z * PROJECTILE_SPAWN_FORWARD_OFFSET
		);
		return spawnBulletsFromAim(
			pelletCount,
			damage,
			spreadAngle,
			config,
			ammoInteractions,
			hitDamageModifiers,
			dealLethalDamage,
			wallPenetrationSettings,
			autoGuidanceSettings,
			shooter,
			commandBuffer,
			origin,
			forward
		);
	}

	@Nonnull
	public static List<Ref<EntityStore>> shootBulletsFrom(int pelletCount, int damage, double spreadAngle, String projectileConfigID,
	                                                       @Nullable AmmoItemInteractions ammoInteractions, @Nonnull HitDamageModifiers hitDamageModifiers, boolean dealLethalDamage,
	                                                       @Nonnull WallPenetrationSettings wallPenetrationSettings, @Nonnull AutoGuidanceSettings autoGuidanceSettings,
	                                                       @Nonnull Ref<EntityStore> shooter, @Nonnull CommandBuffer<EntityStore> commandBuffer,
	                                                       @Nonnull Vector3d origin, @Nonnull Vector3d direction) {
		ProjectileConfig config = resolveProjectileConfig(projectileConfigID);
		if (config == null) {
			if (WARNED_CONFIG_IDS.add(String.valueOf(projectileConfigID))) {
				LOGGER
					.atWarning()
					.log("Failed to resolve projectile config: %s (falling back to default)", projectileConfigID);
			}

			config = resolveProjectileConfig(DEFAULT_PROJECTILE_CONFIG_ID);
		}

		if (config == null) {
			if (WARNED_CONFIG_IDS.add(DEFAULT_PROJECTILE_CONFIG_ID)) {
				LOGGER.atWarning().log("Default projectile config missing too: %s", DEFAULT_PROJECTILE_CONFIG_ID);
			}

			return Collections.emptyList();
		}

		Vector3d normalizedDirection = normalizeDirection(direction);
		if (normalizedDirection == null) {
			return Collections.emptyList();
		}

		return spawnBulletsFromAim(
			pelletCount,
			damage,
			spreadAngle,
			config,
			ammoInteractions,
			hitDamageModifiers,
			dealLethalDamage,
			wallPenetrationSettings,
			autoGuidanceSettings,
			shooter,
			commandBuffer,
			new Vector3d(origin),
			normalizedDirection
		);
	}

	@Nonnull
	private static List<Ref<EntityStore>> spawnBulletsFromAim(int pelletCount, int damage, double spreadAngle,
	                                                          @Nonnull ProjectileConfig config,
	                                                          @Nullable AmmoItemInteractions ammoInteractions,
	                                                          @Nonnull HitDamageModifiers hitDamageModifiers,
	                                                          boolean dealLethalDamage,
	                                                          @Nonnull WallPenetrationSettings wallPenetrationSettings,
	                                                          @Nonnull AutoGuidanceSettings autoGuidanceSettings,
	                                                          @Nonnull Ref<EntityStore> shooter,
	                                                          @Nonnull CommandBuffer<EntityStore> commandBuffer,
	                                                          @Nonnull Vector3d origin,
	                                                          @Nonnull Vector3d forward) {
		double fx = forward.x;
		double fy = forward.y;
		double fz = forward.z;
		double px = origin.x;
		double py = origin.y;
		double pz = origin.z;
		double rx = -fz;
		double ry = 0.0D;
		double rz = fx;
		double rl2 = rx * rx + ry * ry + rz * rz;
		if (rl2 < 1.0E-12D) {
			rx = 1.0D;
			ry = 0.0D;
			rz = 0.0D;
		} else {
			double rinv = 1.0D / Math.sqrt(rl2);
			rx *= rinv;
			ry *= rinv;
			rz *= rinv;
		}

		double ux = ry * fz - rz * fy;
		double uy = rz * fx - rx * fz;
		double uz = rx * fy - ry * fx;
		double ul = Math.sqrt(ux * ux + uy * uy + uz * uz);
		if (ul > 1.0E-12D) {
			double uinv = 1.0D / ul;
			ux *= uinv;
			uy *= uinv;
			uz *= uinv;
		}

		UUIDComponent shooterUuidComponent = commandBuffer.getComponent(shooter, UUIDComponent.getComponentType());
		UUID shooterUuid = shooterUuidComponent != null ? shooterUuidComponent.getUuid() : null;

		ThreadLocalRandom rng = ThreadLocalRandom.current();
		List<Ref<EntityStore>> projectiles = new ArrayList<>(Math.max(1, pelletCount));
		for (int i = 0; i < pelletCount; i++) {
			double sx = (rng.nextDouble() - 0.5D) * 2.0D * spreadAngle;
			double sy = (rng.nextDouble() - 0.5D) * 2.0D * spreadAngle;
			double dx = fx + rx * sx + ux * sy;
			double dy = fy + ry * sx + uy * sy;
			double dz = fz + rz * sx + uz * sy;
			double dl = Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (dl <= 1.0E-12D) {
				continue;
			}

			double dinv = 1.0D / dl;
			dx *= dinv;
			dy *= dinv;
			dz *= dinv;
			Ref<EntityStore> projectileRef = ProjectileModule.get().spawnProjectile(
				shooter, commandBuffer, config,
				new Vector3d(px, py, pz), new Vector3d(dx, dy, dz)
			);
			if (projectileRef == null) {
				LOGGER.atWarning().log("spawnProjectile returned null");
				continue;
			}

			projectiles.add(projectileRef);
		}

		if (projectiles.isEmpty()) {
			return Collections.emptyList();
		}

		applyCustomProjectileBehavior(
			commandBuffer, projectiles, damage, shooter, ammoInteractions, hitDamageModifiers, dealLethalDamage,
			wallPenetrationSettings, autoGuidanceSettings
		);
		applyVisibilityBoundingBox(commandBuffer, projectiles);
		return projectiles;
	}

	@Nullable
	private static Vector3d normalizeDirection(@Nullable Vector3d direction) {
		if (direction == null) {
			return null;
		}

		double length = Math.sqrt((direction.x * direction.x) + (direction.y * direction.y) + (direction.z * direction.z));
		if (length <= 1.0E-12D) {
			return null;
		}

		double inv = 1.0D / length;
		return new Vector3d(direction.x * inv, direction.y * inv, direction.z * inv);
	}

	private static void applyCustomProjectileBehavior(@Nonnull CommandBuffer<EntityStore> commandBuffer,
	                                                  @Nonnull List<Ref<EntityStore>> refs, int damage, @Nonnull Ref<EntityStore> shooter,
	                                                  @Nullable AmmoItemInteractions ammoInteractions, @Nonnull HitDamageModifiers hitDamageModifiers, boolean dealLethalDamage,
	                                                  @Nonnull WallPenetrationSettings wallPenetrationSettings, @Nonnull AutoGuidanceSettings autoGuidanceSettings) {
		if (refs.isEmpty()) {
			return;
		}

		commandBuffer.run(store -> {
			for (Ref<EntityStore> ref : refs) {
				if (ref == null || !ref.isValid()) {
					continue;
				}

				StandardPhysicsProvider physics = store.getComponent(ref, StandardPhysicsProvider.getComponentType());
				if (physics != null) {
					ImpactConsumer existingImpactConsumer = physics.getImpactConsumer();
					setCustomImpactConsumer(
						physics,
						new ProjectileImpact(
							damage, shooter, hitDamageModifiers, ammoInteractions, existingImpactConsumer,
							wallPenetrationSettings.canPenetrateWalls(), wallPenetrationSettings.wallPenetrationBlocks(), dealLethalDamage,
							wallPenetrationSettings.damageReductionModifier(), wallPenetrationSettings.damageReductionDistance()
						)
					);
				} else {
					if (WARNED_IMPACT.add("missingPhysicsProvider")) {
						LOGGER
							.atWarning()
							.log("Projectile has no StandardPhysicsProvider; ImpactConsumer customization is skipped");
					}
				}
			}

		});
		if (!autoGuidanceSettings.enabled()) {
			return;
		}

		HygunsPluginMain plugin = HygunsPluginMain.instance();
		if (plugin == null) {
			if (WARNED_HOMING.add("pluginMissing")) {
				LOGGER.atWarning().log("AmmoAutoGuidance requested, but HygunsPluginMain.instance() is null");
			}

			return;
		}

		ComponentType<EntityStore, AutoGuidanceDataComponent> autoGuidanceType = AutoGuidanceDataComponent.getComponentType();
		if (autoGuidanceType == null) {
			if (WARNED_HOMING.add("componentMissing")) {
				LOGGER
					.atWarning()
					.log("AmmoAutoGuidance requested, but AutoGuidanceDataComponent type is not registered");
			}

			return;
		}

		float lockCone = (float) Math.max(1.0D, Math.min(180.0D, autoGuidanceSettings.coneDegrees()));
		float range = (float) Math.max(1.0D, autoGuidanceSettings.maxDistance());
		float turnRate = (float) Math.max(1.0D, autoGuidanceSettings.turnRate());
		boolean affectsPlayers = autoGuidanceSettings.affectsPlayers();
		String effectId = ValueUtils.Checks.nonBlankOrNull(autoGuidanceSettings.effectId());
		commandBuffer.run(store -> {
			for (Ref<EntityStore> ref : refs) {
				if (ref == null || !ref.isValid()) {
					continue;
				}

				store.putComponent(
					ref, autoGuidanceType,
					new AutoGuidanceDataComponent(turnRate, range, 0.0F, 10.0F, lockCone, affectsPlayers, effectId)
				);
			}

		});
	}

	@Nullable
	private static Field getImpactConsumerField() {
		if (IMPACT_FIELD_LOOKUP_DONE) {
			return IMPACT_CONSUMER_FIELD;
		}

		synchronized (IMPACT_LOCK) {
			if (IMPACT_FIELD_LOOKUP_DONE) {
				return IMPACT_CONSUMER_FIELD;
			}

			try {
				Field f = StandardPhysicsProvider.class.getDeclaredField("impactConsumer");
				f.setAccessible(true);
				IMPACT_CONSUMER_FIELD = f;
			} catch (Exception t) {
				if (WARNED_IMPACT.add("field")) {
					LOGGER
						.atWarning()
						.log("Failed to find StandardPhysicsProvider.impactConsumer field via reflection: %s", t);
				}

				IMPACT_CONSUMER_FIELD = null;
			} finally {
				IMPACT_FIELD_LOOKUP_DONE = true;
			}

			return IMPACT_CONSUMER_FIELD;
		}
	}

	private static void setCustomImpactConsumer(@Nonnull StandardPhysicsProvider physics, @Nonnull ImpactConsumer consumer) {
		java.lang.reflect.Method setter = getImpactConsumerSetter();
		if (setter != null) {
			try {
				setter.invoke(physics, consumer);
				return;
			} catch (Exception t) {
				// Fall back to the field path if invocation fails for any reason.
				if (WARNED_IMPACT.add("invokeSetter")) {
					LOGGER
						.atWarning()
						.log("Failed to invoke ImpactConsumer setter via reflection; falling back to field: %s", t);
				}
			}
		}

		Field f = getImpactConsumerField();
		if (f == null) {
			return;
		}

		try {
			f.set(physics, consumer);
		} catch (Exception t) {
			if (WARNED_IMPACT.add("invokeField")) {
				LOGGER.atWarning().log("Failed to set ImpactConsumer via reflection: %s", t);
			}
		}
	}
}
