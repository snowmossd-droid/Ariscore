package me.vennlmao.ariscore.shards.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.vennlmao.ariscore.shards.ShardsModule;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public class ShardsDatabaseManager {

    private final ShardsModule module;
    private HikariDataSource dataSource;
    private boolean mysql;

    public ShardsDatabaseManager(ShardsModule module) {
        this.module = module;
    }

    public void init() {
        mysql = module.getConfig().getBoolean("mysql.enabled", false);
        HikariConfig config = new HikariConfig();

        if (mysql) {
            String host = module.getConfig().getString("mysql.host", "localhost");
            int port = module.getConfig().getInt("mysql.port", 3306);
            String database = module.getConfig().getString("mysql.database", "ariscore");
            String username = module.getConfig().getString("mysql.username", "root");
            String password = module.getConfig().getString("mysql.password", "");
            boolean ssl = module.getConfig().getBoolean("mysql.use-ssl", false);
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + ssl + "&autoReconnect=true");
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            File dbFile = new File(module.getPlugin().getDataFolder(), "shards/shards.db");
            dbFile.getParentFile().mkdirs();
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
        }

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setPoolName("ArisShards-Pool");
        dataSource = new HikariDataSource(config);
        createTable();
    }

    private void createTable() {
        String prefix = module.getConfig().getString("mysql.table-prefix", "ariscore_");
        String sql = "CREATE TABLE IF NOT EXISTS " + prefix + "shards (" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "amount BIGINT NOT NULL DEFAULT 0" +
                ")";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Shards] Failed to create table: " + e.getMessage());
        }
    }

    public long getShards(UUID uuid) {
        if (dataSource == null) return 0;
        String prefix = module.getConfig().getString("mysql.table-prefix", "ariscore_");
        String sql = "SELECT amount FROM " + prefix + "shards WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong("amount");
        } catch (Exception e) {
            module.getPlugin().getLogger().severe("[Shards] Failed to get shards: " + e.getMessage());
        }
        return 0;
    }

    public void setShards(UUID uuid, long amount) {
        String prefix = module.getConfig().getString("mysql.table-prefix", "ariscore_");
        String sql = mysql
                ? "INSERT INTO " + prefix + "shards (uuid, amount) VALUES (?, ?) ON DUPLICATE KEY UPDATE amount = ?"
                : "INSERT OR REPLACE INTO " + prefix + "shards (uuid, amount) VALUES (?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, amount);
            if (mysql) ps.setLong(3, amount);
            ps.executeUpdate();
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Shards] Failed to set shards: " + e.getMessage());
        }
    }

    public void addShards(UUID uuid, long amount) {
        setShards(uuid, getShards(uuid) + amount);
    }

    public boolean takeShards(UUID uuid, long amount) {
        long current = getShards(uuid);
        if (current < amount) return false;
        setShards(uuid, current - amount);
        return true;
    }

    public void resetShards(UUID uuid) {
        setShards(uuid, 0);
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }
                }
                                      
