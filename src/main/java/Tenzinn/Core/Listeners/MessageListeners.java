package Tenzinn.Core.Listeners;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MessageListeners {

    public enum MessageKey {

        // ─── CHAT ───────────────────────────────────────────────────────────────
        CHAT_WHEN_BUYING            ("chat.when-buying"),
        CHAT_BUYING_LATE            ("chat.buying-late"),
        CHAT_SHOP_IN_LOBBY          ("chat.shop-in-lobby"),
        CHAT_SHOP_IN_LOBBY_FVF      ("chat.shop-in-lobby-fvf"),
        CHAT_DAMAGE_LOBBY           ("chat.damage-lobby"),
        CHAT_IN_QUEUE_X2            ("chat.in-queue-x2"),
        CHAT_COMMAND_LOBBY_INLOBBY  ("chat.command-lobby-inlobby"),
        CHAT_BACK_TO_LOBBY          ("chat.back-to-lobby"),
        CHAT_COMMAND_LEAVE_NO_QUEUE ("chat.command-leave-without-queue"),
        CHAT_COMMAND_LEAVE_IN_GAME  ("chat.command-leave-in-game"),
        CHAT_LEAVE_QUEUE            ("chat.leave-queue"),
        CHAT_ALREADY_IN_GAME        ("chat.already-in-game"),
        CHAT_ADDED_QUEUE            ("chat.added-queue"),
        CHAT_STARTING_GAME          ("chat.starting-game"),
        CHAT_TELEPORTING_GAME       ("chat.teleporting-game"),
        // ─── UI ─────────────────────────────────────────────────────────────────
        UI_NOT_BUYING_PHASE         ("ui.not-buying-phase"),
        UI_LOOT_START_GAME          ("ui.loot-start-game"),
        UI_IN_BUYING_PHASE          ("ui.in-buying-phase"),
        UI_WHEN_RECEIVES_LOOT       ("ui.when-receives-loot"),
        UI_SELECT_LOOT              ("ui.select-loot"),
        UI_CLOSE_SHOP               ("ui.close-shop"),
        UI_BTN_LOBBY                ("ui.btn-lobby"),
        UI_BTN_PLAY                 ("ui.btn-play"),
        UI_TITLE_SUMMARY            ("ui.title-summary"),
        UI_DAMAGE_CAUSED            ("ui.damage-caused"),
        UI_DAMAGE_RECEIVED          ("ui.damage-received"),
        UI_MELEE_DAMAGE             ("ui.melee-damage"),
        UI_MESSAGE_COMMAND_LEAVE    ("ui.message-command-leave"),
        UI_TITLE_QUEUE    ("ui.title-queue"),
        UI_DESC_QUEUE    ("ui.desc-queue"),
        UI_TITLE_SHOP    ("ui.title-shop"),
        UI_DESC_SHOP    ("ui.desc-shop"),
        UI_TITLE_FVF    ("ui.title-fvf"),
        UI_DESC_FVF    ("ui.desc-fvf");
        // ────────────────────────────────────────────────────────────────────────

        private final String key;

        MessageKey(String key) { this.key = key; }
        public String getKey() { return key; }
        @Override public String toString() { return key; }
    }

    private static final Logger LOGGER = Logger.getLogger("Countertale");

    private static final String JSON_PATH = "Countertale/messages.json";
    private static final String MISSING_KEY_FORMAT = "[MISSING: %s]";

    private static final Map<String, String> MESSAGES = new HashMap<>();
    private static boolean loaded = false;

    private MessageListeners () { }
    public static boolean load() {
        try {
            File jar      = new File(MessageListeners.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File jsonFile = new File(jar.getParentFile(), JSON_PATH);
            return load(jsonFile);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[Countertale] No se pudo resolver la ruta del .jar.", e);
            return false;
        }
    }

    public static boolean load(File jsonFile) {
        MESSAGES.clear();
        loaded = false;

        if (!jsonFile.exists()) {
            LOGGER.warning("[Countertale] messages.json not found at: " + jsonFile.getAbsolutePath());
            return false;
        }

        try (FileReader reader = new FileReader(jsonFile)) {

            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            flatten(root, "");
            loaded = true;
            LOGGER.info("[Countertale] Loaded " + MESSAGES.size() + " messages from messages.json");

        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, "[Countertale] messages.json not found.", e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[Countertale] Failed to parse messages.json.", e);
        }

        return loaded;
    }

    private static void flatten(JsonObject obj, String prefix) {
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String fullKey = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            JsonElement value = entry.getValue();

            if (value.isJsonObject()) {
                flatten(value.getAsJsonObject(), fullKey);
            } else {
                MESSAGES.put(fullKey, value.getAsString());
            }
        }
    }

    public static String get(MessageKey key) { return get(key.getKey()); }

    public static String get(String key) {
        String value = MESSAGES.get(key);
        if (value == null) {
            LOGGER.warning("[Countertale] Missing message key: " + key);
            return String.format(MISSING_KEY_FORMAT, key);
        }
        return value;
    }

    public static int size() { return MESSAGES.size(); }
}