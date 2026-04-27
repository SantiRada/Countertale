package Tenzinn.Core.Storage;

import java.io.*;
import java.util.*;
import java.nio.file.*;
import com.google.gson.*;

public class HologramStorage {

    private static final String FILE_PATH = "plugins/OrbisOffensive/holograms.json";
    private static final HologramStorage INSTANCE = new HologramStorage();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private HologramStorage() {}

    public static HologramStorage getInstance() { return INSTANCE; }

    public void savePosition(String statueType, double x, double y, double z) {
        List<HologramEntry> entries = loadAll();
        // Replace entries of the same type
        entries.removeIf(e -> e.statueType.equals(statueType));
        entries.add(new HologramEntry(statueType, x, y, z));
        writeFile(entries);
    }

    public List<HologramEntry> loadAll() {
        try {
            File file = new File(FILE_PATH);
            if (!file.exists()) return new ArrayList<>();
            String content = new String(Files.readAllBytes(file.toPath()));
            HologramEntry[] arr = gson.fromJson(content, HologramEntry[].class);
            return arr != null ? new ArrayList<>(Arrays.asList(arr)) : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void clearAll() { writeFile(new ArrayList<>());    }

    private void writeFile(List<HologramEntry> entries) {
        try {
            File file = new File(FILE_PATH);
            file.getParentFile().mkdirs();
            Files.write(file.toPath(), gson.toJson(entries).getBytes());
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static class HologramEntry {
        public String statueType;
        public double x, y, z;

        public HologramEntry(String statueType, double x, double y, double z) {
            this.statueType = statueType;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
