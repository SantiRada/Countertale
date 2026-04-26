package com.thescar.hygunsplugin.gameplay.zoom;

import com.thescar.hygunsplugin.runtime.components.ZoomStateComponent;
import com.thescar.hygunsplugin.support.hytale.PlayerInventoryAccess;
import com.thescar.hygunsplugin.support.hytale.PlayerRefAccess;
import com.thescar.hygunsplugin.ui.hud.HudCoordinator;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

public final class ZoomManager {
	private static final long CAMERA_RESET_CONFIRM_DELAY_MS = 50L;
	private static volatile int SCOPE_OPEN_SOUND_INDEX = Integer.MIN_VALUE;
	private static volatile int SCOPE_CLOSE_SOUND_INDEX = Integer.MIN_VALUE;

	private ZoomManager() {
	}

	private static float clamp(float value, float min, float max) {
		if (value < min) {
			return min;
		}
		if (value > max) {
			return max;
		}
		return value;
	}

	public static boolean toggleZoom(@Nonnull Ref<EntityStore> playerEntityRef, @Nonnull PlayerRef playerRef,
	                                 @Nonnull String scopedItemId, @Nonnull ZoomSettings settings,
	                                 @Nonnull CommandBuffer<EntityStore> commandBuffer) {
		if (commandBuffer.getComponent(playerEntityRef, ZoomStateComponent.getComponentType()) != null) {
			disableZoom(playerEntityRef, playerRef, commandBuffer);
			return false;
		}

		enableZoom(playerEntityRef, playerRef, scopedItemId, settings, commandBuffer);
		return true;
	}

	public static void enableZoom(@Nonnull Ref<EntityStore> playerEntityRef, @Nonnull PlayerRef playerRef, @Nonnull String scopedItemId,
	                              @Nonnull ZoomSettings settings, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
		ZoomStateComponent zoomState = commandBuffer.ensureAndGetComponent(playerEntityRef, ZoomStateComponent.getComponentType());
		zoomState.applySettings(scopedItemId, settings);
		sendCameraPacket(playerRef, zoomState.currentDistance());
		if (zoomState.overlayTexturePath() != null) {
			HudCoordinator.showScope(playerRef, zoomState.overlayTexturePath());
		}
		playScopeOpenSound(playerRef);
	}

	public static void disableZoom(@Nonnull Player player) {
		Ref<EntityStore> playerEntityRef = PlayerInventoryAccess.getReference(player);
		if (playerEntityRef == null) {
			return;
		}

		PlayerRef playerRef = PlayerRefAccess.getValid(playerEntityRef, playerEntityRef.getStore());
		if (playerRef == null) {
			return;
		}

		disableZoom(playerEntityRef, playerRef);
	}

	public static void disableZoom(@Nonnull Ref<EntityStore> playerEntityRef, @Nonnull PlayerRef playerRef) {
		Store<EntityStore> store = playerEntityRef.getStore();
		ZoomStateComponent zoomState = store.getComponent(playerEntityRef, ZoomStateComponent.getComponentType());
		if (zoomState == null) {
			return;
		}

		store.removeComponent(playerEntityRef, ZoomStateComponent.getComponentType());
		clearZoomView(playerRef);
	}

	public static void disableZoom(@Nonnull Ref<EntityStore> playerEntityRef, @Nonnull PlayerRef playerRef,
	                               @Nonnull CommandBuffer<EntityStore> commandBuffer) {
		if (commandBuffer.getComponent(playerEntityRef, ZoomStateComponent.getComponentType()) == null) {
			return;
		}

		commandBuffer.removeComponent(playerEntityRef, ZoomStateComponent.getComponentType());
		clearZoomView(playerRef);
	}

	public static void clearZoomView(@Nonnull PlayerRef playerRef) {
		resetCamera(playerRef);
		HudCoordinator.hideScope(playerRef);
		playScopeCloseSound(playerRef);
	}

	public static boolean isZooming(@Nonnull Ref<EntityStore> playerEntityRef) {
		return playerEntityRef.getStore().getComponent(playerEntityRef, ZoomStateComponent.getComponentType()) != null;
	}

	public static boolean isZooming(@Nonnull Player player) {
		Ref<EntityStore> playerEntityRef = PlayerInventoryAccess.getReference(player);
		return playerEntityRef != null && isZooming(playerEntityRef);
	}

	public static void stepZoom(@Nonnull Ref<EntityStore> playerEntityRef, @Nonnull PlayerRef playerRef) {
		ZoomStateComponent state = playerEntityRef
			.getStore()
			.getComponent(playerEntityRef, ZoomStateComponent.getComponentType());
		if (state == null) {
			return;
		}

		float newZoomMultiplier = state.zoomMultiplier() + state.zoomMultiplierStep();
		if (newZoomMultiplier > state.maxZoomMultiplier()) {
			newZoomMultiplier = state.defaultZoomMultiplier();
		}

		state.setZoomMultiplier(newZoomMultiplier);
		playScopeOpenSound(playerRef);
	}

