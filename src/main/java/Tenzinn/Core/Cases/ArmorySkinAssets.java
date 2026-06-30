package Tenzinn.Core.Cases;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import java.awt.Color;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ArmorySkinAssets {

    private static final String SKINS_ROOT = "/Common/ArmorySkins/";
    private static final String SKIN_ICONS_UI = "Game/images/skins/SkinIcons.ui";

    private static final Map<String, String> SHOP_WEAPON_TO_ITEM_ID = Map.ofEntries(
            Map.entry("Ak47", "AK47"),
            Map.entry("DesertEagle", "DesertEagle"),
            Map.entry("M4a1s", "M4A1s"),
            Map.entry("Mac10", "Mac10"),
            Map.entry("Mp9", "MP9"),
            Map.entry("Usp", "Glock18")
    );

    private static final Set<String> missingIconWarnings = ConcurrentHashMap.newKeySet();
    private static final Set<String> missingTextureLogged = ConcurrentHashMap.newKeySet();

    private ArmorySkinAssets() {}

    public static boolean hasRequiredIcon(CaseSkin skin) {
        boolean exists = resourceExists(iconResourcePath(skin));
        if (!exists) {
            missingIconWarnings.add("[Armory] Missing icon.png for " + skin.weapon + "/" + skin.folder
                    + ". Skin not listed: " + skin.id);
        }
        return exists;
    }

    public static void announceMissingIconWarnings(PlayerRef ignored) {
        broadcastMissingIconWarnings();
    }

    public static void broadcastMissingIconWarnings() {
        if (missingIconWarnings.isEmpty()) return;
        for (PlayerRef playerRef : Universe.get().getPlayers()) {
            sendMissingIconWarnings(playerRef);
        }
    }

    private static void sendMissingIconWarnings(PlayerRef target) {
        for (String warning : missingIconWarnings) {
            target.sendMessage(Message.raw(warning).color(Color.RED));
        }
    }

    public static String skinIconUiRef() {
        return SKIN_ICONS_UI;
    }

    public static String iconResourcePath(CaseSkin skin) {
        return SKINS_ROOT + skin.weapon + "/" + skin.folder + "/icon.png";
    }

    public static String textureResourcePath(CaseSkin skin) {
        return SKINS_ROOT + skin.weapon + "/" + skin.folder + "/texture.png";
    }

    public static String modelResourcePath(CaseSkin skin) {
        return SKINS_ROOT + skin.weapon + "/" + skin.folder + "/" + skin.folder + ".blockymodel";
    }

    public static boolean hasTexture(CaseSkin skin) {
        return resourceExists(textureResourcePath(skin));
    }

    public static boolean hasModel(CaseSkin skin) {
        return resourceExists(modelResourcePath(skin));
    }

    public static String resolveItemId(UUID playerUuid, String baseItemId, String weaponId) {
        String selectedSkinId = CaseManager.getSelectedSkin(playerUuid, weaponId);
        if (selectedSkinId == null || selectedSkinId.isBlank()) return baseItemId;

        CaseSkin skin = CaseManager.getSkinById(selectedSkinId);
        if (skin == null) return baseItemId;

        if (!hasTexture(skin) && missingTextureLogged.add(skin.id)) {
            System.err.println("[Armory] Missing texture.png for selected skin " + skin.weapon + "/"
                    + skin.folder + ". Generated item falls back to the default weapon texture.");
        }

        return itemExists(skin.itemId) ? skin.itemId : baseItemId;
    }

    public static String defaultItemIdForWeapon(String weaponId) {
        return SHOP_WEAPON_TO_ITEM_ID.getOrDefault(weaponId, weaponId);
    }

    public static String weaponIdFromBaseItem(String baseItemId, String fallbackWeaponId) {
        for (Map.Entry<String, String> entry : SHOP_WEAPON_TO_ITEM_ID.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(baseItemId)) return entry.getKey();
        }
        return fallbackWeaponId;
    }

    private static boolean itemExists(String itemId) {
        try {
            return Item.getAssetMap().getAsset(itemId) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean resourceExists(String resourcePath) {
        return ArmorySkinAssets.class.getResource(resourcePath) != null;
    }
}
