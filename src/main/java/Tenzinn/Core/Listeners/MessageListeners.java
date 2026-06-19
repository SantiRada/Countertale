package Tenzinn.Core.Listeners;

import Tenzinn.Core.Localization.Lang;

import com.hypixel.hytale.server.core.Message;

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
        UI_DESC_SHOP     ("ui.desc-shop"),
        UI_TITLE_ARMORY  ("ui.title-armory"),
        UI_DESC_ARMORY   ("ui.desc-armory");
        // ────────────────────────────────────────────────────────────────────────

        private final String key;

        MessageKey(String key) { this.key = key; }
        public String getKey() { return key; }
        @Override public String toString() { return key; }
    }

    private MessageListeners () { }

    public static boolean load() { return true; }

    public static Message message(MessageKey key) { return Lang.msg(key.getKey()); }

    public static Message message(String key) { return Lang.msg(key); }

    public static String get(MessageKey key) { return "%" + "server.countertale." + key.getKey(); }

    public static String get(String key) { return "%" + "server.countertale." + key; }

    public static int size() { return MessageKey.values().length; }
}
