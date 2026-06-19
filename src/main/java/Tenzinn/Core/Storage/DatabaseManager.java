package Tenzinn.Core.Storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DatabaseManager {

    private static final String CONFIG_PATH = "plugins/Countertale/db_config.json";

    private static String jdbcUrl;
    private static String dbUser;
    private static String dbPassword;
    private static Connection connection;

    private static final ExecutorService DB_EXECUTOR = Executors.newSingleThreadExecutor();

    public static void init() {
        JsonObject config = loadOrCreateConfig();
        jdbcUrl = String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                config.get("host").getAsString(),
                config.get("port").getAsInt(),
                config.get("database").getAsString()
        );
        dbUser     = config.get("username").getAsString();
        dbPassword = config.get("password").getAsString();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
            createTables();
            System.out.println("[DatabaseManager] Connected to database.");
        } catch (Exception e) {
            System.err.println("[DatabaseManager] Could not connect: " + e.getMessage());
        }
    }

    public static void close() {
        DB_EXECUTOR.shutdown();
        try { DB_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        try { if (connection != null && !connection.isClosed()) connection.close(); } catch (SQLException ignored) {}
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private static synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try { Class.forName("com.mysql.cj.jdbc.Driver"); } catch (ClassNotFoundException ignored) {}
            connection = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
        }
        return connection;
    }

    private static void createTables() {
        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS players (
                    uuid       VARCHAR(36) PRIMARY KEY,
                    username   VARCHAR(16) NOT NULL,
                    first_seen TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
                    last_seen  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_skins (
                    id          INT AUTO_INCREMENT PRIMARY KEY,
                    player_uuid VARCHAR(36) NOT NULL,
                    skin_id     VARCHAR(64) NOT NULL,
                    obtained_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_player (player_uuid),
                    FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_selected_skins (
                    player_uuid VARCHAR(36) NOT NULL,
                    weapon_id   VARCHAR(32) NOT NULL,
                    skin_id     VARCHAR(64) NOT NULL,
                    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    PRIMARY KEY (player_uuid, weapon_id),
                    FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Error creating tables: " + e.getMessage());
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public static CompletableFuture<Void> registerPlayer(UUID uuid, String username) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = getConnection().prepareStatement(
                    "INSERT INTO players (uuid, username) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE username = VALUES(username), last_seen = CURRENT_TIMESTAMP")) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, username);
                stmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("[DatabaseManager] registerPlayer error: " + e.getMessage());
            }
        }, DB_EXECUTOR);
    }

    public static CompletableFuture<Void> addSkinToInventory(UUID uuid, String skinId) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = getConnection().prepareStatement(
                    "INSERT INTO player_skins (player_uuid, skin_id) VALUES (?, ?)")) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, skinId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("[DatabaseManager] addSkinToInventory error: " + e.getMessage());
            }
        }, DB_EXECUTOR);
    }

    /** Upserts the skin selected for a specific weapon. */
    public static CompletableFuture<Void> setSelectedSkin(UUID uuid, String weaponId, String skinId) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = getConnection().prepareStatement(
                    "INSERT INTO player_selected_skins (player_uuid, weapon_id, skin_id) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE skin_id = VALUES(skin_id), updated_at = CURRENT_TIMESTAMP")) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, weaponId);
                stmt.setString(3, skinId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("[DatabaseManager] setSelectedSkin error: " + e.getMessage());
            }
        }, DB_EXECUTOR);
    }

    /** Returns all weapon→skinId selections for a player. */
    public static CompletableFuture<Map<String, String>> loadSelectedSkins(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> map = new java.util.HashMap<>();
            try (PreparedStatement stmt = getConnection().prepareStatement(
                    "SELECT weapon_id, skin_id FROM player_selected_skins WHERE player_uuid = ?")) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) map.put(rs.getString("weapon_id"), rs.getString("skin_id"));
            } catch (SQLException e) {
                System.err.println("[DatabaseManager] loadSelectedSkins error: " + e.getMessage());
            }
            return map;
        }, DB_EXECUTOR);
    }

    public static CompletableFuture<List<String>> loadSkinIds(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> ids = new ArrayList<>();
            try (PreparedStatement stmt = getConnection().prepareStatement(
                    "SELECT skin_id FROM player_skins WHERE player_uuid = ? ORDER BY obtained_at ASC")) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) ids.add(rs.getString("skin_id"));
            } catch (SQLException e) {
                System.err.println("[DatabaseManager] loadSkinIds error: " + e.getMessage());
            }
            return ids;
        }, DB_EXECUTOR);
    }

    // ── Config ────────────────────────────────────────────────────────────────

    private static JsonObject loadOrCreateConfig() {
        File file = new File(CONFIG_PATH);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                return new Gson().fromJson(reader, JsonObject.class);
            } catch (Exception e) {
                System.err.println("[DatabaseManager] Error reading db_config.json: " + e.getMessage());
            }
        }

        file.getParentFile().mkdirs();
        JsonObject defaults = new JsonObject();
        defaults.addProperty("host",     "localhost");
        defaults.addProperty("port",     3306);
        defaults.addProperty("database", "countertale");
        defaults.addProperty("username", "root");
        defaults.addProperty("password", "password");

        try (FileWriter writer = new FileWriter(file)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(defaults, writer);
            System.out.println("[DatabaseManager] Default db_config.json created at " + CONFIG_PATH
                    + " — fill in your credentials and restart.");
        } catch (Exception e) {
            System.err.println("[DatabaseManager] Could not write db_config.json: " + e.getMessage());
        }
        return defaults;
    }
}
