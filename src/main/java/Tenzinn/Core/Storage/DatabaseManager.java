package Tenzinn.Core.Storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DatabaseManager {

    private static final File CONFIG_FILE = resolveConfigFile();

    private static String jdbcUrl;
    private static String serverJdbcUrl;
    private static String databaseName;
    private static String dbUser;
    private static String dbPassword;
    private static Connection connection;
    private static volatile boolean initialized;

    private static final ExecutorService DB_EXECUTOR = Executors.newSingleThreadExecutor();

    public static void init() {
        JsonObject config = loadOrCreateConfig();
        databaseName = config.get("database").getAsString();
        serverJdbcUrl = String.format(
                "jdbc:mysql://%s:%d/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                config.get("host").getAsString(),
                config.get("port").getAsInt()
        );
        jdbcUrl = String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                config.get("host").getAsString(),
                config.get("port").getAsInt(),
                databaseName
        );
        dbUser     = config.get("username").getAsString();
        dbPassword = config.get("password").getAsString();

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            createDatabaseIfNeeded();
            connection = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
            createTables();
            initialized = true;
            System.out.println("[DatabaseManager] Connected to database.");
        } catch (Exception e) {
            initialized = false;
            System.err.println("[DatabaseManager] Could not connect: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void close() {
        DB_EXECUTOR.shutdown();
        try { DB_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
        try { if (connection != null && !connection.isClosed()) connection.close(); } catch (SQLException ignored) {}
    }

    // â”€â”€ Internal â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private static synchronized Connection getConnection() throws SQLException {
        if (jdbcUrl == null || dbUser == null || dbPassword == null) {
            throw new SQLException("DatabaseManager is not configured. Check " + CONFIG_FILE.getPath());
        }
        if (connection == null || connection.isClosed()) {
            try { Class.forName("com.mysql.cj.jdbc.Driver"); } catch (ClassNotFoundException ignored) {}
            connection = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
        }
        return connection;
    }

    private static void createDatabaseIfNeeded() throws SQLException {
        try (Connection serverConnection = DriverManager.getConnection(serverJdbcUrl, dbUser, dbPassword);
             Statement stmt = serverConnection.createStatement()) {
            stmt.execute("CREATE DATABASE IF NOT EXISTS `" + databaseName.replace("`", "``") + "` DEFAULT CHARACTER SET utf8mb4");
        }
    }

    private static void createTables() {
        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS players (
                    uuid       VARCHAR(36) PRIMARY KEY,
                    username   VARCHAR(64) NOT NULL,
                    first_seen TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
                    last_seen  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);
            migratePlayersTable(stmt);
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
                CREATE TABLE IF NOT EXISTS player_cases (
                    player_uuid VARCHAR(36) PRIMARY KEY,
                    case_count  INT NOT NULL DEFAULT 0,
                    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
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

    private static void migratePlayersTable(Statement stmt) {
        try {
            stmt.execute("ALTER TABLE players MODIFY username VARCHAR(64) NOT NULL");
        } catch (SQLException e) {
            System.err.println("[DatabaseManager] players migration warning: " + e.getMessage());
        }
    }

    private static void ensurePlayerExists(Connection conn, UUID uuid) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO players (uuid, username) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE last_seen = CURRENT_TIMESTAMP")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, "Unknown");
            stmt.executeUpdate();
        }
    }

    // â”€â”€ Public API â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public static CompletableFuture<Void> registerPlayer(UUID uuid, String username) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = getConnection().prepareStatement(
                    "INSERT INTO players (uuid, username) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE username = VALUES(username), last_seen = CURRENT_TIMESTAMP")) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, username);
                stmt.executeUpdate();
            } catch (Exception e) {
                System.err.println("[DatabaseManager] registerPlayer error: " + e.getMessage());
                e.printStackTrace();
            }
        }, DB_EXECUTOR);
    }

    public static CompletableFuture<Void> addSkinToInventory(UUID uuid, String skinId) {
        return CompletableFuture.runAsync(() -> {
            try {
                Connection conn = getConnection();
                ensurePlayerExists(conn, uuid);
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO player_skins (player_uuid, skin_id) VALUES (?, ?)")) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, skinId);
                stmt.executeUpdate();
                }
            } catch (Exception e) {
                System.err.println("[DatabaseManager] addSkinToInventory error: " + e.getMessage());
                e.printStackTrace();
            }
        }, DB_EXECUTOR);
    }

    public static CompletableFuture<Void> addCases(UUID uuid, int amount) {
        if (amount == 0) return CompletableFuture.completedFuture(null);

        return CompletableFuture.runAsync(() -> {
            try {
                Connection conn = getConnection();
                ensurePlayerExists(conn, uuid);
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO player_cases (player_uuid, case_count) VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE case_count = GREATEST(case_count + VALUES(case_count), 0), updated_at = CURRENT_TIMESTAMP")) {
                    stmt.setString(1, uuid.toString());
                    stmt.setInt(2, amount);
                    stmt.executeUpdate();
                }
            } catch (Exception e) {
                System.err.println("[DatabaseManager] addCases error: " + e.getMessage());
                e.printStackTrace();
            }
        }, DB_EXECUTOR);
    }

    public static CompletableFuture<Void> decrementCase(UUID uuid) {
        return addCases(uuid, -1);
    }

    public static CompletableFuture<Void> clearCases(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = getConnection().prepareStatement(
                    "DELETE FROM player_cases WHERE player_uuid = ?")) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
            } catch (Exception e) {
                System.err.println("[DatabaseManager] clearCases error: " + e.getMessage());
                e.printStackTrace();
            }
        }, DB_EXECUTOR);
    }

    public static CompletableFuture<Integer> loadCaseCount(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement stmt = getConnection().prepareStatement(
                    "SELECT case_count FROM player_cases WHERE player_uuid = ?")) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) return Math.max(0, rs.getInt("case_count"));
            } catch (Exception e) {
                System.err.println("[DatabaseManager] loadCaseCount error: " + e.getMessage());
                e.printStackTrace();
            }
            return 0;
        }, DB_EXECUTOR);
    }

    /** Upserts the skin selected for a specific weapon. */
    public static CompletableFuture<Void> setSelectedSkin(UUID uuid, String weaponId, String skinId) {
        return CompletableFuture.runAsync(() -> {
            try {
                Connection conn = getConnection();
                ensurePlayerExists(conn, uuid);
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO player_selected_skins (player_uuid, weapon_id, skin_id) VALUES (?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE skin_id = VALUES(skin_id), updated_at = CURRENT_TIMESTAMP")) {
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, weaponId);
                    stmt.setString(3, skinId);
                    stmt.executeUpdate();
                }
            } catch (Exception e) {
                System.err.println("[DatabaseManager] setSelectedSkin error: " + e.getMessage());
                e.printStackTrace();
            }
        }, DB_EXECUTOR);
    }

    public static CompletableFuture<Void> clearSelectedSkin(UUID uuid, String weaponId) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = getConnection().prepareStatement(
                    "DELETE FROM player_selected_skins WHERE player_uuid = ? AND weapon_id = ?")) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, weaponId);
                stmt.executeUpdate();
            } catch (Exception e) {
                System.err.println("[DatabaseManager] clearSelectedSkin error: " + e.getMessage());
                e.printStackTrace();
            }
        }, DB_EXECUTOR);
    }

    /** Returns all weaponâ†’skinId selections for a player. */
    public static CompletableFuture<Map<String, String>> loadSelectedSkins(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> map = new java.util.HashMap<>();
            try (PreparedStatement stmt = getConnection().prepareStatement(
                    "SELECT weapon_id, skin_id FROM player_selected_skins WHERE player_uuid = ?")) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) map.put(rs.getString("weapon_id"), rs.getString("skin_id"));
            } catch (Exception e) {
                System.err.println("[DatabaseManager] loadSelectedSkins error: " + e.getMessage());
                e.printStackTrace();
            }
            return map;
        }, DB_EXECUTOR);
    }

    public static CompletableFuture<Void> clearSkinInventory(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement stmt = getConnection().prepareStatement(
                    "DELETE FROM player_skins WHERE player_uuid = ?")) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
            } catch (Exception e) {
                System.err.println("[DatabaseManager] clearSkinInventory error: " + e.getMessage());
                e.printStackTrace();
            }
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
            } catch (Exception e) {
                System.err.println("[DatabaseManager] loadSkinIds error: " + e.getMessage());
                e.printStackTrace();
            }
            return ids;
        }, DB_EXECUTOR);
    }

    public static CompletableFuture<String> debugStatus(UUID uuid, String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Connection conn = getConnection();
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO players (uuid, username) VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE username = VALUES(username), last_seen = CURRENT_TIMESTAMP")) {
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, username);
                    stmt.executeUpdate();
                }

                int skins = countRows(conn, "player_skins", uuid);
                int selections = countRows(conn, "player_selected_skins", uuid);
                int cases = 0;
                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT case_count FROM player_cases WHERE player_uuid = ?")) {
                    stmt.setString(1, uuid.toString());
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) cases = Math.max(0, rs.getInt("case_count"));
                }

                return "SQL OK | initialized=" + initialized
                        + " | cases=" + cases
                        + " | skins=" + skins
                        + " | selections=" + selections;
            } catch (Exception e) {
                System.err.println("[DatabaseManager] debugStatus error: " + e.getMessage());
                e.printStackTrace();
                return "SQL ERROR | initialized=" + initialized + " | " + e.getMessage()
                        + " | check " + CONFIG_FILE.getPath();
            }
        }, DB_EXECUTOR);
    }

    private static int countRows(Connection conn, String tableName, UUID uuid) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT COUNT(*) AS total FROM " + tableName + " WHERE player_uuid = ?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt("total") : 0;
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }

    // â”€â”€ Config â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private static JsonObject loadOrCreateConfig() {
        File file = CONFIG_FILE;
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                System.out.println("[DatabaseManager] Loading db_config.json from " + file.getPath());
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
        defaults.addProperty("password", "");

        try (FileWriter writer = new FileWriter(file)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(defaults, writer);
            System.out.println("[DatabaseManager] Default db_config.json created at " + file.getPath()
                    + " â€” fill in your credentials and restart.");
        } catch (Exception e) {
            System.err.println("[DatabaseManager] Could not write db_config.json: " + e.getMessage());
        }
        return defaults;
    }

    private static File resolveConfigFile() {
        try {
            File location = new File(DatabaseManager.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());

            File baseDir = location.isFile()
                    ? location.getParentFile()
                    : new File(System.getProperty("user.dir"));

            return new File(baseDir, "Countertale/db_config.json");
        } catch (Exception e) {
            return new File("Countertale/db_config.json");
        }
    }
}

