package Tenzinn.Core.Localization;

import com.hypixel.hytale.server.core.Message;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class Lang {

    private static final String PREFIX = "server.countertale.";
    private static final Properties STRINGS = loadStrings();

    private Lang() { }

    public static Message msg(String key) {
        String fullKey = PREFIX + key;
        return Message.raw(STRINGS.getProperty(fullKey, fullKey));
    }

    public static Message msg(String key, Object... params) {
        String fullKey = PREFIX + key;
        String text = STRINGS.getProperty(fullKey, fullKey);

        for (int i = 0; i + 1 < params.length; i += 2) {
            String name = String.valueOf(params[i]);
            Object value = params[i + 1];
            text = text.replace("{" + name + "}", String.valueOf(value));
        }

        return Message.raw(text);
    }

    private static Properties loadStrings() {
        Properties properties = new Properties();
        try (InputStream input = Lang.class.getClassLoader().getResourceAsStream("Server/Languages/en-US/server.lang")) {
            if (input != null) properties.load(new java.io.InputStreamReader(input, StandardCharsets.UTF_8));
        } catch (Exception ignored) { }

        return properties;
    }
}
