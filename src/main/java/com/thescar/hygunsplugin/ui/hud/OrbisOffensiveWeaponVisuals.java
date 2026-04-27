package com.thescar.hygunsplugin.ui.hud;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public final class OrbisOffensiveWeaponVisuals {

    public record Visual(String image, String crosshair, String firemode, boolean usesAmmo) {
    }

    private static final Map<String, Visual> VISUALS = new HashMap<>();

    static {
        put("Weapon_Glock18", "Glock18", "Weapon", "Single", true);
        put("Weapon_USPS", "USPS", "Weapon", "Single", true);
        put("Weapon_FiveSeven", "FiveSeven", "Weapon", "Single", true);
        put("Weapon_ColtRevolver", "ColtRevolver", "Weapon", "Single", true);
        put("Weapon_DesertEagle", "DesertEagle", "Weapon", "Single", true);

        put("Weapon_Mac10", "Mac10", "Weapon", "Automatic", true);
        put("Weapon_MP9", "Mp9", "Weapon", "Automatic", true);
        put("Weapon_P90", "P90", "Weapon", "Automatic", true);
        put("Weapon_Thompson", "Thompson", "Weapon", "Automatic", true);

        put("Weapon_AK47", "Ak47", "Weapon", "Automatic", true);
        put("Weapon_M4A1s", "M4a1s", "Weapon", "Automatic", true);

        put("Weapon_AWP", "Awp", "Weapon", "Single", true);
        put("Weapon_Barret50", "Barret50", "Weapon", "Single", true);
        put("Weapon_DoubleBarrel", "DoubleBarrel", "Shotgun", "Single", true);
        put("Weapon_Flamethrower", "Flamethrower", "Weapon", "Automatic", true);

        put("Weapon_Frag", "Frag", "Weapon", "Single", false);

        put("Knife", "Knife", "Knife", "Melee", false);
        put("Weapon_Daggers_Cobalt", "Knife", "Knife", "Melee", false);
    }

    private OrbisOffensiveWeaponVisuals() {
    }

    private static void put(String itemId, String image, String crosshair, String firemode, boolean usesAmmo) {
        VISUALS.put(itemId, new Visual(image, crosshair, firemode, usesAmmo));
    }

    @Nullable
    public static Visual resolve(@Nullable String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }

        String cleanId = cleanItemId(itemId);

        Visual visual = VISUALS.get(cleanId);
        if (visual != null) {
            return visual;
        }

        String lower = cleanId.toLowerCase();

        if (lower.contains("knife") || lower.contains("dagger")) {
            return VISUALS.get("Knife");
        }

        return null;
    }

    private static String cleanItemId(String itemId) {
        String clean = itemId.trim();

        int namespaceIndex = clean.lastIndexOf(':');
        if (namespaceIndex >= 0 && namespaceIndex < clean.length() - 1) {
            clean = clean.substring(namespaceIndex + 1);
        }

        return clean;
    }
}