package Tenzinn.Deathmatch.Bots;

import Tenzinn.Core.GameMatch;
import Tenzinn.Core.Listeners.MapListeners;
import Tenzinn.Core.Objects.PlayerStats;
import Tenzinn.Core.Tools.RefactorTool;
import com.thescar.hygunsplugin.content.settings.AutoGuidanceSettings;
import com.thescar.hygunsplugin.content.settings.GunSettings;
import com.thescar.hygunsplugin.content.settings.WeaponFireSettings;
import com.thescar.hygunsplugin.content.settings.WeaponProjectileSettings;
import com.thescar.hygunsplugin.content.settings.WallPenetrationSettings;
import com.thescar.hygunsplugin.content.weapon.WeaponContentApi;
import com.thescar.hygunsplugin.gameplay.projectile.HitDamageModifiers;
import com.thescar.hygunsplugin.gameplay.projectile.ShootProjectile;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.path.WorldPath;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import it.unimi.dsi.fastutil.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DeathmatchBotManager {

    private static final String ROLE_T_BOT = "OrbisOffensive_T_Bot";
    private static final String ROLE_CT_BOT = "OrbisOffensive_CT_Bot";
    private static final String T_BOT_WEAPON_ID = "Weapon_AK47";
    private static final String CT_BOT_WEAPON_ID = "Weapon_M4A1s";
    private static final String T_BOT_WEAPON_NAME = "AK47";
    private static final String CT_BOT_WEAPON_NAME = "M4A1s";
    private static final String WEAPON_AK47_FIRE_SOUND_ID = "World_AK47_Fire";
    private static final String WEAPON_M4A1S_FIRE_SOUND_ID = "World_M4A1s_Fire";
    private static final String DEFAULT_PROJECTILE_CONFIG_ID = "Hyguns_Projectile_Config_Bullet";

    // Fallback defaults. Runtime values are sourced from bots.json.
    public static final int MAX_DM_PARTICIPANTS = 10;
    public static final long BOT_TICK_MS = 500L;
    public static final double BOT_TARGET_RANGE = 28.0d;
    public static final long BOT_RESPAWN_DELAY_MS = 3000L;
    public static final double BOT_RESPAWN_PLAYER_AVOID_RANGE = 5.0d;
    public static final long BOT_MOVEMENT_UPDATE_MS = 1000L;
    public static final double BOT_DESIRED_RANGE_MIN = 8.0d;
    public static final double BOT_DESIRED_RANGE_MAX = 18.0d;
    public static final double BOT_TOO_CLOSE_RANGE = 5.0d;
    public static final double BOT_CHASE_RANGE = 24.0d;
    public static final long BOT_REPOSITION_COOLDOWN_MS = 2000L;
    public static final double BOT_MAX_TELEPORT_REPOSITION_DISTANCE = 0.0d;
    public static final int AK_BOT_DAMAGE = 8;
    public static final int M4_BOT_DAMAGE = 7;
    public static final long AK_BOT_ATTACK_COOLDOWN_MS = 750L;
    public static final long M4_BOT_ATTACK_COOLDOWN_MS = 650L;
    public static final double BOT_HIT_CHANCE = 0.55d;
    public static final double BOT_CLOSE_RANGE_HIT_CHANCE = 0.65d;
    public static final double BOT_LONG_RANGE_HIT_CHANCE = 0.35d;
    public static final boolean BOT_DIRECT_DAMAGE_FALLBACK = true;
    public static final double BOT_YAW_OFFSET_DEGREES = 0.0d;
    public static final double BOT_PITCH_OFFSET_DEGREES = 0.0d;
    public static final double BOT_PROJECTILE_EYE_HEIGHT = 1.45d;
    public static final double BOT_PROJECTILE_FORWARD_OFFSET = 0.4d;

    private static final double BOT_CLOSE_RANGE = 8.0d;
    private static final double BOT_LONG_RANGE_START = 20.0d;
    private static final long BOT_TARGET_LOCK_MIN_MS = 700L;
    private static final long BOT_TARGET_LOCK_MAX_MS = 1300L;
    private static final double BOT_TARGET_LOAD_PENALTY_SQ = 9.0d;
    private static final long BOT_INITIAL_ATTACK_JITTER_MIN_MS = 200L;
    private static final long BOT_INITIAL_ATTACK_JITTER_MAX_MS = 900L;
    private static final double BOT_PROJECTILE_TARGET_HEIGHT_OFFSET = 1.35d;
    private static final int MAX_PENDING_BOT_FIRE_REQUESTS = 512;
    private static final int MAX_BOT_FIRE_REQUESTS_PER_TICK = 96;
    private static final long BOT_FIRE_REQUEST_TTL_MS = 1500L;
    private static final long DEBUG_MOVE_LOG_RATE_MS = 500L;
    private static final long DEBUG_FACE_LOG_RATE_MS = 500L;
    private static final long DEBUG_FIRE_LOG_RATE_MS = 500L;
    private static final HitDamageModifiers BOT_HIT_DAMAGE_MODIFIERS = HitDamageModifiers.DEFAULT;
    private static final AutoGuidanceSettings BOT_AUTO_GUIDANCE_SETTINGS = AutoGuidanceSettings.DEFAULTS;
    private static final WallPenetrationSettings BOT_WALL_PENETRATION_SETTINGS = WallPenetrationSettings.DEFAULTS;

    private static final Logger LOGGER = Logger.getLogger(DeathmatchBotManager.class.getName());

    private static final Map<UUID, List<DeathmatchBot>> BOTS_BY_MATCH = new ConcurrentHashMap<>();
    private static final Map<UUID, GameMatch> MATCHES_BY_ID = new ConcurrentHashMap<>();
    private static final Map<Ref<EntityStore>, DeathmatchBot> BOT_BY_REF = new ConcurrentHashMap<>();
    private static final Set<Ref<EntityStore>> BOT_REFS = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> SPAWNING_MATCHES = ConcurrentHashMap.newKeySet();

    private static final Object TICKER_LOCK = new Object();
    private static ScheduledFuture<?> botTickerTask;
    private static volatile DamageCause cachedBotDamageCause;
    private static volatile DeathmatchBotConfig botConfig = DeathmatchBotConfig.defaults();
    private static volatile boolean botsEnabled = botConfig.enabled();
    private static volatile String botConfigPath = DeathmatchBotConfig.getConfigPath();
    private static final AtomicBoolean damageApiWarningLogged = new AtomicBoolean(false);
    private static final AtomicBoolean lineOfSightWarningLogged = new AtomicBoolean(false);
    private static final AtomicBoolean movementWarningLogged = new AtomicBoolean(false);
    private static final AtomicBoolean rotationWarningLogged = new AtomicBoolean(false);
    private static final AtomicBoolean botsDisabledLogPrinted = new AtomicBoolean(false);
    private static final AtomicBoolean weaponEquipWarningLogged = new AtomicBoolean(false);
    private static final AtomicBoolean weaponSoundWarningLogged = new AtomicBoolean(false);
    private static final AtomicBoolean projectileModeWarningLogged = new AtomicBoolean(false);
    private static final AtomicBoolean projectileSpawnFailureWarningLogged = new AtomicBoolean(false);
    private static final AtomicBoolean projectileQueueOverflowWarningLogged = new AtomicBoolean(false);
    private static final Set<String> missingWeaponConfigWarnings = ConcurrentHashMap.newKeySet();
    private static final Set<String> weaponConfigErrorWarnings = ConcurrentHashMap.newKeySet();
    private static final Set<String> missingSoundWarnings = ConcurrentHashMap.newKeySet();
    private static final Map<String, Integer> soundEventIndexCache = new ConcurrentHashMap<>();
    private static final Map<UUID, ConcurrentLinkedQueue<BotFireRequest>> BOT_FIRE_REQUESTS_BY_WORLD = new ConcurrentHashMap<>();
    private static final AtomicInteger BOT_FIRE_REQUEST_COUNT = new AtomicInteger(0);
    private static final Map<String, Long> DEBUG_LOG_TIMESTAMPS_MS = new ConcurrentHashMap<>();

    public static void initialize() {
        reloadConfig();
    }

    public static boolean reloadConfig() {
        DeathmatchBotConfig.LoadResult result = DeathmatchBotConfig.loadOrCreate();
        if (result == null || result.config() == null) {
            botConfig = DeathmatchBotConfig.defaults();
            botConfigPath = DeathmatchBotConfig.getConfigPath();
            botsEnabled = botConfig.enabled();
            restartTickerForConfig();
            return false;
        }

        botConfig = result.config();
        botConfigPath = result.path();
        botsEnabled = botConfig.enabled();

        if (botsEnabled) {
            botsDisabledLogPrinted.set(false);
        } else {
            botsDisabledLogPrinted.set(true);
            removeAllBots();
        }

        restartTickerForConfig();

        if (result.createdDefaultFile()) {
            LOGGER.log(Level.INFO, "[DeathmatchBot] Created default bots config: " + botConfigPath);
        }
        if (!result.parseSuccessful()) {
            LOGGER.log(Level.WARNING, "[DeathmatchBot] bots.json invalid; safe defaults are active for this runtime.");
        } else {
            debugLog("[DeathmatchBot] Reloaded config from " + botConfigPath);
        }

        return result.parseSuccessful();
    }

    public static boolean isTickerRunning() {
        synchronized (TICKER_LOCK) {
            return botTickerTask != null && !botTickerTask.isCancelled() && !botTickerTask.isDone();
        }
    }

    public static boolean isBotsEnabled() {
        return botsEnabled;
    }

    public static void setBotsEnabled(boolean enabled) {
        botsEnabled = enabled;
        if (enabled) {
            botsDisabledLogPrinted.set(false);
            if (hasActiveBots()) {
                ensureTickerRunning();
            }
            debugLog("[DeathmatchBot] Bots enabled at runtime.");
            return;
        }

        botsDisabledLogPrinted.set(true);
        removeAllBots();
        debugLog("[DeathmatchBot] Bots disabled at runtime and active bots cleared.");
    }

    public static int getConfiguredFillDeathmatchTo() {
        return config().fillDeathmatchTo();
    }

    public static String getConfigPath() {
        return botConfigPath;
    }

    public static boolean isBotProjectileModeEnabled() {
        return config().botUseRealProjectiles();
    }

    public static boolean isBotDirectDamageFallbackEnabled() {
        return config().botDirectDamageFallback();
    }

    public static double getBotYawOffsetDegrees() {
        return config().botYawOffsetDegrees();
    }

    public static int getTotalBotCountAllMatches() {
        int total = 0;
        for (List<DeathmatchBot> bots : BOTS_BY_MATCH.values()) {
            if (bots == null) {
                continue;
            }
            synchronized (bots) {
                total += bots.size();
            }
        }
        return total;
    }

    public static int getAliveBotCountAllMatches() {
        int total = 0;
        for (List<DeathmatchBot> bots : BOTS_BY_MATCH.values()) {
            if (bots == null) {
                continue;
            }
            synchronized (bots) {
                for (DeathmatchBot bot : bots) {
                    if (bot != null && bot.alive) {
                        total++;
                    }
                }
            }
        }
        return total;
    }

    public static boolean respawnFillBots(GameMatch match, World world) {
        if (match == null || world == null || match.getMapId() == null || match.getMapId().isBlank()) {
            return false;
        }
        if (!isDeathmatch(match)) {
            return false;
        }

        removeBots(match);
        if (!isBotsEnabled()) {
            return true;
        }

        spawnFillBots(match, world, match.getMapId());
        return true;
    }

    public static void spawnFillBots(GameMatch match, World world, String mapId) {
        if (match == null || world == null || mapId == null || mapId.isBlank()) {
            return;
        }
        if (!isBotsEnabled()) {
            if (botsDisabledLogPrinted.compareAndSet(false, true)) {
                LOGGER.log(Level.INFO, "[DeathmatchBot] Bots are disabled. Skipping DM bot spawn.");
            }
            return;
        }
        if (!isDeathmatch(match) || match.getState() == GameMatch.MatchState.FINISHED || match.getPlayers().isEmpty()) {
            return;
        }

        UUID matchId = match.getMatchId();
        if (matchId == null) {
            return;
        }

        MATCHES_BY_ID.put(matchId, match);

        List<DeathmatchBot> existing = BOTS_BY_MATCH.get(matchId);
        if (existing != null && !existing.isEmpty()) {
            ensureTickerRunning();
            return;
        }

        if (!SPAWNING_MATCHES.add(matchId)) {
            return;
        }

        int humans = match.getPlayerCount();
        int fillTarget = Math.max(0, Math.min(MAX_DM_PARTICIPANTS, config().fillDeathmatchTo()));
        if (fillTarget <= 0) {
            debugLog("[DeathmatchBot] fillDeathmatchTo is 0. Skipping DM bot spawn for match=" + matchId);
            SPAWNING_MATCHES.remove(matchId);
            return;
        }

        int botsNeeded = Math.max(0, fillTarget - humans);
        if (botsNeeded <= 0) {
            SPAWNING_MATCHES.remove(matchId);
            return;
        }

        ArrayList<Vector3d> spawns;
        try {
            spawns = RefactorTool.getSpawns(mapId, MapListeners.SpawnMode.DM);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[DeathmatchBot] Unable to load DM spawns for map=" + mapId, e);
            SPAWNING_MATCHES.remove(matchId);
            return;
        }

        if (spawns == null || spawns.isEmpty()) {
            LOGGER.log(Level.WARNING, "[DeathmatchBot] No DM spawns found for map=" + mapId);
            SPAWNING_MATCHES.remove(matchId);
            return;
        }

        List<DeathmatchBot> matchBots = Collections.synchronizedList(new ArrayList<>());
        BOTS_BY_MATCH.put(matchId, matchBots);

        world.execute(() -> {
            int createdCount = 0;
            try {
                if (!SPAWNING_MATCHES.contains(matchId)) {
                    return;
                }
                if (!isDeathmatch(match) || match.getState() == GameMatch.MatchState.FINISHED || match.getPlayers().isEmpty()) {
                    BOTS_BY_MATCH.remove(matchId);
                    return;
                }

                Store<EntityStore> store = world.getEntityStore().getStore();

                for (int i = 0; i < botsNeeded; i++) {
                    if (!SPAWNING_MATCHES.contains(matchId)) {
                        break;
                    }
                    if (match.getState() == GameMatch.MatchState.FINISHED || match.getPlayers().isEmpty()) {
                        break;
                    }

                    boolean terroristBot = i % 2 == 0;
                    String roleId = terroristBot ? ROLE_T_BOT : ROLE_CT_BOT;
                    String weaponId = terroristBot ? T_BOT_WEAPON_ID : CT_BOT_WEAPON_ID;
                    String weaponDisplayName = terroristBot ? T_BOT_WEAPON_NAME : CT_BOT_WEAPON_NAME;
                    String displayName = terroristBot ? "T Bot " + (i + 1) : "CT Bot " + (i + 1);

                    Vector3d spawn = toCenteredSpawn(spawns.get((humans + i) % spawns.size()));
                    Vector3f rotation = new Vector3f(0f, 0f, 0f);

                    Ref<EntityStore> botRef;
                    try {
                        botRef = spawnRole(store, roleId, spawn, rotation);
                    } catch (Exception spawnError) {
                        LOGGER.log(Level.WARNING, "[DeathmatchBot] Failed to spawn custom role '" + roleId
                                + "' for match=" + matchId, spawnError);
                        continue;
                    }

                    DeathmatchBot bot = new DeathmatchBot(matchId, botRef, roleId, displayName, weaponId, weaponDisplayName, spawn);
                    bot.nextAttackAtMs = nowWithAttackJitter();
                    bot.nextMovementAtMs = nowWithMovementJitter();
                    bot.nextRepositionAtMs = 0L;
                    bot.lastTargetPosition = null;
                    bot.failedMovementAttempts = 0;
                    bot.lastMovementObservedPosition = new Vector3d(spawn);
                    bot.lastMovementObservedAtMs = System.currentTimeMillis();
                    applyBotDisplayName(store, botRef, displayName);
                    equipBotWeapon(store, botRef, weaponId);
                    matchBots.add(bot);
                    registerBotRef(bot, botRef);
                    createdCount++;
                }

                if (createdCount > 0) {
                    ensureTickerRunning();
                }
            } finally {
                SPAWNING_MATCHES.remove(matchId);
                List<DeathmatchBot> created = BOTS_BY_MATCH.get(matchId);
                if (created != null && created.isEmpty()) {
                    BOTS_BY_MATCH.remove(matchId);
                }

                if (createdCount > 0) {
                    LOGGER.log(Level.INFO, "[DeathmatchBot] Spawned " + createdCount + " bots for match=" + matchId);
                }
            }
        });
    }

    public static List<DeathmatchBot> getBots(GameMatch match) {
        if (match == null || match.getMatchId() == null) {
            return List.of();
        }

        List<DeathmatchBot> bots = BOTS_BY_MATCH.get(match.getMatchId());
        if (bots == null || bots.isEmpty()) {
            return List.of();
        }

        synchronized (bots) {
            return new ArrayList<>(bots);
        }
    }

    public static int getTotalBotCount(GameMatch match) {
        return getBots(match).size();
    }

    public static int getAliveBotCount(GameMatch match) {
        int count = 0;
        for (DeathmatchBot bot : getBots(match)) {
            if (bot != null && bot.alive) {
                count++;
            }
        }
        return count;
    }

    public static int getTotalBotKills(GameMatch match) {
        int total = 0;
        for (DeathmatchBot bot : getBots(match)) {
            if (bot != null) {
                total += bot.kills;
            }
        }
        return total;
    }

    public static int getTotalBotDeaths(GameMatch match) {
        int total = 0;
        for (DeathmatchBot bot : getBots(match)) {
            if (bot != null) {
                total += bot.deaths;
            }
        }
        return total;
    }

    public static boolean isBot(Ref<EntityStore> ref) {
        return ref != null && BOT_REFS.contains(ref);
    }

    public static void recordBotKill(Ref<EntityStore> botRef) {
        if (botRef == null) {
            return;
        }

        DeathmatchBot bot = BOT_BY_REF.get(botRef);
        if (bot == null) {
            return;
        }

        bot.kills++;
        bot.score += 15;
    }

    public static void handleBotDeath(Ref<EntityStore> botRef, Ref<EntityStore> killerRef) {
        if (botRef == null) {
            return;
        }

        DeathmatchBot bot = BOT_BY_REF.get(botRef);
        if (bot == null || !bot.alive) {
            return;
        }

        bot.alive = false;
        bot.deaths++;
        bot.score = Math.max(0, bot.score - 10);
        bot.nextAttackAtMs = 0L;
        bot.respawnAtMs = 0L;
        bot.nextMovementAtMs = 0L;
        bot.nextRepositionAtMs = 0L;
        bot.targetPlayerUuid = null;
        bot.targetLockUntilMs = 0L;
        bot.lastTargetPosition = null;
        bot.failedMovementAttempts = 0;
        bot.lastMovementObservedPosition = null;
        bot.lastMovementObservedAtMs = 0L;

        unregisterBotRef(botRef);
        removePendingFireRequestsForBot(botRef);
        removeBotEntity(botRef);

        GameMatch match = MATCHES_BY_ID.get(bot.matchId);
        if (!isMatchAvailableForBots(match, bot.matchId)) {
            return;
        }

        applyKillerCredit(match, bot, killerRef);
        scheduleRespawn(match, bot);
    }

    public static void removeBots(GameMatch match) {
        if (match == null || match.getMatchId() == null) {
            return;
        }

        UUID matchId = match.getMatchId();
        SPAWNING_MATCHES.remove(matchId);
        MATCHES_BY_ID.remove(matchId);
        removePendingFireRequestsForMatch(matchId);

        List<DeathmatchBot> bots = BOTS_BY_MATCH.remove(matchId);
        if (bots == null || bots.isEmpty()) {
            stopTickerIfIdle();
            return;
        }

        int removed = 0;
        List<DeathmatchBot> snapshot;
        synchronized (bots) {
            snapshot = new ArrayList<>(bots);
            bots.clear();
        }

        for (DeathmatchBot bot : snapshot) {
            if (bot == null) {
                continue;
            }

            bot.alive = false;
            bot.respawnAtMs = 0L;
            bot.nextAttackAtMs = 0L;
            bot.nextMovementAtMs = 0L;
            bot.nextRepositionAtMs = 0L;
            bot.targetPlayerUuid = null;
            bot.targetLockUntilMs = 0L;
            bot.lastTargetPosition = null;
            bot.failedMovementAttempts = 0;
            bot.lastMovementObservedPosition = null;
            bot.lastMovementObservedAtMs = 0L;

            Ref<EntityStore> ref = bot.entityRef;
            if (ref != null) {
                unregisterBotRef(ref);
                removeBotEntity(ref);
                removed++;
            }
        }

        if (removed > 0) {
            LOGGER.log(Level.INFO, "[DeathmatchBot] Removed " + removed + " bots for match=" + matchId);
        }

        stopTickerIfIdle();
    }

    public static void removeAllBots() {
        SPAWNING_MATCHES.clear();
        MATCHES_BY_ID.clear();
        clearPendingFireRequests();

        List<List<DeathmatchBot>> allLists = new ArrayList<>(BOTS_BY_MATCH.values());
        BOTS_BY_MATCH.clear();

        int removed = 0;
        for (List<DeathmatchBot> botList : allLists) {
            if (botList == null || botList.isEmpty()) {
                continue;
            }

            List<DeathmatchBot> snapshot;
            synchronized (botList) {
                snapshot = new ArrayList<>(botList);
                botList.clear();
            }

            for (DeathmatchBot bot : snapshot) {
                if (bot == null) {
                    continue;
                }

                bot.alive = false;
                bot.respawnAtMs = 0L;
                bot.nextAttackAtMs = 0L;
                bot.nextMovementAtMs = 0L;
                bot.nextRepositionAtMs = 0L;
                bot.targetPlayerUuid = null;
                bot.targetLockUntilMs = 0L;
                bot.lastTargetPosition = null;
                bot.failedMovementAttempts = 0;
                bot.lastMovementObservedPosition = null;
                bot.lastMovementObservedAtMs = 0L;

                Ref<EntityStore> ref = bot.entityRef;
                if (ref != null) {
                    unregisterBotRef(ref);
                    removeBotEntity(ref);
                    removed++;
                }
            }
        }

        BOT_REFS.clear();
        BOT_BY_REF.clear();
        stopTicker();

        if (removed > 0) {
            LOGGER.log(Level.INFO, "[DeathmatchBot] Removed " + removed + " bots globally");
        }
    }

    private static void ensureTickerRunning() {
        synchronized (TICKER_LOCK) {
            if (botTickerTask != null && !botTickerTask.isCancelled() && !botTickerTask.isDone()) {
                return;
            }

            long tickMs = Math.max(250L, config().tickMs());
            botTickerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
                try {
                    tickBots();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "[DeathmatchBot] Bot ticker error: " + e.getMessage(), e);
                }
            }, tickMs, tickMs, TimeUnit.MILLISECONDS);

            debugLog("[DeathmatchBot] Bot ticker started with interval=" + tickMs + "ms");
        }
    }

    private static void tickBots() {
        if (!isBotsEnabled()) {
            removeAllBots();
            return;
        }

        if (!hasActiveBots()) {
            stopTickerIfIdle();
            return;
        }

        long now = System.currentTimeMillis();
        List<UUID> matchIds = new ArrayList<>(BOTS_BY_MATCH.keySet());

        for (UUID matchId : matchIds) {
            GameMatch match = MATCHES_BY_ID.get(matchId);
            List<DeathmatchBot> bots = BOTS_BY_MATCH.get(matchId);
            if (match == null || bots == null || bots.isEmpty()) {
                continue;
            }

            World world = resolveMatchWorld(match, bots);
            if (world == null) {
                continue;
            }

            world.execute(() -> tickMatchBots(matchId, now));
        }
    }

    private static void tickMatchBots(UUID matchId, long now) {
        GameMatch match = MATCHES_BY_ID.get(matchId);
        List<DeathmatchBot> bots = BOTS_BY_MATCH.get(matchId);
        if (match == null || bots == null || bots.isEmpty()) {
            return;
        }

        List<DeathmatchBot> snapshot;
        synchronized (bots) {
            snapshot = new ArrayList<>(bots);
        }

        Map<UUID, Integer> targetLoad = new HashMap<>();
        for (DeathmatchBot bot : snapshot) {
            if (bot != null && bot.alive && bot.targetPlayerUuid != null) {
                targetLoad.merge(bot.targetPlayerUuid, 1, Integer::sum);
            }
        }

        for (DeathmatchBot bot : snapshot) {
            tickBot(match, bot, now, targetLoad);
        }
    }

    private static void tickBot(GameMatch match, DeathmatchBot bot, long now, Map<UUID, Integer> targetLoad) {
        if (bot == null) {
            return;
        }
        if (!isDeathmatch(match) || match.getState() == GameMatch.MatchState.FINISHED || match.getPlayers().isEmpty()) {
            return;
        }

        if (!bot.alive) {
            if (bot.respawnAtMs > 0L && now >= bot.respawnAtMs) {
                respawnBot(match, bot);
            }
            return;
        }

        Ref<EntityStore> botRef = bot.entityRef;
        if (botRef == null || !botRef.isValid()) {
            bot.alive = false;
            bot.targetPlayerUuid = null;
            bot.targetLockUntilMs = 0L;
            bot.lastTargetPosition = null;
            scheduleRespawn(match, bot);
            return;
        }

        Store<EntityStore> store = botRef.getStore();
        if (store == null) {
            return;
        }

        TransformComponent botTransform = store.getComponent(botRef, TransformComponent.getComponentType());
        if (botTransform == null) {
            return;
        }

        Vector3d botPosition = new Vector3d(botTransform.getPosition());
        TargetCandidate target = selectTarget(match, bot, store, botPosition, now, targetLoad);
        if (target == null) {
            bot.targetPlayerUuid = null;
            bot.targetLockUntilMs = 0L;
            bot.lastTargetPosition = null;
            return;
        }

        if (target.playerUuid != null) {
            if (!target.playerUuid.equals(bot.targetPlayerUuid)) {
                bot.targetPlayerUuid = target.playerUuid;
                bot.targetLockUntilMs = now + ThreadLocalRandom.current().nextLong(BOT_TARGET_LOCK_MIN_MS, BOT_TARGET_LOCK_MAX_MS + 1L);
            } else if (now >= bot.targetLockUntilMs) {
                bot.targetLockUntilMs = now + ThreadLocalRandom.current().nextLong(BOT_TARGET_LOCK_MIN_MS, BOT_TARGET_LOCK_MAX_MS + 1L);
            }

            targetLoad.merge(target.playerUuid, 1, Integer::sum);
        } else {
            bot.targetPlayerUuid = null;
            bot.targetLockUntilMs = 0L;
        }

        bot.lastTargetPosition = new Vector3d(target.position);
        updateBotMovement(match, bot, store, botPosition, target, now);

        if (now < bot.nextAttackAtMs) {
            return;
        }

        boolean faced = faceTarget(store, botRef, botPosition, targetAimPosition(target.position));
        if (!faced) {
            debugLogEvery("face:skip:" + bot.displayName, DEBUG_FACE_LOG_RATE_MS,
                    "[DeathmatchBot][debug] Face update skipped for " + bot.displayName);
        }
        BotWeaponTuning weaponTuning = resolveWeaponTuning(bot.weaponId);
        bot.nextAttackAtMs = now + weaponTuning.cooldownMs();

        if (!hasLineOfSight(store, botPosition, targetAimPosition(target.position))) {
            return;
        }
        if (!config().botUseRealProjectiles()) {
            double hitChance = resolveHitChance(target.distanceSq, weaponTuning.spread());
            if (ThreadLocalRandom.current().nextDouble() > hitChance) {
                return;
            }
        }

        playBotFireSound(bot, store, botPosition);
        tryFireBotProjectile(bot, target.playerRef, store, botPosition, target.position, weaponTuning);
        return;
    }

    private static TargetCandidate selectTarget(GameMatch match,
                                                DeathmatchBot bot,
                                                Store<EntityStore> store,
                                                Vector3d botPosition,
                                                long now,
                                                Map<UUID, Integer> targetLoad) {
        UUID botWorldUuid = getWorldUuidFromStore(store);

        if (bot.targetPlayerUuid != null && now < bot.targetLockUntilMs) {
            TargetCandidate locked = findCandidateByUuid(match, bot.targetPlayerUuid, store, botPosition, botWorldUuid);
            if (locked != null) {
                return locked;
            }
        }

        TargetCandidate best = null;
        double bestScore = Double.MAX_VALUE;

        for (PlayerRef matchPlayerRef : match.getPlayers()) {
            TargetCandidate candidate = toCandidate(matchPlayerRef, store, botPosition, botWorldUuid);
            if (candidate == null) {
                continue;
            }

            int load = targetLoad.getOrDefault(candidate.playerUuid, 0);
            double score = candidate.distanceSq + (load * BOT_TARGET_LOAD_PENALTY_SQ);

            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        TargetCandidate botCandidate = findNearestBotCandidate(match, bot, store, botPosition, botWorldUuid);
        if (botCandidate != null && botCandidate.distanceSq < bestScore) {
            best = botCandidate;
        }

        return best;
    }

    private static TargetCandidate findNearestBotCandidate(GameMatch match,
                                                           DeathmatchBot self,
                                                           Store<EntityStore> store,
                                                           Vector3d botPosition,
                                                           UUID botWorldUuid) {
        if (match == null || self == null || store == null || botPosition == null) {
            return null;
        }

        TargetCandidate best = null;
        double bestDistanceSq = Double.MAX_VALUE;
        double targetRange = Math.max(64.0d, config().targetRange());
        double maxDistanceSq = targetRange * targetRange;

        for (DeathmatchBot other : getBots(match)) {
            if (other == null || other == self || !other.alive) {
                continue;
            }
            Ref<EntityStore> otherRef = other.entityRef;
            if (otherRef == null || !otherRef.isValid()) {
                continue;
            }

            Store<EntityStore> otherStore = otherRef.getStore();
            if (otherStore == null) {
                continue;
            }
            UUID otherWorldUuid = getWorldUuidFromStore(otherStore);
            if (botWorldUuid != null && otherWorldUuid != null && !botWorldUuid.equals(otherWorldUuid)) {
                continue;
            }

            TransformComponent otherTransform = store.getComponent(otherRef, TransformComponent.getComponentType());
            if (otherTransform == null) {
                continue;
            }

            Vector3d otherPosition = new Vector3d(otherTransform.getPosition());
            double distanceSq = distanceSq(botPosition, otherPosition);
            if (distanceSq > maxDistanceSq || distanceSq >= bestDistanceSq) {
                continue;
            }

            bestDistanceSq = distanceSq;
            best = new TargetCandidate(null, otherRef, otherPosition, distanceSq);
        }

        return best;
    }

    private static TargetCandidate findCandidateByUuid(GameMatch match,
                                                       UUID targetUuid,
                                                       Store<EntityStore> store,
                                                       Vector3d botPosition,
                                                       UUID botWorldUuid) {
        for (PlayerRef matchPlayerRef : match.getPlayers()) {
            if (matchPlayerRef == null || matchPlayerRef.getUuid() == null) {
                continue;
            }
            if (!matchPlayerRef.getUuid().equals(targetUuid)) {
                continue;
            }
            return toCandidate(matchPlayerRef, store, botPosition, botWorldUuid);
        }

        return null;
    }

    private static TargetCandidate toCandidate(PlayerRef matchPlayerRef,
                                               Store<EntityStore> store,
                                               Vector3d botPosition,
                                               UUID botWorldUuid) {
        if (matchPlayerRef == null || matchPlayerRef.getUuid() == null) {
            return null;
        }

        PlayerRef livePlayerRef = Universe.get().getPlayer(matchPlayerRef.getUuid());
        if (livePlayerRef == null || livePlayerRef.getReference() == null || livePlayerRef.getWorldUuid() == null) {
            return null;
        }
        if (botWorldUuid != null && !botWorldUuid.equals(livePlayerRef.getWorldUuid())) {
            return null;
        }

        PlayerStats stats = RefactorTool.getPlayerStats(livePlayerRef);
        if (stats == null || stats.playerState != PlayerStats.PlayerState.DEFAULT) {
            return null;
        }

        Player player = RefactorTool.getPlayer(livePlayerRef);
        if (player == null || player.getReference() == null || !player.getReference().isValid()) {
            return null;
        }

        Ref<EntityStore> playerEntityRef = player.getReference();
        TransformComponent playerTransform = store.getComponent(playerEntityRef, TransformComponent.getComponentType());
        if (playerTransform == null) {
            return null;
        }

        Vector3d targetPos = new Vector3d(playerTransform.getPosition());
        double distanceSq = distanceSq(botPosition, targetPos);
        double targetRange = Math.max(64.0d, config().targetRange());
        if (distanceSq > targetRange * targetRange) {
            return null;
        }

        return new TargetCandidate(livePlayerRef.getUuid(), playerEntityRef, targetPos, distanceSq);
    }

    private static void applyBotDamageToPlayer(DeathmatchBot bot,
                                               Ref<EntityStore> targetRef,
                                               ComponentAccessor<EntityStore> accessor,
                                               int damageAmount) {
        if (damageAmount <= 0 || bot == null || targetRef == null || !targetRef.isValid()) {
            return;
        }

        Ref<EntityStore> sourceRef = bot.entityRef;
        if (sourceRef == null || !sourceRef.isValid()) {
            return;
        }

        DamageCause cause = resolveBotDamageCause();
        if (cause == null) {
            if (damageApiWarningLogged.compareAndSet(false, true)) {
                LOGGER.log(Level.WARNING, "[DeathmatchBot] Bot damage API missing DamageCause 'Projectile'.");
            }
            return;
        }

        try {
            Damage damage = new Damage(new Damage.EntitySource(sourceRef), cause, (float) damageAmount);
            DamageSystems.executeDamage(targetRef, accessor, damage);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[DeathmatchBot] Failed applying bot damage: " + e.getMessage(), e);
        }
    }

    private static void scheduleRespawn(GameMatch match, DeathmatchBot bot) {
        if (bot == null) {
            return;
        }
        if (!isMatchAvailableForBots(match, bot.matchId)) {
            bot.respawnAtMs = 0L;
            return;
        }

        long delayMs = Math.max(1000L, config().respawnDelayMs());
        bot.respawnAtMs = System.currentTimeMillis() + delayMs;
        debugLog("[DeathmatchBot] Scheduled bot respawn in " + delayMs + "ms for " + bot.displayName);
        ensureTickerRunning();
    }

    private static void respawnBot(GameMatch match, DeathmatchBot bot) {
        if (bot == null || !isMatchAvailableForBots(match, bot.matchId)) {
            if (bot != null) {
                bot.respawnAtMs = 0L;
            }
            return;
        }

        World world = resolveMatchWorld(match, List.of(bot));
        if (world == null) {
            return;
        }

        Store<EntityStore> store = world.getEntityStore().getStore();
        ArrayList<Vector3d> spawns;
        try {
            spawns = RefactorTool.getSpawns(match.getMapId(), MapListeners.SpawnMode.DM);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[DeathmatchBot] Respawn failed: unable to load DM spawns for map=" + match.getMapId(), e);
            bot.respawnAtMs = System.currentTimeMillis() + Math.max(1000L, config().respawnDelayMs());
            return;
        }

        if (spawns == null || spawns.isEmpty()) {
            bot.respawnAtMs = System.currentTimeMillis() + Math.max(1000L, config().respawnDelayMs());
            return;
        }

        Vector3d spawn = pickRespawnPosition(match, spawns, store);
        Vector3f rotation = new Vector3f(0f, 0f, 0f);

        Ref<EntityStore> newRef;
        try {
            newRef = spawnRole(store, bot.roleId, spawn, rotation);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[DeathmatchBot] Respawn failed for role '" + bot.roleId + "'", e);
            bot.respawnAtMs = System.currentTimeMillis() + Math.max(1000L, config().respawnDelayMs());
            return;
        }

        applyBotDisplayName(store, newRef, bot.displayName);
        equipBotWeapon(store, newRef, bot.weaponId);

        Ref<EntityStore> oldRef = bot.entityRef;
        if (oldRef != null) {
            unregisterBotRef(oldRef);
            removePendingFireRequestsForBot(oldRef);
        }

        bot.entityRef = newRef;
        bot.alive = true;
        bot.nextAttackAtMs = nowWithAttackJitter();
        bot.nextMovementAtMs = nowWithMovementJitter();
        bot.respawnAtMs = 0L;
        bot.nextRepositionAtMs = 0L;
        bot.targetPlayerUuid = null;
        bot.targetLockUntilMs = 0L;
        bot.lastKnownSpawn = new Vector3d(spawn);
        bot.lastTargetPosition = null;
        bot.failedMovementAttempts = 0;
        bot.lastMovementObservedPosition = new Vector3d(spawn);
        bot.lastMovementObservedAtMs = System.currentTimeMillis();

        registerBotRef(bot, newRef);
    }

    private static boolean hasActiveBots() {
        for (List<DeathmatchBot> bots : BOTS_BY_MATCH.values()) {
            if (bots != null && !bots.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void stopTickerIfIdle() {
        synchronized (TICKER_LOCK) {
            if (botTickerTask == null) {
                return;
            }
            if (hasActiveBots()) {
                return;
            }

            botTickerTask.cancel(false);
            botTickerTask = null;
            debugLog("[DeathmatchBot] Bot ticker stopped (idle).");
        }
    }

    private static void stopTicker() {
        synchronized (TICKER_LOCK) {
            if (botTickerTask != null) {
                botTickerTask.cancel(false);
            }
            botTickerTask = null;
            debugLog("[DeathmatchBot] Bot ticker stopped.");
        }
    }

    private static void registerBotRef(DeathmatchBot bot, Ref<EntityStore> ref) {
        if (bot == null || ref == null) {
            return;
        }
        BOT_REFS.add(ref);
        BOT_BY_REF.put(ref, bot);
    }

    private static void unregisterBotRef(Ref<EntityStore> ref) {
        if (ref == null) {
            return;
        }
        BOT_REFS.remove(ref);
        BOT_BY_REF.remove(ref);
    }

    private static void applyKillerCredit(GameMatch match, DeathmatchBot victimBot, Ref<EntityStore> killerRef) {
        if (match == null || killerRef == null || !killerRef.isValid()) {
            return;
        }

        if (isBot(killerRef)) {
            DeathmatchBot killerBot = BOT_BY_REF.get(killerRef);
            if (killerBot != null && killerBot != victimBot) {
                killerBot.kills++;
                killerBot.score += 15;
                RefactorTool.setChangesInUI(match);
            }
            return;
        }

        Store<EntityStore> store = killerRef.getStore();
        if (store == null) {
            return;
        }

        Player killer = store.getComponent(killerRef, Player.getComponentType());
        if (killer == null) {
            return;
        }

        PlayerRef killerPlayerRef = Universe.get().getPlayerByUsername(killer.getDisplayName(), NameMatching.EXACT);
        if (killerPlayerRef == null) {
            return;
        }

        PlayerStats killerStats = RefactorTool.getPlayerStats(killerPlayerRef);
        if (killerStats == null || killerStats.getCurrentMatch() == null) {
            return;
        }
        if (!match.getMatchId().equals(killerStats.getCurrentMatch().getMatchId())) {
            return;
        }

        killerStats.setKills();
        RefactorTool.setChangesInUI(match);
    }

    private static DamageCause resolveBotDamageCause() {
        DamageCause cached = cachedBotDamageCause;
        if (cached != null) {
            return cached;
        }

        DamageCause cause = DamageCause.getAssetMap().getAsset("Projectile");
        if (cause == null) {
            cause = DamageCause.PROJECTILE;
        }
        if (cause != null) {
            cachedBotDamageCause = cause;
        }

        return cause;
    }

    private static World resolveMatchWorld(GameMatch match, List<DeathmatchBot> bots) {
        if (match != null) {
            for (PlayerRef playerRef : match.getPlayers()) {
                if (playerRef == null || playerRef.getUuid() == null) {
                    continue;
                }

                PlayerRef livePlayerRef = Universe.get().getPlayer(playerRef.getUuid());
                if (livePlayerRef == null || livePlayerRef.getWorldUuid() == null) {
                    continue;
                }

                World world = Universe.get().getWorld(livePlayerRef.getWorldUuid());
                if (world != null) {
                    return world;
                }
            }
        }

        if (bots == null) {
            return null;
        }

        for (DeathmatchBot bot : bots) {
            if (bot == null || bot.entityRef == null || !bot.entityRef.isValid()) {
                continue;
            }

            Store<EntityStore> store = bot.entityRef.getStore();
            if (store == null || store.getExternalData() == null) {
                continue;
            }

            World world = store.getExternalData().getWorld();
            if (world != null) {
                return world;
            }
        }

        return null;
    }

    private static boolean isMatchAvailableForBots(GameMatch match, UUID matchId) {
        if (match == null || matchId == null) {
            return false;
        }
        if (!isBotsEnabled()) {
            return false;
        }
        if (!MATCHES_BY_ID.containsKey(matchId)) {
            return false;
        }
        if (!isDeathmatch(match)) {
            return false;
        }
        if (match.getState() == GameMatch.MatchState.FINISHED) {
            return false;
        }

        return !match.getPlayers().isEmpty();
    }

    private static boolean isDeathmatch(GameMatch match) {
        return match != null && match.getMode() != null && match.getMode().equalsIgnoreCase("dm");
    }

    private static boolean isAkWeapon(String weaponId) {
        return weaponId != null && weaponId.equalsIgnoreCase(T_BOT_WEAPON_ID);
    }

    private static String resolveWeaponDisplayName(String weaponId) {
        return isAkWeapon(weaponId)
                ? T_BOT_WEAPON_NAME
                : CT_BOT_WEAPON_NAME;
    }

    private static BotWeaponTuning resolveWeaponTuning(String weaponId) {
        DeathmatchBotConfig cfg = config();
        String weaponKey = (weaponId != null && !weaponId.isBlank())
                ? weaponId
                : CT_BOT_WEAPON_ID;

        int baseDamage = isAkWeapon(weaponId)
                ? cfg.akDamage()
                : cfg.m4Damage();
        long baseCooldownMs = isAkWeapon(weaponId)
                ? cfg.akCooldownMs()
                : cfg.m4CooldownMs();
        double spread = 0.0d;
        int projectileCount = 1;
        String projectileConfigId = DEFAULT_PROJECTILE_CONFIG_ID;
        String projectileId = null;
        boolean usedGunDamage = false;

        if (cfg.useGunConfig()) {
            try {
                GunSettings settings = WeaponContentApi.getSettings(weaponKey);
                if (settings == null) {
                    if (missingWeaponConfigWarnings.add(weaponKey)) {
                        LOGGER.log(Level.WARNING, "[DeathmatchBot] Gun config not found for weaponId=" + weaponKey
                                + ". Using fallback bot weapon values.");
                    }
                } else {
                    WeaponProjectileSettings projectileSettings = settings.projectiles();
                    if (projectileSettings != null) {
                        Integer damage = projectileSettings.damage();
                        if (damage != null && damage > 0) {
                            baseDamage = damage;
                            usedGunDamage = true;
                        }

                        Double projectileSpread = projectileSettings.spread();
                        if (projectileSpread != null && projectileSpread >= 0.0d) {
                            spread = projectileSpread;
                        }

                        Integer projectileAmount = projectileSettings.count();
                        if (projectileAmount != null && projectileAmount > 0) {
                            projectileCount = clampInt(projectileAmount, 1, 12);
                        }

                        if (projectileSettings.configId() != null && !projectileSettings.configId().isBlank()) {
                            projectileConfigId = projectileSettings.configId().trim();
                        }
                        if (projectileSettings.projectileId() != null && !projectileSettings.projectileId().isBlank()) {
                            projectileId = projectileSettings.projectileId().trim();
                        }
                    }

                    WeaponFireSettings fireSettings = settings.fire();
                    if (fireSettings != null) {
                        Double cooldownSec = fireSettings.cooldown();
                        if (cooldownSec != null && cooldownSec > 0.0d) {
                            baseCooldownMs = Math.max(50L, Math.round(cooldownSec * 1000.0d));
                        }
                    }
                }
            } catch (Exception e) {
                String warningKey = weaponKey + ":" + e.getClass().getName();
                if (weaponConfigErrorWarnings.add(warningKey)) {
                    LOGGER.log(Level.WARNING, "[DeathmatchBot] Failed reading gun config for weaponId=" + weaponKey
                            + ". Using fallback bot weapon values.", e);
                }
            }
        }

        int minDamage = clampInt(cfg.minBotDamage(), 1, 200);
        int maxDamage = clampInt(cfg.maxBotDamage(), minDamage, 200);
        double damageMultiplier = usedGunDamage ? 1.0d : clampDouble(cfg.botDamageMultiplier(), 0.05d, 2.0d);
        int finalDamage = clampInt((int) Math.round(baseDamage * damageMultiplier), minDamage, Math.max(maxDamage, baseDamage));
        long finalCooldownMs = Math.max(50L, baseCooldownMs);
        double finalSpread = clampDouble(spread, 0.0d, 0.35d);
        return new BotWeaponTuning(finalDamage, finalCooldownMs, finalSpread, projectileCount, projectileConfigId, projectileId);
    }

    private static void playBotFireSound(DeathmatchBot bot, Store<EntityStore> store, Vector3d botPosition) {
        if (bot == null || store == null || botPosition == null) {
            return;
        }
        if (!config().botFireSound()) {
            return;
        }

        String eventId = isAkWeapon(bot.weaponId)
                ? WEAPON_AK47_FIRE_SOUND_ID
                : WEAPON_M4A1S_FIRE_SOUND_ID;
        int soundIndex = resolveSoundEventIndex(eventId);
        if (soundIndex < 0) {
            return;
        }

        try {
            SoundUtil.playSoundEvent3d(soundIndex, SoundCategory.SFX, botPosition, store);
        } catch (Exception soundError) {
            if (weaponSoundWarningLogged.compareAndSet(false, true)) {
                LOGGER.log(Level.WARNING, "[DeathmatchBot] Failed to play bot fire sound event '" + eventId + "'.", soundError);
            }
        }
    }

    private static int resolveSoundEventIndex(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return -1;
        }

        Integer cached = soundEventIndexCache.get(eventId);
        if (cached != null) {
            return cached;
        }

        int index = -1;
        try {
            index = SoundEvent.getAssetMap().getIndex(eventId);
        } catch (Exception soundError) {
            if (weaponSoundWarningLogged.compareAndSet(false, true)) {
                LOGGER.log(Level.WARNING, "[DeathmatchBot] Failed to resolve sound event index for '" + eventId + "'.", soundError);
            }
            return -1;
        }

        if (index < 0) {
            if (missingSoundWarnings.add(eventId)) {
                LOGGER.log(Level.WARNING, "[DeathmatchBot] Sound event missing for bot fire: " + eventId);
            }
            return -1;
        }

        soundEventIndexCache.put(eventId, index);
        return index;
    }

    private static ProjectileFireAttempt tryFireBotProjectile(DeathmatchBot bot,
                                                              Ref<EntityStore> targetRef,
                                                              Store<EntityStore> store,
                                                              Vector3d fromPosition,
                                                              Vector3d toPosition,
                                                              BotWeaponTuning tuning) {
        if (!config().botUseRealProjectiles()) {
            return ProjectileFireAttempt.SKIPPED;
        }

        if (bot == null || targetRef == null || store == null || tuning == null || fromPosition == null || toPosition == null) {
            debugLogEvery("fire:invalid", DEBUG_FIRE_LOG_RATE_MS,
                    "[DeathmatchBot][debug] Projectile fire skipped due to missing references.");
            return ProjectileFireAttempt.FAILED;
        }

        Ref<EntityStore> botRef = bot.entityRef;
        if (botRef == null || !botRef.isValid()) {
            return ProjectileFireAttempt.FAILED;
        }

        UUID worldUuid = getWorldUuidFromStore(store);
        if (worldUuid == null) {
            return ProjectileFireAttempt.FAILED;
        }

        DeathmatchBotConfig cfg = config();
        Vector3d targetCenter = new Vector3d(toPosition).add(0.0d, BOT_PROJECTILE_TARGET_HEIGHT_OFFSET, 0.0d);
        Vector3d direction = new Vector3d(targetCenter).subtract(fromPosition);
        double directionLength = direction.length();
        if (directionLength < 1.0E-6d) {
            return ProjectileFireAttempt.FAILED;
        }
        direction.scale(1.0d / directionLength);

        double eyeHeight = clampDouble(cfg.botProjectileEyeHeight(), 0.5d, 2.5d);
        double forwardOffset = clampDouble(cfg.botProjectileForwardOffset(), 0.0d, 2.0d);
        Vector3d origin = new Vector3d(fromPosition)
                .add(0.0d, eyeHeight, 0.0d)
                .add(direction.x * forwardOffset, direction.y * forwardOffset, direction.z * forwardOffset);

        BotFireRequest request = new BotFireRequest(
                bot.matchId,
                worldUuid,
                botRef,
                targetRef,
                bot.weaponId,
                tuning.damage(),
                tuning.projectileCount(),
                tuning.spread(),
                tuning.projectileConfigId(),
                tuning.projectileId(),
                origin,
                direction,
                System.currentTimeMillis()
        );

        if (!enqueueFireRequest(request)) {
            return ProjectileFireAttempt.FAILED;
        }

        debugLogEvery(
                "fire:queue:" + bot.displayName,
                DEBUG_FIRE_LOG_RATE_MS,
                "[DeathmatchBot][debug] Queued projectile fire for " + bot.displayName
                        + " origin=" + formatVector(origin)
                        + " direction=" + formatVector(direction)
                        + " projectileConfigId=" + tuning.projectileConfigId()
                        + " projectileId=" + tuning.projectileId()
                        + " damage=" + tuning.damage()
                        + " spread=" + tuning.spread()
                        + " count=" + tuning.projectileCount()
        );
        return ProjectileFireAttempt.QUEUED;
    }

    static boolean hasPendingProjectileRequests(Store<EntityStore> store) {
        UUID worldUuid = getWorldUuidFromStore(store);
        if (worldUuid == null) {
            return false;
        }

        ConcurrentLinkedQueue<BotFireRequest> queue = BOT_FIRE_REQUESTS_BY_WORLD.get(worldUuid);
        return queue != null && !queue.isEmpty();
    }

    static void processQueuedProjectileRequests(Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        if (store == null || commandBuffer == null) {
            return;
        }

        UUID worldUuid = getWorldUuidFromStore(store);
        if (worldUuid == null) {
            return;
        }

        ConcurrentLinkedQueue<BotFireRequest> queue = BOT_FIRE_REQUESTS_BY_WORLD.get(worldUuid);
        if (queue == null || queue.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        int processed = 0;
        while (processed < MAX_BOT_FIRE_REQUESTS_PER_TICK) {
            BotFireRequest request = queue.poll();
            if (request == null) {
                break;
            }
            decrementFireRequestCount();
            processed++;

            if (now - request.createdAtMs() > BOT_FIRE_REQUEST_TTL_MS) {
                continue;
            }

            GameMatch match = MATCHES_BY_ID.get(request.matchId());
            if (!isMatchAvailableForBots(match, request.matchId())) {
                continue;
            }

            DeathmatchBot bot = BOT_BY_REF.get(request.botRef());
            if (bot == null || !bot.alive) {
                continue;
            }
            if (request.targetRef() == null || !request.targetRef().isValid()) {
                continue;
            }
            if (request.botRef() == null || !request.botRef().isValid()) {
                continue;
            }

            List<Ref<EntityStore>> spawned;
            try {
                if (request.projectileId() != null && !request.projectileId().isBlank()) {
                    if (projectileModeWarningLogged.compareAndSet(false, true)) {
                        LOGGER.log(Level.WARNING, "[DeathmatchBot] Bot weapon projectileId mode is not supported yet; using projectileConfig path.");
                    }
                }

                spawned = ShootProjectile.shootBulletsFrom(
                        request.projectileCount(),
                        request.damage(),
                        request.spread(),
                        request.projectileConfigId(),
                        null,
                        BOT_HIT_DAMAGE_MODIFIERS,
                        false,
                        BOT_WALL_PENETRATION_SETTINGS,
                        BOT_AUTO_GUIDANCE_SETTINGS,
                        request.botRef(),
                        commandBuffer,
                        request.origin(),
                        request.direction()
                );
            } catch (Exception fireError) {
                spawned = List.of();
                if (projectileSpawnFailureWarningLogged.compareAndSet(false, true)) {
                    LOGGER.log(Level.WARNING, "[DeathmatchBot] Bot projectile fire failed. No direct bot damage will be applied.", fireError);
                }
            }

            boolean fired = spawned != null && !spawned.isEmpty();
            debugLogEvery(
                    "fire:result:" + bot.displayName,
                    DEBUG_FIRE_LOG_RATE_MS,
                    "[DeathmatchBot][debug] Projectile fire result for " + bot.displayName + ": "
                            + (fired ? "success" : "failed")
                            + ", spawned=" + (spawned == null ? 0 : spawned.size())
            );
            if (!fired) {
                debugLogEvery("fire:no-projectile:" + bot.displayName, DEBUG_FIRE_LOG_RATE_MS,
                        "[DeathmatchBot][debug] Projectile fire failed for " + bot.displayName + "; no direct damage was applied.");
            }
        }

        if (queue.isEmpty()) {
            BOT_FIRE_REQUESTS_BY_WORLD.remove(worldUuid, queue);
        }
    }

    private static boolean enqueueFireRequest(BotFireRequest request) {
        if (request == null || request.worldUuid() == null) {
            return false;
        }

        if (BOT_FIRE_REQUEST_COUNT.get() >= MAX_PENDING_BOT_FIRE_REQUESTS) {
            if (projectileQueueOverflowWarningLogged.compareAndSet(false, true)) {
                LOGGER.log(Level.WARNING, "[DeathmatchBot] Projectile queue is full. Dropping bot projectile requests.");
            }
            return false;
        }

        BOT_FIRE_REQUESTS_BY_WORLD
                .computeIfAbsent(request.worldUuid(), ignored -> new ConcurrentLinkedQueue<>())
                .offer(request);
        BOT_FIRE_REQUEST_COUNT.incrementAndGet();
        return true;
    }

    private static void decrementFireRequestCount() {
        int remaining = BOT_FIRE_REQUEST_COUNT.decrementAndGet();
        if (remaining < 0) {
            BOT_FIRE_REQUEST_COUNT.set(0);
        }
    }

    private static void clearPendingFireRequests() {
        BOT_FIRE_REQUESTS_BY_WORLD.clear();
        BOT_FIRE_REQUEST_COUNT.set(0);
    }

    private static void removePendingFireRequestsForMatch(UUID matchId) {
        if (matchId == null) {
            return;
        }

        for (ConcurrentLinkedQueue<BotFireRequest> queue : BOT_FIRE_REQUESTS_BY_WORLD.values()) {
            if (queue == null || queue.isEmpty()) {
                continue;
            }
            queue.removeIf(request -> request != null && matchId.equals(request.matchId()));
        }
        recalculateFireRequestCount();
    }

    private static void removePendingFireRequestsForBot(Ref<EntityStore> botRef) {
        if (botRef == null) {
            return;
        }

        for (ConcurrentLinkedQueue<BotFireRequest> queue : BOT_FIRE_REQUESTS_BY_WORLD.values()) {
            if (queue == null || queue.isEmpty()) {
                continue;
            }
            queue.removeIf(request -> request != null && Objects.equals(request.botRef(), botRef));
        }
        recalculateFireRequestCount();
    }

    private static void recalculateFireRequestCount() {
        int total = 0;
        for (ConcurrentLinkedQueue<BotFireRequest> queue : BOT_FIRE_REQUESTS_BY_WORLD.values()) {
            if (queue != null) {
                total += queue.size();
            }
        }
        BOT_FIRE_REQUEST_COUNT.set(Math.max(0, total));
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void updateBotMovement(GameMatch match,
                                          DeathmatchBot bot,
                                          Store<EntityStore> store,
                                          Vector3d botPosition,
                                          TargetCandidate target,
                                          long now) {
        if (match == null || bot == null || store == null || target == null) {
            return;
        }
        if (now < bot.nextMovementAtMs) {
            return;
        }

        DeathmatchBotConfig cfg = config();
        bot.nextMovementAtMs = now + Math.max(250L, cfg.movementUpdateMs());
        if (!isMatchAvailableForBots(match, bot.matchId)) {
            return;
        }

        Ref<EntityStore> botRef = bot.entityRef;
        if (botRef == null || !botRef.isValid()) {
            return;
        }

        NPCEntity npc = store.getComponent(botRef, NPCEntity.getComponentType());
        if (npc == null || npc.getPathManager() == null) {
            return;
        }

        Vector3d targetPos = target.position;
        double distance = Math.sqrt(target.distanceSq);
        if (distance <= cfg.desiredRangeMax() && distance >= cfg.desiredRangeMin()) {
            return;
        }
        if (distance > cfg.chaseRange()) {
            return;
        }

        Vector3d directionToTarget = new Vector3d(targetPos).subtract(botPosition);
        double directionLength = directionToTarget.length();
        if (directionLength < 1.0E-6d) {
            return;
        }
        directionToTarget.scale(1.0d / directionLength);

        Vector3d desiredPosition = null;
        if (distance > cfg.desiredRangeMax()) {
            double offset = Math.max(1.0d, distance - cfg.desiredRangeMax());
            desiredPosition = new Vector3d(botPosition).add(
                    directionToTarget.x * offset,
                    0.0d,
                    directionToTarget.z * offset
            );
        } else if (distance < cfg.tooCloseRange() && now >= bot.nextRepositionAtMs) {
            double offset = Math.max(1.0d, cfg.desiredRangeMin() - distance);
            desiredPosition = new Vector3d(botPosition).add(
                    -directionToTarget.x * offset,
                    0.0d,
                    -directionToTarget.z * offset
            );
            bot.nextRepositionAtMs = now + Math.max(500L, cfg.repositionCooldownMs());
        }

        if (desiredPosition == null) {
            return;
        }

        // Keep bot movement on the same Y plane as current position for simple DM repositioning.
        desiredPosition.y = botPosition.y;
        if (Double.isNaN(desiredPosition.x) || Double.isNaN(desiredPosition.y) || Double.isNaN(desiredPosition.z)) {
            return;
        }
        if (distanceSq(botPosition, desiredPosition) <= 0.0625d) {
            return;
        }

        try {
            if (bot.lastMovementObservedPosition != null
                    && now - bot.lastMovementObservedAtMs >= Math.max(1000L, cfg.movementUpdateMs())) {
                double movedSq = distanceSq(botPosition, bot.lastMovementObservedPosition);
                if (movedSq <= 0.16d) {
                    debugLogEvery(
                            "move:stalled:" + bot.displayName,
                            1500L,
                            "[DeathmatchBot][debug] Path command previously succeeded but movement was not observed for "
                                    + bot.displayName
                    );
                }
            }

            debugLogEvery(
                    "move:cmd:" + bot.displayName,
                    DEBUG_MOVE_LOG_RATE_MS,
                    "[DeathmatchBot][debug] Move command for " + bot.displayName
                            + " botPos=" + formatVector(botPosition)
                            + " targetPos=" + formatVector(targetPos)
                            + " desiredPos=" + formatVector(desiredPosition)
                            + " distance=" + String.format("%.2f", distance)
            );

            WorldPath transientPath = new WorldPath(
                    "dm_bot_move_" + bot.matchId + "_" + bot.displayName.replace(' ', '_') + "_" + now,
                    List.of(new Transform(new Vector3d(botPosition)), new Transform(new Vector3d(desiredPosition)))
            );
            npc.getPathManager().setTransientPath(transientPath);
            boolean followingPath = npc.getPathManager().isFollowingPath();
            if (!followingPath) {
                applyDirectMovementStep(store, botRef, botPosition, desiredPosition, cfg);
            }

            bot.lastMovementObservedPosition = new Vector3d(botPosition);
            bot.lastMovementObservedAtMs = now;
            bot.failedMovementAttempts = 0;
            debugLogEvery(
                    "move:ok:" + bot.displayName,
                    DEBUG_MOVE_LOG_RATE_MS,
                    "[DeathmatchBot][debug] setTransientPath succeeded for " + bot.displayName
                            + ", followingPath=" + followingPath
            );
        } catch (Exception movementError) {
            bot.failedMovementAttempts++;

            if (BOT_MAX_TELEPORT_REPOSITION_DISTANCE > 0.0d) {
                double teleportDistance = Math.sqrt(distanceSq(botPosition, desiredPosition));
                if (teleportDistance <= BOT_MAX_TELEPORT_REPOSITION_DISTANCE) {
                    tryTeleportReposition(store, botRef, desiredPosition);
                }
            }

            if (movementWarningLogged.compareAndSet(false, true)) {
                LOGGER.log(Level.WARNING, "[DeathmatchBot] Movement/pathing update failed. Bots will continue combat without movement.", movementError);
            }
            debugLogEvery(
                    "move:fail:" + bot.displayName,
                    1500L,
                    "[DeathmatchBot][debug] setTransientPath failed for " + bot.displayName
                            + " botPos=" + formatVector(botPosition)
                            + " targetPos=" + formatVector(targetPos)
                            + " desiredPos=" + formatVector(desiredPosition)
                            + " attempts=" + bot.failedMovementAttempts
            );
        }
    }

    private static boolean applyDirectMovementStep(Store<EntityStore> store, Ref<EntityStore> botRef, Vector3d from, Vector3d to, DeathmatchBotConfig cfg) {
        if (store == null || botRef == null || from == null || to == null || cfg == null || !botRef.isValid()) {
            return false;
        }

        Vector3d direction = new Vector3d(to).subtract(from);
        direction.y = 0.0d;
        double length = direction.length();
        if (length < 1.0E-6d) {
            return false;
        }

        double maxStep = Math.max(0.4d, Math.min(3.0d, cfg.movementUpdateMs() / 1000.0d * 4.0d));
        double step = Math.min(length, maxStep);
        direction.scale(step / length);

        Vector3d next = new Vector3d(from).add(direction.x, 0.0d, direction.z);
        try {
            TransformComponent transform = store.getComponent(botRef, TransformComponent.getComponentType());
            if (transform == null) {
                return false;
            }

            transform.teleportPosition(next);
            store.putComponent(botRef, TransformComponent.getComponentType(), transform);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void tryTeleportReposition(Store<EntityStore> store, Ref<EntityStore> botRef, Vector3d position) {
        if (store == null || botRef == null || position == null || !botRef.isValid()) {
            return;
        }

        try {
            TransformComponent transform = store.getComponent(botRef, TransformComponent.getComponentType());
            if (transform == null) {
                return;
            }

            transform.teleportPosition(new Vector3d(position));
            store.putComponent(botRef, TransformComponent.getComponentType(), transform);
        } catch (Exception ignored) {
            // Teleport reposition is optional and disabled by default.
        }
    }

    private static boolean faceTarget(Store<EntityStore> store,
                                      Ref<EntityStore> botRef,
                                      Vector3d botPosition,
                                      Vector3d targetPosition) {
        if (store == null || botRef == null || botPosition == null || targetPosition == null || !botRef.isValid()) {
            return false;
        }

        try {
            TransformComponent transform = store.getComponent(botRef, TransformComponent.getComponentType());
            if (transform == null) {
                return false;
            }

            double dx = targetPosition.x - botPosition.x;
            double dy = targetPosition.y - botPosition.y;
            double dz = targetPosition.z - botPosition.z;
            double horizontalDistance = Math.sqrt((dx * dx) + (dz * dz));
            if (horizontalDistance < 1.0E-6d) {
                return false;
            }

            DeathmatchBotConfig cfg = config();
            double yawOffsetRad = Math.toRadians(cfg.botYawOffsetDegrees());
            double pitchOffsetRad = Math.toRadians(cfg.botPitchOffsetDegrees());
            double yaw = Math.atan2(dx, dz) + yawOffsetRad;
            double pitch = Math.atan2(dy, horizontalDistance) + pitchOffsetRad;

            Vector3f newRotation = new Vector3f(transform.getRotation());
            newRotation.setYaw((float) yaw);
            newRotation.setPitch((float) pitch);
            newRotation.setRoll(0.0f);

            transform.setRotation(newRotation);
            store.putComponent(botRef, TransformComponent.getComponentType(), transform);
            debugLogEvery(
                    "face:ok:" + botRef,
                    DEBUG_FACE_LOG_RATE_MS,
                    "[DeathmatchBot][debug] Facing update succeeded for ref=" + botRef
                            + " botPos=" + formatVector(botPosition)
                            + " targetPos=" + formatVector(targetPosition)
                            + " yaw=" + String.format("%.3f", yaw)
                            + " pitch=" + String.format("%.3f", pitch)
            );
            return true;
        } catch (Exception rotationError) {
            if (rotationWarningLogged.compareAndSet(false, true)) {
                LOGGER.log(Level.WARNING, "[DeathmatchBot] Failed to update bot rotation/facing. Combat will continue.", rotationError);
            }
            debugLogEvery("face:fail", 1500L,
                    "[DeathmatchBot][debug] Facing update failed for bot ref=" + botRef);
            return false;
        }
    }

    private static Vector3d targetAimPosition(Vector3d targetPosition) {
        if (targetPosition == null) {
            return null;
        }
        return new Vector3d(targetPosition).add(0.0d, BOT_PROJECTILE_TARGET_HEIGHT_OFFSET, 0.0d);
    }


    private static boolean hasLineOfSight(Store<EntityStore> store, Vector3d from, Vector3d to) {
        if (store == null || from == null || to == null) {
            return true;
        }

        try {
            if (store.getExternalData() == null || store.getExternalData().getWorld() == null) {
                return true;
            }

            World world = store.getExternalData().getWorld();

            Vector3d delta = new Vector3d(to).subtract(from);
            double totalLength = delta.length();
            if (totalLength < 0.5d) {
                return true;
            }

            delta.scale(1.0d / totalLength);
            final double step = 0.35d;
            int steps = Math.max(1, (int) Math.ceil(totalLength / step));

            for (int i = 1; i < steps; i++) {
                double t = i * step;
                if (t >= totalLength - 0.4d) {
                    break;
                }

                double px = from.x + (delta.x * t);
                double py = from.y + (delta.y * t);
                double pz = from.z + (delta.z * t);

                int bx = (int) Math.floor(px);
                int by = (int) Math.floor(py);
                int bz = (int) Math.floor(pz);

                if (isSolidBlock(world, bx, by, bz)) {
                    return false;
                }
            }

            return true;
        } catch (Exception losError) {
            if (lineOfSightWarningLogged.compareAndSet(false, true)) {
                LOGGER.log(Level.WARNING, "[DeathmatchBot] LOS check failed; defaulting to fail-open.", losError);
            }
            return true;
        }
    }

    private static boolean isSolidBlock(World world, int x, int y, int z) {
        if (world == null) {
            return false;
        }

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

    private static double resolveHitChance(double distanceSq, double spread) {
        double baseChance;
        if (distanceSq <= BOT_CLOSE_RANGE * BOT_CLOSE_RANGE) {
            baseChance = config().hitChanceClose();
        } else if (distanceSq >= BOT_LONG_RANGE_START * BOT_LONG_RANGE_START) {
            baseChance = config().hitChanceLong();
        } else {
            baseChance = config().hitChanceMedium();
        }

        // Higher spread slightly lowers hit consistency for direct-damage approximation.
        double spreadPenalty = clampDouble(spread * 6.0d, 0.0d, 0.25d);
        double adjustedChance = baseChance - spreadPenalty;
        if (distanceSq <= BOT_CLOSE_RANGE * BOT_CLOSE_RANGE) {
            adjustedChance += 0.05d;
        }

        return clampDouble(adjustedChance, 0.05d, 0.95d);
    }

    private static long nowWithAttackJitter() {
        return System.currentTimeMillis()
                + ThreadLocalRandom.current().nextLong(BOT_INITIAL_ATTACK_JITTER_MIN_MS, BOT_INITIAL_ATTACK_JITTER_MAX_MS + 1L);
    }

    private static long nowWithMovementJitter() {
        long movementUpdateMs = Math.max(250L, config().movementUpdateMs());
        return System.currentTimeMillis()
                + ThreadLocalRandom.current().nextLong(100L, movementUpdateMs + 1L);
    }

    private static void restartTickerForConfig() {
        boolean hadRunningTicker = isTickerRunning();
        synchronized (TICKER_LOCK) {
            if (botTickerTask != null) {
                botTickerTask.cancel(false);
                botTickerTask = null;
            }
        }

        if (hadRunningTicker && hasActiveBots() && isBotsEnabled()) {
            ensureTickerRunning();
        }
    }

    private static DeathmatchBotConfig config() {
        DeathmatchBotConfig cfg = botConfig;
        return cfg != null ? cfg : DeathmatchBotConfig.defaults();
    }

    private static void debugLog(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        if (config().debugLogging()) {
            LOGGER.log(Level.INFO, message);
        }
    }

    private static void debugLogEvery(String key, long minIntervalMs, String message) {
        if (key == null || key.isBlank() || message == null || message.isBlank()) {
            return;
        }
        if (!config().debugLogging()) {
            return;
        }

        long now = System.currentTimeMillis();
        long interval = Math.max(100L, minIntervalMs);
        Long previous = DEBUG_LOG_TIMESTAMPS_MS.get(key);
        if (previous != null && now - previous < interval) {
            return;
        }
        DEBUG_LOG_TIMESTAMPS_MS.put(key, now);
        LOGGER.log(Level.INFO, message);
    }

    private static String formatVector(Vector3d vector) {
        if (vector == null) {
            return "null";
        }
        return String.format("(%.2f, %.2f, %.2f)", vector.x, vector.y, vector.z);
    }

    private static Vector3d pickRespawnPosition(GameMatch match, List<Vector3d> spawns, Store<EntityStore> store) {
        List<Vector3d> centeredSpawns = new ArrayList<>(spawns.size());
        for (Vector3d spawn : spawns) {
            if (spawn != null) {
                centeredSpawns.add(toCenteredSpawn(spawn));
            }
        }
        if (centeredSpawns.isEmpty()) {
            return new Vector3d(0d, 0d, 0d);
        }

        List<Vector3d> safeSpawns = new ArrayList<>();
        for (Vector3d spawn : centeredSpawns) {
            if (!isNearAlivePlayer(match, store, spawn, BOT_RESPAWN_PLAYER_AVOID_RANGE)) {
                safeSpawns.add(spawn);
            }
        }

        List<Vector3d> pool = safeSpawns.isEmpty() ? centeredSpawns : safeSpawns;
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    private static boolean isNearAlivePlayer(GameMatch match,
                                             Store<EntityStore> store,
                                             Vector3d position,
                                             double range) {
        double rangeSq = range * range;

        for (PlayerRef matchPlayerRef : match.getPlayers()) {
            if (matchPlayerRef == null || matchPlayerRef.getUuid() == null) {
                continue;
            }

            PlayerRef livePlayerRef = Universe.get().getPlayer(matchPlayerRef.getUuid());
            if (livePlayerRef == null) {
                continue;
            }

            PlayerStats stats = RefactorTool.getPlayerStats(livePlayerRef);
            if (stats == null || stats.playerState != PlayerStats.PlayerState.DEFAULT) {
                continue;
            }

            Player player = RefactorTool.getPlayer(livePlayerRef);
            if (player == null || player.getReference() == null) {
                continue;
            }

            TransformComponent transform = store.getComponent(player.getReference(), TransformComponent.getComponentType());
            if (transform == null) {
                continue;
            }

            if (distanceSq(position, transform.getPosition()) < rangeSq) {
                return true;
            }
        }

        return false;
    }

    private static UUID getWorldUuidFromStore(Store<EntityStore> store) {
        if (store == null || store.getExternalData() == null || store.getExternalData().getWorld() == null) {
            return null;
        }

        return store.getExternalData().getWorld().getWorldConfig().getUuid();
    }

    private static double distanceSq(Vector3d a, Vector3d b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        double dz = a.z - b.z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static Vector3d toCenteredSpawn(Vector3d spawn) {
        return new Vector3d(spawn.x + 0.5d, spawn.y, spawn.z + 0.5d);
    }

    private static void applyBotDisplayName(Store<EntityStore> store, Ref<EntityStore> botRef, String displayName) {
        if (store == null || botRef == null || !botRef.isValid() || displayName == null || displayName.isBlank()) {
            return;
        }

        try {
            Nameplate current = store.getComponent(botRef, Nameplate.getComponentType());
            if (current == null) {
                store.addComponent(botRef, Nameplate.getComponentType(), new Nameplate(displayName));
            } else {
                current.setText(displayName);
                store.putComponent(botRef, Nameplate.getComponentType(), current);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[DeathmatchBot] Failed to apply bot nameplate: " + e.getMessage(), e);
        }
    }

    private static void equipBotWeapon(Store<EntityStore> store, Ref<EntityStore> botRef, String weaponId) {
        if (store == null || botRef == null || !botRef.isValid() || weaponId == null || weaponId.isBlank()) {
            return;
        }

        try {
            NPCEntity npc = store.getComponent(botRef, NPCEntity.getComponentType());
            if (npc == null) {
                return;
            }

            Inventory inventory = npc.getInventory();
            if (inventory == null || inventory.getHotbar() == null) {
                return;
            }

            inventory.getHotbar().setItemStackForSlot((short) 0, new ItemStack(weaponId, 1));
            inventory.setActiveHotbarSlot(botRef, (byte) 0, store);
            npc.invalidateEquipmentNetwork();
        } catch (Exception equipError) {
            if (weaponEquipWarningLogged.compareAndSet(false, true)) {
                LOGGER.log(Level.WARNING, "[DeathmatchBot] Failed to equip NPC bot weapon. Bot will still use "
                        + resolveWeaponDisplayName(weaponId) + " config for combat.", equipError);
            }
        }
    }

    private static void removeBotEntity(Ref<EntityStore> botRef) {
        if (botRef == null) {
            return;
        }

        try {
            if (!botRef.isValid()) {
                return;
            }

            Store<EntityStore> store = botRef.getStore();
            if (store == null) {
                return;
            }

            Runnable removeAction = () -> {
                try {
                    if (botRef.isValid()) {
                        store.removeEntity(botRef, RemoveReason.REMOVE);
                    }
                } catch (Exception removeError) {
                    LOGGER.log(Level.WARNING, "[DeathmatchBot] Failed to remove bot entity: " + removeError.getMessage(), removeError);
                }
            };

            World botWorld = null;
            try {
                if (store.getExternalData() != null) {
                    botWorld = store.getExternalData().getWorld();
                }
            } catch (Exception ignored) {
                // fallback below
            }

            if (botWorld != null) {
                botWorld.execute(removeAction);
            } else {
                removeAction.run();
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[DeathmatchBot] Failed to remove bot ref: " + e.getMessage(), e);
        }
    }

    private static Ref<EntityStore> spawnRole(Store<EntityStore> store, String roleId, Vector3d spawn, Vector3f rotation) {
        Pair<Ref<EntityStore>, ?> result = NPCPlugin.get().spawnNPC(store, roleId, null, spawn, rotation);
        if (result == null || result.left() == null || !result.left().isValid()) {
            throw new IllegalStateException("spawnNPC returned invalid ref for role=" + roleId);
        }
        return result.left();
    }

    private enum ProjectileFireAttempt {
        SKIPPED,
        QUEUED,
        FAILED
    }

    private record BotFireRequest(UUID matchId,
                                  UUID worldUuid,
                                  Ref<EntityStore> botRef,
                                  Ref<EntityStore> targetRef,
                                  String weaponId,
                                  int damage,
                                  int projectileCount,
                                  double spread,
                                  String projectileConfigId,
                                  String projectileId,
                                  Vector3d origin,
                                  Vector3d direction,
                                  long createdAtMs) { }

    private record BotWeaponTuning(int damage,
                                   long cooldownMs,
                                   double spread,
                                   int projectileCount,
                                   String projectileConfigId,
                                   String projectileId) { }

    private record TargetCandidate(UUID playerUuid, Ref<EntityStore> playerRef, Vector3d position, double distanceSq) { }
}