	public static void zoomIn(@Nonnull Ref<EntityStore> playerEntityRef, @Nonnull PlayerRef playerRef) {
		ZoomStateComponent state = playerEntityRef
			.getStore()
			.getComponent(playerEntityRef, ZoomStateComponent.getComponentType());
		if (state == null) {
			return;
		}

		float newZoomMultiplier = state.zoomMultiplier() + state.zoomMultiplierStep();
		if (newZoomMultiplier > state.maxZoomMultiplier()) {
			newZoomMultiplier = state.maxZoomMultiplier();
		}

		state.setZoomMultiplier(newZoomMultiplier);
		playScopeOpenSound(playerRef);
	}

	public static void zoomOut(@Nonnull Ref<EntityStore> playerEntityRef, @Nonnull PlayerRef playerRef) {
		ZoomStateComponent state = playerEntityRef
			.getStore()
			.getComponent(playerEntityRef, ZoomStateComponent.getComponentType());
		if (state == null) {
			return;
		}

		float newZoomMultiplier = state.zoomMultiplier() - state.zoomMultiplierStep();
		if (newZoomMultiplier < state.defaultZoomMultiplier()) {
			newZoomMultiplier = state.defaultZoomMultiplier();
		}

		state.setZoomMultiplier(newZoomMultiplier);
		playScopeCloseSound(playerRef);
	}

	public static int getMaxZoomStep(@Nonnull ZoomStateComponent state) {
		if (state.zoomMultiplierStep() <= 0.0F) {
			return 0;
		}

		float span = Math.max(0.0F, state.maxZoomMultiplier() - state.defaultZoomMultiplier());
		return Math.max(0, Math.round(span / state.zoomMultiplierStep()));
	}

	public static int getZoomStep(@Nonnull ZoomStateComponent state) {
		if (state.zoomMultiplierStep() <= 0.0F) {
			return 0;
		}

		float offset = Math.max(0.0F, state.zoomMultiplier() - state.defaultZoomMultiplier());
		int step = Math.round(offset / state.zoomMultiplierStep());
		return Math.max(0, Math.min(step, getMaxZoomStep(state)));
	}

	public static void setZoomStep(@Nonnull ZoomStateComponent state, Integer step) {
		int safeStep = step != null
		               ? Math.max(0, step)
		               : 0;
		int clampedStep = Math.min(safeStep, getMaxZoomStep(state));
		float multiplier = state.defaultZoomMultiplier() + (clampedStep * state.zoomMultiplierStep());
		state.setZoomMultiplier(Math.min(multiplier, state.maxZoomMultiplier()));
	}

	public static void updateZoom(@Nonnull ZoomStateComponent state, @Nonnull PlayerRef playerRef,
	                              @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull World world,
	                              @Nonnull Ref<EntityStore> entityRef) {
		float targetDistance = calculateSafeDistance(commandBuffer, world, entityRef, state);
		if (state.currentDistance() == targetDistance) {
			return;
		}

		state.setCurrentDistance(targetDistance);
		sendCameraPacket(playerRef, targetDistance);
	}

	private static void playScopeOpenSound(PlayerRef playerRef) {
		int soundEventIndex = SCOPE_OPEN_SOUND_INDEX;
		if (soundEventIndex == Integer.MIN_VALUE) {
			soundEventIndex = SoundEvent.getAssetMap().getIndex("SFX_Scope_Open");
			SCOPE_OPEN_SOUND_INDEX = soundEventIndex;
		}

		SoundUtil.playSoundEvent2dToPlayer(playerRef, soundEventIndex, SoundCategory.SFX);
	}

	private static void playScopeCloseSound(PlayerRef playerRef) {
		int soundEventIndex = SCOPE_CLOSE_SOUND_INDEX;
		if (soundEventIndex == Integer.MIN_VALUE) {
			soundEventIndex = SoundEvent.getAssetMap().getIndex("SFX_Scope_Close");
			SCOPE_CLOSE_SOUND_INDEX = soundEventIndex;
		}

		SoundUtil.playSoundEvent2dToPlayer(playerRef, soundEventIndex, SoundCategory.SFX);
	}

	private static float calculateSafeDistance(@Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull World world,
	                                           @Nonnull Ref<EntityStore> entityRef, @Nonnull ZoomStateComponent state) {
		Transform transform = TargetUtil.getLook(entityRef, commandBuffer);
		Vector3d position = transform.getPosition();
		Vector3d direction = transform.getDirection();
		float maxDistance = state.currentMaxDistance();
		Vector3i hitBlock = TargetUtil.getTargetBlock(
			world, (blockId, fluidId) -> blockId != 0, position.x, position.y, position.z,
			direction.x, direction.y, direction.z, maxDistance
		);
		if (hitBlock == null) {
			return maxDistance;
		}

		double dx = hitBlock.x + 0.5 - position.x;
		double dy = hitBlock.y + 0.5 - position.y;
		double dz = hitBlock.z + 0.5 - position.z;
		float distanceToBlock = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		float collisionMargin = computeCollisionMargin(maxDistance);
		float safeDistance = distanceToBlock - collisionMargin;
		return clamp(safeDistance, state.minDistance(), maxDistance);
	}

