package Tenzinn.Core.Cases;

import Tenzinn.Core.Storage.DatabaseManager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class CaseManager {

    public static final float DEFAULT_DROP_CHANCE = 0.30f;

    private static final Map<CaseSkin.Rarity, Integer> RARITY_WEIGHTS;
    private static final int TOTAL_RARITY_WEIGHT;

    public static final int STRIP_LENGTH = 26;
    public static final int WINNER_INDEX = 22;

    public static final List<CaseSkin> SKINS;

    private static float currentDropChance = DEFAULT_DROP_CHANCE;

    private static final Map<UUID, Integer> playerCases = new ConcurrentHashMap<>();

    private static final Map<UUID, List<CaseSkin>> playerInventory   = new ConcurrentHashMap<>();
    /** weaponId → skinId selection per player */
    private static final Map<UUID, Map<String, String>> selectedSkins = new ConcurrentHashMap<>();

    static {
        SKINS = loadSkins();
        RARITY_WEIGHTS = loadConfig();
        int total = 0;
        for (int w : RARITY_WEIGHTS.values()) total += w;
        TOTAL_RARITY_WEIGHT = total;
    }

    private static List<CaseSkin> loadSkins() {
        try (InputStream is = CaseManager.class.getResourceAsStream("/Common/UI/cases.json")) {
            if (is == null) {
                System.err.println("[CaseManager] cases.json not found.");
                return new ArrayList<>();
            }
            JsonArray arr = new Gson()
                    .fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class)
                    .getAsJsonArray("skins");
            List<CaseSkin> list = new ArrayList<>();
            for (JsonElement el : arr) {
                JsonObject s = el.getAsJsonObject();
                String folder = s.has("folder") ? s.get("folder").getAsString() : s.get("id").getAsString();
                CaseSkin skin = new CaseSkin(
                        s.get("id").getAsString(),
                        s.get("displayName").getAsString(),
                        s.get("weapon").getAsString(),
                        CaseSkin.Rarity.valueOf(s.get("rarity").getAsString()),
                        folder
                );
                if (ArmorySkinAssets.hasRequiredIcon(skin)) list.add(skin);
            }
            System.out.println("[CaseManager] Loaded " + list.size() + " skins.");
            return list;
        } catch (Exception e) {
            System.err.println("[CaseManager] Error loading cases.json: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private static Map<CaseSkin.Rarity, Integer> loadConfig() {
        Map<CaseSkin.Rarity, Integer> weights = new EnumMap<>(CaseSkin.Rarity.class);
        for (CaseSkin.Rarity r : CaseSkin.Rarity.values()) weights.put(r, r.weight);

        try (InputStream is = CaseManager.class.getResourceAsStream("/Common/UI/case_config.json")) {
            if (is == null) { System.err.println("[CaseManager] case_config.json not found; using defaults."); return weights; }
            JsonObject root = new Gson().fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);
            if (root.has("dropChance")) currentDropChance = root.get("dropChance").getAsFloat();
            if (root.has("rarityWeights")) {
                JsonObject rw = root.getAsJsonObject("rarityWeights");
                for (CaseSkin.Rarity r : CaseSkin.Rarity.values()) {
                    if (rw.has(r.name())) weights.put(r, rw.get(r.name()).getAsInt());
                }
            }
            System.out.println("[CaseManager] Config loaded. Drop chance: " + (int)(currentDropChance*100) + "%");
        } catch (Exception e) {
            System.err.println("[CaseManager] Error loading case_config.json: " + e.getMessage());
        }
        return weights;
    }

    public static boolean rollForCase() {
        return ThreadLocalRandom.current().nextFloat() < currentDropChance;
    }

    public static CaseSkin pickWinner() {
        CaseSkin.Rarity tier = rollRarity();
        List<CaseSkin> candidates = SKINS.stream()
                .filter(s -> s.rarity == tier).collect(Collectors.toList());
        if (candidates.isEmpty()) {
            candidates = SKINS.stream().filter(s -> s.rarity == CaseSkin.Rarity.MIL_SPEC).collect(Collectors.toList());
            if (candidates.isEmpty()) candidates = new ArrayList<>(SKINS);
        }
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    private static CaseSkin.Rarity rollRarity() {
        int roll = ThreadLocalRandom.current().nextInt(TOTAL_RARITY_WEIGHT);
        int cumulative = 0;
        for (CaseSkin.Rarity r : CaseSkin.Rarity.values()) {
            cumulative += RARITY_WEIGHTS.get(r);
            if (roll < cumulative) return r;
        }
        return CaseSkin.Rarity.MIL_SPEC;
    }

    public static List<CaseSkin> generateRoulette(CaseSkin winner) {
        List<CaseSkin> strip = new ArrayList<>(STRIP_LENGTH);
        List<CaseSkin> pool = SKINS.isEmpty() ? List.of(winner) : SKINS;
        for (int i = 0; i < STRIP_LENGTH; i++) {
            strip.add(i == WINNER_INDEX ? winner : pool.get(ThreadLocalRandom.current().nextInt(pool.size())));
        }
        return strip;
    }

    public static void addCase(UUID uuid) {
        playerCases.merge(uuid, 1, Integer::sum);
        DatabaseManager.addCases(uuid, 1);
    }

    public static int getCaseCount(UUID uuid) {
        return playerCases.getOrDefault(uuid, 0);
    }

    public static boolean useCase(UUID uuid) {
        AtomicBoolean used = new AtomicBoolean(false);
        playerCases.compute(uuid, (k, v) -> {
            if (v == null || v <= 0) return null;
            used.set(true);
            int next = v - 1;
            return next > 0 ? next : null;
        });
        if (used.get()) DatabaseManager.decrementCase(uuid);
        return used.get();
    }

    public static void clearCases(UUID uuid) {
        playerCases.remove(uuid);
        DatabaseManager.clearCases(uuid);
    }

    public static void addToInventory(UUID uuid, CaseSkin skin) {
        playerInventory.computeIfAbsent(uuid, k -> new ArrayList<>()).add(skin);
        DatabaseManager.addSkinToInventory(uuid, skin.id);
    }

    /**
     * Loads this player's skins from the database into the in-memory map.
     * Called once when the player enters the lobby.
     */
    public static void loadInventoryFromDb(UUID uuid) {
        DatabaseManager.loadSkinIds(uuid).thenAccept(ids -> {
            List<CaseSkin> skins = new ArrayList<>();
            for (String id : ids) {
                CaseSkin skin = getSkinById(id);
                if (skin != null) skins.add(skin);
            }
            playerInventory.put(uuid, skins);
        });
    }

    public static void loadCasesFromDb(UUID uuid) {
        DatabaseManager.loadCaseCount(uuid).thenAccept(count -> {
            if (count > 0) playerCases.put(uuid, count);
            else playerCases.remove(uuid);
        });
    }

    public static List<CaseSkin> getInventory(UUID uuid) {
        return playerInventory.getOrDefault(uuid, Collections.emptyList());
    }

    public static void clearInventory(UUID uuid) {
        playerInventory.remove(uuid);
        DatabaseManager.clearSkinInventory(uuid);
    }

    public static void setDropChance(float chance) { currentDropChance = Math.max(0f, Math.min(1f, chance)); }
    public static float getDropChance()            { return currentDropChance; }

    public static CaseSkin getSkinById(String id) {
        for (CaseSkin s : SKINS) if (s.id.equalsIgnoreCase(id)) return s;
        return null;
    }

    public static List<CaseSkin> getSkinsByWeapon(String weaponId) {
        return SKINS.stream()
                .filter(s -> s.weapon.equalsIgnoreCase(weaponId))
                .collect(Collectors.toList());
    }

    // ── Selected skins ────────────────────────────────────────────────────────

    public static void setSelectedSkin(UUID uuid, String weaponId, String skinId) {
        selectedSkins.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(weaponId, skinId);
        DatabaseManager.setSelectedSkin(uuid, weaponId, skinId);
    }

    public static void clearSelectedSkin(UUID uuid, String weaponId) {
        Map<String, String> map = selectedSkins.get(uuid);
        if (map != null) {
            map.remove(weaponId);
            if (map.isEmpty()) selectedSkins.remove(uuid);
        }
        DatabaseManager.clearSelectedSkin(uuid, weaponId);
    }

    public static String getSelectedSkin(UUID uuid, String weaponId) {
        Map<String, String> map = selectedSkins.get(uuid);
        return map != null ? map.get(weaponId) : null;
    }

    public static Map<String, String> getSelectedSkins(UUID uuid) {
        return selectedSkins.getOrDefault(uuid, Collections.emptyMap());
    }

    public static void loadSelectedSkinsFromDb(UUID uuid) {
        DatabaseManager.loadSelectedSkins(uuid).thenAccept(map ->
                selectedSkins.put(uuid, new ConcurrentHashMap<>(map)));
    }
}
