package com.thescar.hygunsplugin.content.particles;

import com.thescar.hygunsplugin.content.registry.ItemAssetScanner;
import com.thescar.hygunsplugin.debug.DebugLogger;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.Interaction;
import com.hypixel.hytale.protocol.InteractionEffects;
import com.hypixel.hytale.protocol.ModelParticle;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.ParticleAnimationFrame;
import com.hypixel.hytale.protocol.ParticleSpawnerGroup;
import com.hypixel.hytale.protocol.UpdateType;
import com.hypixel.hytale.protocol.packets.assets.UpdateInteractions;
import com.hypixel.hytale.protocol.packets.assets.UpdateParticleSpawners;
import com.hypixel.hytale.protocol.packets.assets.UpdateParticleSystems;
import com.hypixel.hytale.protocol.packets.world.SpawnParticleSystem;
import com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSpawner;
import com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSystem;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class ParticleColorVariantService {
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	private static final String VARIANT_MARKER = "__hyguns_color__";
	private static final Map<String, Variant> VARIANT_CACHE = new ConcurrentHashMap<>();
	private static final Map<Integer, Set<String>> SENT_VARIANTS_BY_HANDLER = new ConcurrentHashMap<>();
	private static final Map<Integer, PacketHandler> KNOWN_HANDLERS = new ConcurrentHashMap<>();
	private static final Map<UUID, PlayerRef> KNOWN_PLAYERS = new ConcurrentHashMap<>();
	private static final Set<String> LOGGED_SKIPS = ConcurrentHashMap.newKeySet();
	private static volatile boolean registered;

	private ParticleColorVariantService() {
	}

	public static void register() {
		if (registered) {
			return;
		}
		synchronized (ParticleColorVariantService.class) {
			if (registered) {
				return;
			}
			PacketAdapters.registerOutbound(ParticleColorVariantService::interceptOutbound);
			registered = true;
			precomputeInteractionVariants();
			LOGGER.atInfo().log("Particle color variants enabled");
		}
	}

	public static void rememberPlayer(@Nullable PlayerRef playerRef) {
		if (playerRef == null) {
			return;
		}
		KNOWN_PLAYERS.put(playerRef.getUuid(), playerRef);
		rememberHandler(playerRef.getPacketHandler());
	}

	public static void forgetPlayer(@Nullable UUID playerUuid) {
		if (playerUuid == null) {
			return;
		}
		KNOWN_PLAYERS.remove(playerUuid);
	}

	public static String debugState() {
		long validPlayers = KNOWN_PLAYERS
			.values()
			.stream()
			.filter(playerRef -> playerRef != null && playerRef.isValid())
			.count();
		long activeHandlers = KNOWN_HANDLERS
			.values()
			.stream()
			.filter(handler -> handler != null && handler.stillActive())
			.count();
		return "players=" + KNOWN_PLAYERS.size()
			+ "/" + validPlayers
			+ ", handlers=" + KNOWN_HANDLERS.size()
			+ "/" + activeHandlers
			+ ", variants=" + VARIANT_CACHE.size();
	}

	public static void clearCaches() {
		VARIANT_CACHE.clear();
		SENT_VARIANTS_BY_HANDLER.clear();
		LOGGED_SKIPS.clear();
		precomputeInteractionVariants();
	}

	private static boolean interceptOutbound(PacketHandler handler, Packet packet) {
		rememberHandler(handler);
		if (packet instanceof UpdateParticleSpawners updateParticleSpawners) {
			precomputeInteractionVariants();
			augmentParticleSpawners(updateParticleSpawners);
			return false;
		}
		if (packet instanceof UpdateParticleSystems updateParticleSystems) {
			precomputeInteractionVariants();
			augmentParticleSystems(updateParticleSystems);
			return false;
		}
		if (packet instanceof UpdateInteractions updateInteractions) {
			rewriteInteractionParticles(updateInteractions);
			return false;
		}
		if (!(packet instanceof SpawnParticleSystem spawn)) {
			return false;
		}
		if (spawn.particleSystemId == null || spawn.particleSystemId.contains(VARIANT_MARKER)) {
			return false;
		}
		if (spawn.color == null) {
			debugSkip(spawn.particleSystemId, "spawn has no external color");
			return false;
		}
		Variant variant = resolveVariant(spawn.particleSystemId, spawn.color);
		if (variant == null) {
			return false;
		}
		sendVariantIfNeeded(handler, variant);
		spawn.particleSystemId = variant.systemId();
		return false;
	}

	public static int resendToKnownHandlers() {
		precomputeInteractionVariants();
		UpdateInteractions interactions = buildInteractionUpdatePacket();
		Map<String, com.hypixel.hytale.protocol.ParticleSpawner> spawners = collectVariantSpawners();
		Map<String, com.hypixel.hytale.protocol.ParticleSystem> systems = collectVariantSystems();
		if (spawners.isEmpty() && systems.isEmpty() && (interactions == null || interactions.interactions == null || interactions.interactions.isEmpty())) {
			return 0;
		}

		int sent = 0;
		Set<Integer> sentHandlers = ConcurrentHashMap.newKeySet();
		for (Map.Entry<UUID, PlayerRef> entry : KNOWN_PLAYERS.entrySet()) {
			PlayerRef playerRef = entry.getValue();
			if (playerRef == null || !playerRef.isValid()) {
				KNOWN_PLAYERS.remove(entry.getKey(), playerRef);
				continue;
			}
			PacketHandler handler = playerRef.getPacketHandler();
			if (!isClientReady(handler)) {
				continue;
			}
			if (sendReloadPackets(handler, spawners, systems, interactions, sentHandlers)) {
				sent++;
			}
		}
		if (sent > 0) {
			int sentCount = sent;
			DebugLogger.debug("Particles", () -> "Resent color variant assets to " + sentCount + " packet handler(s)");
		}
		return sent;
	}

	private static void rememberHandler(PacketHandler handler) {
		if (handler == null || !handler.stillActive()) {
			return;
		}
		KNOWN_HANDLERS.put(System.identityHashCode(handler), handler);
	}

	private static boolean isClientReady(@Nullable PacketHandler handler) {
		if (handler == null || !handler.stillActive()) {
			return false;
		}
		try {
			CompletableFuture<Void> readyFuture = handler.getClientReadyForChunksFuture();
			return readyFuture == null || (readyFuture.isDone() && !readyFuture.isCompletedExceptionally() && !readyFuture.isCancelled());
		} catch (Exception ignored) {
			return false;
		}
	}

	private static boolean sendReloadPackets(
		@Nullable PacketHandler handler,
		Map<String, com.hypixel.hytale.protocol.ParticleSpawner> spawners,
		Map<String, com.hypixel.hytale.protocol.ParticleSystem> systems,
		@Nullable UpdateInteractions interactions,
		Set<Integer> sentHandlers
	) {
		if (!isClientReady(handler)) {
			return false;
		}
		int handlerId = System.identityHashCode(handler);
		if (!sentHandlers.add(handlerId)) {
			return false;
		}
		if (!spawners.isEmpty()) {
			handler.writeNoCache(new UpdateParticleSpawners(UpdateType.AddOrUpdate, new HashMap<>(spawners), new String[0]));
		}
		if (!systems.isEmpty()) {
			handler.writeNoCache(new UpdateParticleSystems(UpdateType.AddOrUpdate, new HashMap<>(systems), new String[0]));
		}
		if (interactions != null && interactions.interactions != null && !interactions.interactions.isEmpty()) {
			handler.writeNoCache(new UpdateInteractions(interactions));
		}
		return true;
	}

	private static void precomputeInteractionVariants() {
		if (ParticleColorRuleRegistry.size() <= 0) {
			return;
		}
		try {
			for (var entry : com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction
				.getAssetMap()
				.getAssetMap()
				.entrySet()) {
				var interactionAsset = entry.getValue();
				if (interactionAsset == null) {
					continue;
				}
				Interaction interaction = interactionAsset.toPacket();
				if (interaction != null && interaction.effects != null) {
					precomputeEffectsParticles(entry.getKey(), interaction.effects);
				}
			}
		} catch (Exception t) {
			LOGGER.atWarning().log("Particle color variant precompute failed: %s", t.getMessage());
		}
	}

	private static void precomputeEffectsParticles(@Nullable String interactionId, InteractionEffects effects) {
		precomputeModelParticles(interactionId, false, effects.particles);
		precomputeModelParticles(interactionId, true, effects.firstPersonParticles);
	}

	private static void precomputeModelParticles(@Nullable String interactionId, boolean firstPerson, @Nullable ModelParticle[] particles) {
		if (particles == null) {
			return;
		}
		for (int i = 0; i < particles.length; i++) {
			ModelParticle particle = particles[i];
			if (particle == null || particle.systemId == null) {
				continue;
			}
			Map<String, Color> colorVars = colorVars(interactionId, firstPerson, i);
			Color baseColor = particle.color != null
			                  ? particle.color
			                  : firstColor(colorVars);
			if (baseColor == null) {
				continue;
			}
			resolveVariant(baseSystemId(particle.systemId), baseColor, colorVars);
		}
	}

	private static void augmentParticleSpawners(UpdateParticleSpawners packet) {
		if (VARIANT_CACHE.isEmpty()) {
			return;
		}
		if (packet.particleSpawners == null) {
			packet.particleSpawners = new HashMap<>();
		}
		int before = packet.particleSpawners.size();
		for (Variant variant : VARIANT_CACHE.values()) {
			packet.particleSpawners.putAll(variant.spawners());
		}
		int added = packet.particleSpawners.size() - before;
		if (added > 0) {
			DebugLogger.debug("Particles", () -> "Added " + added + " color variant particle spawner(s) to UpdateParticleSpawners");
		}
	}

	private static void augmentParticleSystems(UpdateParticleSystems packet) {
		if (VARIANT_CACHE.isEmpty()) {
			return;
		}
		if (packet.particleSystems == null) {
			packet.particleSystems = new HashMap<>();
		}
		int before = packet.particleSystems.size();
		for (Variant variant : VARIANT_CACHE.values()) {
			packet.particleSystems.put(variant.systemId(), variant.system());
		}
		int added = packet.particleSystems.size() - before;
		if (added > 0) {
			DebugLogger.debug("Particles", () -> "Added " + added + " color variant particle system(s) to UpdateParticleSystems");
		}
	}

	private static void rewriteInteractionParticles(UpdateInteractions packet) {
		if (packet.interactions == null || packet.interactions.isEmpty() || ParticleColorRuleRegistry.size() <= 0) {
			return;
		}

		int changed = 0;
		for (Map.Entry<Integer, Interaction> entry : packet.interactions.entrySet()) {
			Interaction interaction = entry.getValue();
			if (interaction == null || interaction.effects == null) {
				continue;
			}
			interaction.effects = new InteractionEffects(interaction.effects);
			changed += rewriteEffectsParticles(interactionIdForIndex(entry.getKey()), interaction.effects);
		}
		if (changed > 0) {
			int changedCount = changed;
			DebugLogger.debug("Particles", () -> "Rewrote " + changedCount + " interaction particle reference(s) in UpdateInteractions");
		}
	}

	private static int rewriteEffectsParticles(@Nullable String interactionId, InteractionEffects effects) {
		effects.particles = copyParticles(effects.particles);
		effects.firstPersonParticles = copyParticles(effects.firstPersonParticles);
		int changed = 0;
		changed += rewriteModelParticles(interactionId, false, effects.particles);
		changed += rewriteModelParticles(interactionId, true, effects.firstPersonParticles);
		return changed;
	}

	@Nullable
	private static ModelParticle[] copyParticles(@Nullable ModelParticle[] particles) {
		if (particles == null) {
			return null;
		}
		ModelParticle[] copy = new ModelParticle[particles.length];
		for (int i = 0; i < particles.length; i++) {
			copy[i] = particles[i] != null
			          ? new ModelParticle(particles[i])
			          : null;
		}
		return copy;
	}

	private static int rewriteModelParticles(@Nullable String interactionId, boolean firstPerson, @Nullable ModelParticle[] particles) {
		if (particles == null || particles.length == 0) {
			return 0;
		}

		int changed = 0;
		for (int i = 0; i < particles.length; i++) {
			ModelParticle particle = particles[i];
			if (particle == null || particle.systemId == null) {
				continue;
			}
			Map<String, Color> colorVars = colorVars(interactionId, firstPerson, i);
			Color baseColor = particle.color != null
			                  ? particle.color
			                  : firstColor(colorVars);
			if (baseColor == null) {
				debugSkip(particle.systemId, "interaction particle has no external color");
				continue;
			}
			Variant variant = resolveVariant(baseSystemId(particle.systemId), baseColor, colorVars);
			if (variant == null) {
				continue;
			}
			particle.systemId = variant.systemId();
			changed++;
		}
		return changed;
	}

	private static Map<String, com.hypixel.hytale.protocol.ParticleSpawner> collectVariantSpawners() {
		LinkedHashMap<String, com.hypixel.hytale.protocol.ParticleSpawner> spawners = new LinkedHashMap<>();
		for (Variant variant : VARIANT_CACHE.values()) {
			spawners.putAll(variant.spawners());
		}
		return spawners;
	}

	private static Map<String, com.hypixel.hytale.protocol.ParticleSystem> collectVariantSystems() {
		LinkedHashMap<String, com.hypixel.hytale.protocol.ParticleSystem> systems = new LinkedHashMap<>();
		for (Variant variant : VARIANT_CACHE.values()) {
			systems.put(variant.systemId(), variant.system());
		}
		return systems;
	}

	@Nullable
	private static UpdateInteractions buildInteractionUpdatePacket() {
		var assetMap = com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction.getAssetMap();
		LinkedHashMap<Integer, Interaction> interactions = new LinkedHashMap<>();
		for (Map.Entry<String, com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction> entry : assetMap
			.getAssetMap()
			.entrySet()) {
			if (entry.getKey() == null || entry.getValue() == null) {
				continue;
			}
			int index = assetMap.getIndexOrDefault(entry.getKey(), -1);
			if (index < 0) {
				continue;
			}
			Interaction interaction = entry.getValue().toPacket();
			if (interaction != null) {
				interactions.put(index, interaction);
			}
		}
		if (interactions.isEmpty()) {
			return null;
		}

		UpdateInteractions packet = new UpdateInteractions(UpdateType.AddOrUpdate, assetMap.getNextIndex(), interactions);
		rewriteInteractionParticles(packet);
		return packet;
	}

	private static void sendVariantIfNeeded(PacketHandler handler, Variant variant) {
		int handlerId = System.identityHashCode(handler);
		Set<String> sent = SENT_VARIANTS_BY_HANDLER.computeIfAbsent(handlerId, ignored -> ConcurrentHashMap.newKeySet());
		if (!sent.add(variant.systemId())) {
			return;
		}
		handler.writeNoCache(new UpdateParticleSpawners(UpdateType.AddOrUpdate, variant.spawners(), new String[0]));
		handler.writeNoCache(new UpdateParticleSystems(UpdateType.AddOrUpdate, Map.of(variant.systemId(), variant.system()), new String[0]));
	}

	@Nullable
	private static Variant resolveVariant(String systemId, Color color) {
		return resolveVariant(systemId, color, Map.of());
	}

	@Nullable
	private static Variant resolveVariant(String systemId, Color color, @Nullable Map<String, Color> colorVars) {
		Map<String, Color> effectiveColorVars = colorVars == null
		                                        ? Map.of()
		                                        : colorVars;
		String cacheKey = variantCacheKey(systemId, color, effectiveColorVars);
		Variant cached = VARIANT_CACHE.get(cacheKey);
		if (cached != null) {
			return cached;
		}
		Variant created = createVariant(systemId, color, effectiveColorVars);
		if (created != null) {
			VARIANT_CACHE.put(cacheKey, created);
		}
		return created;
	}

	private static String variantCacheKey(String systemId, Color color, Map<String, Color> colorVars) {
		return systemId + "|" + colorKey(color) + "|" + colorVarsKey(colorVars);
	}

	private static String baseSystemId(String systemId) {
		int markerIndex = systemId.indexOf(VARIANT_MARKER);
		return markerIndex >= 0
		       ? systemId.substring(0, markerIndex)
		       : systemId;
	}

	@Nullable
	private static Variant createVariant(String systemId, Color color, Map<String, Color> colorVars) {
		ParticleSystem sourceSystemAsset = lookupParticleSystem(systemId);
		if (sourceSystemAsset == null) {
			debugSkip(systemId, "particle system asset not found");
			return null;
		}
		com.hypixel.hytale.protocol.ParticleSystem sourceSystem = sourceSystemAsset.toPacket();
		if (sourceSystem == null || sourceSystem.spawners == null || sourceSystem.spawners.length == 0) {
			debugSkip(systemId, "particle system has no spawners");
			return null;
		}

		LinkedHashMap<String, com.hypixel.hytale.protocol.ParticleSpawner> changedSpawners = new LinkedHashMap<>();
		String variantSuffix = variantSuffix(systemId, color, colorVars, sourceSystem);

		com.hypixel.hytale.protocol.ParticleSystem variantSystem = new com.hypixel.hytale.protocol.ParticleSystem(sourceSystem);
		variantSystem.id = systemId + VARIANT_MARKER + variantSuffix;
		variantSystem.spawners = new ParticleSpawnerGroup[sourceSystem.spawners.length];
		for (int i = 0; i < sourceSystem.spawners.length; i++) {
			ParticleSpawnerGroup group = sourceSystem.spawners[i] != null
			                             ? new ParticleSpawnerGroup(sourceSystem.spawners[i])
			                             : null;
			variantSystem.spawners[i] = group;
			if (group == null || group.spawnerId == null) {
				continue;
			}
			ParticleColorRuleRegistry.SpawnerRules rules = ParticleColorRuleRegistry.get(group.spawnerId);
			if (rules == null) {
				continue;
			}
			ParticleSpawner sourceSpawnerAsset = lookupParticleSpawner(group.spawnerId);
			if (sourceSpawnerAsset == null) {
				debugSkip(systemId, "spawner asset not found: " + group.spawnerId);
				continue;
			}
			com.hypixel.hytale.protocol.ParticleSpawner sourceSpawner = sourceSpawnerAsset.toPacket();
			com.hypixel.hytale.protocol.ParticleSpawner variantSpawner = recolorSpawner(sourceSpawner, rules, color, colorVars);
			if (variantSpawner == null) {
				continue;
			}
			String variantSpawnerId = group.spawnerId + VARIANT_MARKER + variantSuffix;
			variantSpawner.id = variantSpawnerId;
			group.spawnerId = variantSpawnerId;
			changedSpawners.put(variantSpawnerId, variantSpawner);
		}

		if (changedSpawners.isEmpty()) {
			debugSkip(systemId, "no system spawner matched loaded color rules");
			return null;
		}
		DebugLogger.debug(
			"Particles",
			() -> "Created color variant " + variantSystem.id + " for " + systemId
				+ " color=#" + colorKey(color)
				+ (!colorVars.isEmpty()
				   ? " vars=" + colorVarsKey(colorVars)
				   : "")
				+ " spawners=" + changedSpawners.keySet()
		);
		return new Variant(variantSystem.id, variantSystem, Map.copyOf(changedSpawners));
	}

	@Nullable
	private static ParticleSystem lookupParticleSystem(String systemId) {
		ParticleSystem direct = ParticleSystem.getAssetMap().getAsset(systemId);
		if (direct != null) {
			return direct;
		}
		return ItemAssetScanner.lookupWithVariants(ParticleSystem.getAssetMap().getAssetMap(), systemId);
	}

	@Nullable
	private static ParticleSpawner lookupParticleSpawner(String spawnerId) {
		ParticleSpawner direct = ParticleSpawner.getAssetMap().getAsset(spawnerId);
		if (direct != null) {
			return direct;
		}
		return ItemAssetScanner.lookupWithVariants(ParticleSpawner.getAssetMap().getAssetMap(), spawnerId);
	}

	private static void debugSkip(String systemId, String reason) {
		if (ParticleColorRuleRegistry.size() <= 0) {
			return;
		}
		String key = systemId + "|" + reason;
		if (!LOGGED_SKIPS.add(key)) {
			return;
		}
		DebugLogger.debug("Particles", () -> "Skipped color variant for " + systemId + ": " + reason);
	}

	@Nullable
	private static com.hypixel.hytale.protocol.ParticleSpawner recolorSpawner(
		@Nullable com.hypixel.hytale.protocol.ParticleSpawner sourceSpawner,
		ParticleColorRuleRegistry.SpawnerRules rules,
		Color spawnColor,
		Map<String, Color> colorVars
	) {
		if (sourceSpawner == null || sourceSpawner.particle == null || sourceSpawner.particle.animationFrames == null) {
			return null;
		}
		com.hypixel.hytale.protocol.ParticleSpawner variant = new com.hypixel.hytale.protocol.ParticleSpawner(sourceSpawner);
		variant.particle = new com.hypixel.hytale.protocol.Particle(sourceSpawner.particle);
		variant.particle.animationFrames = new HashMap<>(sourceSpawner.particle.animationFrames);

		boolean changed = false;
		LinkedHashMap<Integer, Color> updatedColors = new LinkedHashMap<>();
		for (Map.Entry<Integer, ParticleColorRuleRegistry.FrameRule> entry : rules.animation().entrySet()) {
			ParticleAnimationFrame frame = variant.particle.animationFrames.get(entry.getKey());
			if (frame == null) {
				LOGGER
					.atWarning()
					.log("Particle color rule %s references missing keyframe %d for spawner %s", rules.sourceName(), entry.getKey(), rules.spawnerId());
				continue;
			}
			ParticleAnimationFrame updatedFrame = new ParticleAnimationFrame(frame);
			updatedFrame.color = transformColor(spawnColor, colorVars, entry.getValue());
			variant.particle.animationFrames.put(entry.getKey(), updatedFrame);
			updatedColors.put(entry.getKey(), updatedFrame.color);
			changed = true;
		}
		if (changed) {
			DebugLogger.debug(
				"Particles",
				() -> "Applied color rules for " + rules.spawnerId()
					+ " color=#" + colorKey(spawnColor)
					+ " frames=" + describeFrameColors(updatedColors)
			);
		}
		return changed
		       ? variant
		       : null;
	}

	private static Color transformColor(Color spawnColor, Map<String, Color> palette, ParticleColorRuleRegistry.FrameRule rule) {
		if (rule.paletteColor() != null) {
			Color direct = palette.get(rule.paletteColor());
			return direct != null
			       ? direct
			       : spawnColor;
		}
		float[] hsb = java.awt.Color.RGBtoHSB(unsigned(spawnColor.red), unsigned(spawnColor.green), unsigned(spawnColor.blue), null);
		if (Math.abs(rule.hueShift()) > 1.0E-9D) {
			hsb[0] = wrap01(hsb[0] + (float) rule.hueShift());
		}
		int rgb = java.awt.Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
		int red = (rgb >> 16) & 0xFF;
		int green = (rgb >> 8) & 0xFF;
		int blue = rgb & 0xFF;
		if (rule.brightness() != null) {
			double t = rule.brightness();
			if (t > 0.5D) {
				double amount = (t - 0.5D) / 0.5D;
				red = lerp(red, 255, amount);
				green = lerp(green, 255, amount);
				blue = lerp(blue, 255, amount);
			} else if (t < 0.5D) {
				double amount = t / 0.5D;
				red = lerp(0, red, amount);
				green = lerp(0, green, amount);
				blue = lerp(0, blue, amount);
			}
		}
		return new Color((byte) red, (byte) green, (byte) blue);
	}

	private static int lerp(int left, int right, double amount) {
		double clamped = Math.max(0.0D, Math.min(1.0D, amount));
		return Math.max(0, Math.min(255, (int) Math.round(left + (right - left) * clamped)));
	}

	private static float wrap01(float value) {
		float wrapped = value % 1.0F;
		return wrapped < 0.0F
		       ? wrapped + 1.0F
		       : wrapped;
	}

	private static int unsigned(byte value) {
		return value & 0xFF;
	}

	private static String colorKey(Color color) {
		return String.format(Locale.ROOT, "%02x%02x%02x", unsigned(color.red), unsigned(color.green), unsigned(color.blue));
	}

	private static String describeFrameColors(Map<Integer, Color> colors) {
		StringBuilder builder = new StringBuilder("{");
		boolean first = true;
		for (Map.Entry<Integer, Color> entry : colors.entrySet()) {
			if (!first) {
				builder.append(", ");
			}
			builder.append(entry.getKey()).append("=#").append(colorKey(entry.getValue()));
			first = false;
		}
		return builder.append('}').toString();
	}

	private static String variantSuffix(String systemId, Color color, Map<String, Color> colorVars, com.hypixel.hytale.protocol.ParticleSystem system) {
		StringBuilder builder = new StringBuilder(systemId)
			.append('|')
			.append(colorKey(color))
			.append('|')
			.append(colorVarsKey(colorVars));
		if (system.spawners != null) {
			for (ParticleSpawnerGroup group : system.spawners) {
				if (group == null || group.spawnerId == null) {
					continue;
				}
				ParticleColorRuleRegistry.SpawnerRules rules = ParticleColorRuleRegistry.get(group.spawnerId);
				if (rules != null) {
					builder.append('|').append(rules.spawnerId()).append('=').append(rules.animation());
				}
			}
		}
		return shortHash(builder.toString());
	}

	private static Map<String, Color> colorVars(@Nullable String interactionId, boolean firstPerson, int particleIndex) {
		Map<String, Color> vars = ParticleInteractionPaletteRegistry.get(interactionId, firstPerson, particleIndex);
		return vars == null
		       ? Map.of()
		       : vars;
	}

	@Nullable
	private static String interactionIdForIndex(int index) {
		var assetMap = com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction.getAssetMap();
		for (String key : assetMap.getAssetMap().keySet()) {
			if (assetMap.getIndexOrDefault(key, -1) == index) {
				return key;
			}
		}
		return null;
	}

	private static String colorVarsKey(Map<String, Color> colorVars) {
		if (colorVars.isEmpty()) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		colorVars
			.entrySet()
			.stream()
			.sorted(Map.Entry.comparingByKey())
			.forEach(entry -> builder
				.append(entry.getKey())
				.append("=#")
				.append(colorKey(entry.getValue()))
				.append(';'));
		return builder.toString();
	}

	@Nullable
	private static Color firstColor(Map<String, Color> colors) {
		for (Color color : colors.values()) {
			if (color != null) {
				return color;
			}
		}
		return null;
	}

	private static String shortHash(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			StringBuilder out = new StringBuilder(12);
			for (int i = 0; i < 6 && i < hash.length; i++) {
				out.append(String.format(Locale.ROOT, "%02x", hash[i] & 0xFF));
			}
			return out.toString();
		} catch (Exception ignored) {
			return Integer.toHexString(value.hashCode());
		}
	}

	private record Variant(
		String systemId,
		com.hypixel.hytale.protocol.ParticleSystem system,
		Map<String, com.hypixel.hytale.protocol.ParticleSpawner> spawners
	) {
	}
}
