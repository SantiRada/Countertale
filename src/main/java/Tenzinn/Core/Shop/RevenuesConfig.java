package Tenzinn.Core.Shop;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Objects;

public class RevenuesConfig {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static String getConfigPath() {
        try {
            File jar = new File(RevenuesConfig.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());

            File configFile = new File(jar.getParentFile(), "OrbisOffensive/revenues.json");
            return configFile.getAbsolutePath();

        } catch (Exception e) {
            System.err.println("Error gives path: " + e.getMessage());
            return null;
        }
    }

    // 📖 Leer JSON
    public static JsonObject load() {
        try {
            Path path = Paths.get(getConfigPath());
            String content = new String(Files.readAllBytes(path));
            return gson.fromJson(content, JsonObject.class);

        } catch (Exception e) {
            System.err.println("Error reading JSON: " + e.getMessage());
            return null;
        }
    }

    // ✏️ Edit direct value within "revenues"
    public static void updateValue(String key, int newValue) {
        try {
            Path path = Paths.get(getConfigPath());
            String content = new String(Files.readAllBytes(path));

            JsonObject root = gson.fromJson(content, JsonObject.class);
            JsonObject revenues = root.getAsJsonObject("revenues");

            if (revenues == null) { System.err.println("Not exist 'revenues'"); return; }
            revenues.addProperty(key, newValue);

            Files.write(path, gson.toJson(root).getBytes());

        } catch (Exception e) {
            System.err.println("Error updating JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static ArrayList<String> getList() {
        ArrayList<String> allData = new ArrayList<>();

        try {
            Path path = Paths.get(Objects.requireNonNull(getConfigPath()));
            String content = new String(Files.readAllBytes(path));

            JsonObject root = gson.fromJson(content, JsonObject.class);
            JsonObject revenues = root.getAsJsonObject("revenues");

            if (revenues == null) { System.err.println("Not exist 'revenues'"); return new ArrayList<>(); }

            for (var entry : revenues.entrySet()) {
                String key = entry.getKey();
                JsonElement value = entry.getValue();

                allData.add(key + " = " + value.getAsInt());
            }
        } catch (Exception e) { throw new RuntimeException(e); }

        return allData;
    }

    public static int getValue(String key) {
        int value = -1;

        try {
            Path path = Paths.get(Objects.requireNonNull(getConfigPath()));
            String content = new String(Files.readAllBytes(path));

            JsonObject root = gson.fromJson(content, JsonObject.class);
            JsonObject revenues = root.getAsJsonObject("revenues");

            if (revenues == null) { System.err.println("Not exist 'revenues'"); return -1; }

            for (var entry : revenues.entrySet()) {
                if(entry.getKey().equals(key)) {
                    value = entry.getValue().getAsInt();
                    break;
                }
            }
        } catch (Exception e) { throw new RuntimeException(e); }

        return value;
    }
}