package Tenzinn.Core.Localization;

import com.hypixel.hytale.server.core.Message;

public final class Lang {

    private static final String PREFIX = "server.countertale.";

    private Lang() { }

    public static Message msg(String key) {
        return Message.translation(PREFIX + key);
    }

    public static Message msg(String key, Object... params) {
        Message message = msg(key);
        for (int i = 0; i + 1 < params.length; i += 2) {
            String name = String.valueOf(params[i]);
            Object value = params[i + 1];

            if (value instanceof Message nested) message.param(name, nested);
            else if (value instanceof Integer number) message.param(name, number);
            else if (value instanceof Long number) message.param(name, number);
            else if (value instanceof Float number) message.param(name, number);
            else if (value instanceof Double number) message.param(name, number);
            else if (value instanceof Boolean bool) message.param(name, bool);
            else message.param(name, String.valueOf(value));
        }
        return message;
    }
}