	private static float computeCollisionMargin(float targetDistance) {
		float scaledMargin = ZoomConfig.BASE_COLLISION_MARGIN + (targetDistance * ZoomConfig.COLLISION_SCALE_FACTOR);
		return Math.min(scaledMargin, ZoomConfig.MAX_COLLISION_MARGIN);
	}

	private static void sendCameraPacket(@Nonnull PlayerRef playerRef, float distance) {
		ServerCameraSettings settings = buildSettings(distance);
		playerRef.getPacketHandler().writeNoCache(new SetServerCamera(ClientCameraView.Custom, true, settings));
	}

	private static void resetCamera(@Nonnull PlayerRef playerRef) {
		// Force an immediate snap back to first-person, then clear custom camera state.
		playerRef
			.getPacketHandler()
			.writeNoCache(new SetServerCamera(ClientCameraView.Custom, true, buildInstantFirstPersonSettings()));
		playerRef.getPacketHandler().writeNoCache(new SetServerCamera(ClientCameraView.Custom, true, null));
		HytaleServer.SCHEDULED_EXECUTOR.schedule(
			() -> playerRef.getPacketHandler().writeNoCache(new SetServerCamera(ClientCameraView.Custom, true, null)),
			CAMERA_RESET_CONFIRM_DELAY_MS, TimeUnit.MILLISECONDS
		);
	}

	private static ServerCameraSettings buildSettings(float distance) {
		ServerCameraSettings s = new ServerCameraSettings();
		s.isFirstPerson = false;
		s.distance = -distance;
		s.eyeOffset = true;
		s.positionLerpSpeed = 0.2F;
		s.rotationLerpSpeed = 0.1F;
		s.movementMultiplier = new Vector3f(0.33F, 0.33F, 0.33F);
		s.positionDistanceOffsetType = PositionDistanceOffsetType.DistanceOffset;
		s.sendMouseMotion = true;
		s.displayReticle = true;
		return s;
	}

	private static ServerCameraSettings buildInstantFirstPersonSettings() {
		ServerCameraSettings s = new ServerCameraSettings();
		s.isFirstPerson = true;
		s.distance = 0F;
		s.eyeOffset = false;
		s.positionLerpSpeed = 1F;
		s.rotationLerpSpeed = 1F;
		s.movementMultiplier = new Vector3f(1F, 1F, 1F);
		s.positionDistanceOffsetType = PositionDistanceOffsetType.DistanceOffset;
		s.sendMouseMotion = true;
		s.displayReticle = true;
		return s;
	}

	public record ZoomSettings(
		float maxDistance, float minDistance, float defaultZoomMultiplier, float maxZoomMultiplier,
		float zoomMultiplierStep, String overlayTexturePath
	) {
		public static ZoomSettings of(float maxDistance, float minDistance, float defaultZoomMultiplier, float maxZoomMultiplier,
		                              float zoomMultiplierStep, String overlayTexturePath) {
			float safeMaxDistance = Math.max(0.5F, maxDistance);
			float safeMinDistance = Math.max(0.1F, Math.min(minDistance, safeMaxDistance));
			float safeDefaultMultiplier = Math.max(0.1F, defaultZoomMultiplier);
			float safeMaxMultiplier = Math.max(safeDefaultMultiplier, maxZoomMultiplier);
			float safeMultiplierStep = Math.max(0.01F, zoomMultiplierStep);
			String safeOverlay = (overlayTexturePath == null || overlayTexturePath.isBlank())
			                     ? null
			                     : overlayTexturePath;
			return new ZoomSettings(
				safeMaxDistance, safeMinDistance, safeDefaultMultiplier, safeMaxMultiplier, safeMultiplierStep,
				safeOverlay
			);
		}
	}

	public static final class ZoomConfig {
		public static final float MAX_DISTANCE = 20.0F;
		public static final float MIN_DISTANCE = 1.0F;
		public static final float DEFAULT_ZOOM_MULTIPLIER = 1.0F;
		public static final float MAX_ZOOM_MULTIPLIER = 2.5F;
		public static final float ZOOM_MULTIPLIER_STEP = 0.5F;
		public static final float BASE_COLLISION_MARGIN = 3.0F;
		public static final float COLLISION_SCALE_FACTOR = 0.05F;
		public static final float MAX_COLLISION_MARGIN = 4.0F;
	}
}
