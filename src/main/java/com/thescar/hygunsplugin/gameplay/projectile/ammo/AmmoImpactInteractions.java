package com.thescar.hygunsplugin.gameplay.projectile.ammo;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.livingentity.LivingEntityEffectSystem;
import com.hypixel.hytale.server.core.modules.entity.teleport.PendingTeleport;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import com.google.gson.JsonObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public final class AmmoImpactInteractions {
	private static final List<AmmoImpactInteractionHandler> HANDLERS = List.of(
		new ApplyEffectHandler(), new ClearEntityEffectHandler(),
		new TeleportToBlockHitHandler()
	);

	private AmmoImpactInteractions() {
	}

	public static void execute(@Nullable JsonObject interaction, @Nonnull AmmoImpactInteractionContext context) {
		if (interaction == null) {
			return;
		}

		String type = readString(interaction, "Type");
		if (type == null) {
			return;
		}

		for (AmmoImpactInteractionHandler handler : HANDLERS) {
			if (handler.supports(type)) {
				handler.execute(interaction, context);
				return;
			}
		}
	}

	@Nullable
	static Ref<EntityStore> resolveEntity(@Nonnull JsonObject interaction, @Nonnull AmmoImpactInteractionContext context) {
		String entity = readString(interaction, "Entity");
		if (entity == null || "Target".equalsIgnoreCase(entity)) {
			return context.targetRef();
		}

		if ("Shooter".equalsIgnoreCase(entity) || "Source".equalsIgnoreCase(entity)) {
			return context.shooterRef();
		}

		if ("User".equalsIgnoreCase(entity) || "Projectile".equalsIgnoreCase(entity)) {
			return context.projectileRef();
		}

		return null;
	}

	@Nullable
	static String readString(@Nonnull JsonObject root, @Nonnull String key) {
		if (!root.has(key) || !root.get(key).isJsonPrimitive()) {
			return null;
		}

		String value = root.get(key).getAsString();
		if (value == null) {
			return null;
		}

		value = value.trim();
		return value.isEmpty()
		       ? null
		       : value;
	}

	static boolean isSolidMaterial(@Nonnull World world, int x, int y, int z) {
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

	private static final class ApplyEffectHandler implements AmmoImpactInteractionHandler {
		@Override
		public boolean supports(@Nonnull String type) {
			return "ApplyEffect".equalsIgnoreCase(type);
		}

		@Override
		public void execute(@Nonnull JsonObject interaction, @Nonnull AmmoImpactInteractionContext context) {
			Ref<EntityStore> entityRef = resolveEntity(interaction, context);
			if (entityRef == null || !entityRef.isValid()) {
				return;
			}

			String effectId = readString(interaction, "EffectId");
			if (effectId == null) {
				return;
			}

			EntityEffect effect = EntityEffect.getAssetMap().getAsset(effectId);
			if (effect == null || !LivingEntityEffectSystem.canApplyEffect(entityRef, effect, context.commandBuffer())) {
				return;
			}

			EffectControllerComponent effectController = context.commandBuffer().getComponent(
				entityRef,
				EffectControllerComponent.getComponentType()
			);
			if (effectController == null) {
				return;
			}

			effectController.addEffect(entityRef, effect, context.commandBuffer());
		}
	}

	private static final class ClearEntityEffectHandler implements AmmoImpactInteractionHandler {
		@Override
		public boolean supports(@Nonnull String type) {
			return "ClearEntityEffect".equalsIgnoreCase(type);
		}

		@Override
		public void execute(@Nonnull JsonObject interaction, @Nonnull AmmoImpactInteractionContext context) {
			Ref<EntityStore> entityRef = resolveEntity(interaction, context);
			if (entityRef == null || !entityRef.isValid()) {
				return;
			}

			String effectId = readString(interaction, "EntityEffectId");
			if (effectId == null) {
				return;
			}

			int effectIndex = EntityEffect.getAssetMap().getIndex(effectId);
			if (effectIndex < 0) {
				return;
			}

			EffectControllerComponent effectController = context.commandBuffer().getComponent(
				entityRef,
				EffectControllerComponent.getComponentType()
			);
			if (effectController == null) {
				return;
			}

			effectController.removeEffect(entityRef, effectIndex, context.commandBuffer());
		}
	}

	private static final class TeleportToBlockHitHandler implements AmmoImpactInteractionHandler {
		@Override
		public boolean supports(@Nonnull String type) {
			return "TeleportToBlockHit".equalsIgnoreCase(type);
		}

		@Override
		public void execute(@Nonnull JsonObject interaction, @Nonnull AmmoImpactInteractionContext context) {
			if (context.targetRef() != null || context.impactPosition() == null || context.shooterRef() == null
				|| !context.shooterRef().isValid()) {
				return;
			}

			TransformComponent shooterTransform = context.commandBuffer().getComponent(
				context.shooterRef(),
				EntityModule.get().getTransformComponentType()
			);
			if (shooterTransform == null) {
				return;
			}

			int blockX = (int) Math.floor(context.impactPosition().x);
			int blockY = (int) Math.floor(context.impactPosition().y);
			int blockZ = (int) Math.floor(context.impactPosition().z);
			World world = (context.commandBuffer().getExternalData() != null)
			              ? context.commandBuffer().getExternalData().getWorld()
			              : null;
			boolean canStandOnTop = world != null && !isSolidMaterial(world, blockX, blockY + 1, blockZ)
				&& !isSolidMaterial(world, blockX, blockY + 2, blockZ);
			var destination = canStandOnTop
			                  ? new com.hypixel.hytale.math.vector.Vector3d(blockX, blockY + 1.0D, blockZ)
			                  : new com.hypixel.hytale.math.vector.Vector3d(blockX, blockY, blockZ);
			var rotation = new com.hypixel.hytale.math.vector.Vector3f(shooterTransform.getRotation());
			Teleport teleport = world != null
			                    ? Teleport.createForPlayer(world, destination, rotation)
			                    : Teleport.createForPlayer(destination, rotation);
			PendingTeleport pendingTeleport = context.commandBuffer().getComponent(
				context.shooterRef(),
				PendingTeleport.getComponentType()
			);
			if (pendingTeleport != null) {
				pendingTeleport.queueTeleport(teleport);
				return;
			}

			context.commandBuffer().putComponent(context.shooterRef(), Teleport.getComponentType(), teleport);
		}
	}
}
