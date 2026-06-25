package me.vennlmao.ariscore.crates.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.vennlmao.ariscore.crates.CratesModule;
import me.vennlmao.ariscore.crates.models.GamerModel;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CratesDatabaseManager {

    private final CratesModule module;
    private HikariDataSource dataSource;
    private boolean mysql;
    private String tableName;

    public CratesDatabaseManager(CratesModule module) {
        this.module = module;
    }

    public void init() {
        mysql = module.getConfig().getBoolean("mysql.enabled", false);
        HikariConfig config = new HikariConfig();

        if (mysql) {
            String host     = module.getConfig().getString("mysql.host", "localhost");
            int port        = module.getConfig().getInt("mysql.port", 3306);
            String database = module.getConfig().getString("mysql.database", "ariscore");
            String username = module.getConfig().getString("mysql.username", "root");
            String password = module.getConfig().getString("mysql.password", "");
            boolean ssl     = module.getConfig().getBoolean("mysql.use-ssl", false);

            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=" + ssl + "&autoReconnect=true&characterEncoding=utf8");
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            File dbFile = new File(module.getPlugin().getDataFolder(), "crates/crates.db");
            dbFile.getParentFile().mkdirs();
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
        }

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setPoolName("ArisCrates-Pool");
        dataSource = new HikariDataSource(config);

        tableName = resolveTableName();
        createTable();
    }

    private String resolveTableName() {
        String custom = module.getConfig().getString("mysql.table-name", "");
        if (custom != null && !custom.isEmpty()) return custom;
        String prefix = module.getConfig().getString("mysql.table-prefix", "ariscore_");
        return prefix + "crates_keys";
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + "uuid VARCHAR(36) NOT NULL,"
                + "crate_name VARCHAR(64) NOT NULL,"
                + "amount INT NOT NULL DEFAULT 0,"
                + "PRIMARY KEY (uuid, crate_name)"
                + ")";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Crates] Failed to create table: " + e.getMessage());
        }
    }

    public GamerModel loadPlayer(UUID uuid) {
        GamerModel gamer = new GamerModel(uuid);
        String sql = "SELECT crate_name, amount FROM " + tableName + " WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                gamer.addKeyAmount(rs.getString("crate_name"), rs.getInt("amount"));
            }
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Crates] Failed to load player " + uuid + ": " + e.getMessage());
        }
        return gamer;
    }

    public void savePlayer(GamerModel gamer) {
        String sql = mysql
                ? "INSERT INTO " + tableName + " (uuid, crate_name, amount) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE amount = VALUES(amount)"
                : "INSERT OR REPLACE INTO " + tableName + " (uuid, crate_name, amount) VALUES (?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Map.Entry<String, Integer> entry : gamer.getKeys().entrySet()) {
                ps.setString(1, gamer.getUniqueId().toString());
                ps.setString(2, entry.getKey());
                ps.setInt(3, entry.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Crates] Failed to save player " + gamer.getUniqueId() + ": " + e.getMessage());
        }
    }

    public void saveAllPlayers(GamerDataManager dataManager) {
        for (GamerModel gamer : dataManager.values()) {
            savePlayer(gamer);
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }
}
