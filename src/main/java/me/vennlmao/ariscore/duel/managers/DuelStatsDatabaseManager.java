package me.vennlmao.ariscore.duel.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.vennlmao.ariscore.duel.DuelModule;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public class DuelStatsDatabaseManager implements AutoCloseable {

    private final DuelModule module;
    private HikariDataSource dataSource;
    private boolean mysql;

    public DuelStatsDatabaseManager(DuelModule module) { this.module = module; }

    private String tableName() {
        return module.getConfig().getString("mysql.table-prefix", "ariscore_") + "duel_stats";
    }

    public void init() {
        mysql = module.getConfig().getBoolean("mysql.enabled", false);
        HikariConfig config = new HikariConfig();

        if (mysql) {
            String host     = module.getConfig().getString("mysql.host", "localhost");
            int    port     = module.getConfig().getInt("mysql.port", 3306);
            String database = module.getConfig().getString("mysql.database", "ariscore");
            String username = module.getConfig().getString("mysql.username", "root");
            String password = module.getConfig().getString("mysql.password", "");
            boolean ssl     = module.getConfig().getBoolean("mysql.use-ssl", false);
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + ssl + "&autoReconnect=true");
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            File dbFile = new File(module.getPlugin().getDataFolder(), "duel/duels.db");
            dbFile.getParentFile().mkdirs();
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
        }

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("ArisDuelStats-Pool");
        dataSource = new HikariDataSource(config);
        createTable();
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName() + " (" +
                "uuid VARCHAR(36) NOT NULL PRIMARY KEY," +
                "wins INT NOT NULL DEFAULT 0," +
                "losses INT NOT NULL DEFAULT 0," +
                "draws INT NOT NULL DEFAULT 0," +
                "streak INT NOT NULL DEFAULT 0," +
                "best_streak INT NOT NULL DEFAULT 0)";
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Duel] Failed to create stats table: " + e.getMessage());
        }
    }

    public DuelStats loadStats(UUID uuid) {
        String sql = "SELECT * FROM " + tableName() + " WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new DuelStats(uuid, rs.getInt("wins"), rs.getInt("losses"), rs.getInt("draws"), rs.getInt("streak"), rs.getInt("best_streak"));
                }
            }
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Duel] Failed to load stats: " + e.getMessage());
        }
        return new DuelStats(uuid, 0, 0, 0, 0, 0);
    }

    public void saveStats(DuelStats stats) {
        String sql = mysql
                ? "INSERT INTO " + tableName() + " (uuid,wins,losses,draws,streak,best_streak) VALUES (?,?,?,?,?,?) " +
                  "ON DUPLICATE KEY UPDATE wins=?,losses=?,draws=?,streak=?,best_streak=?"
                : "INSERT OR REPLACE INTO " + tableName() + " (uuid,wins,losses,draws,streak,best_streak) VALUES (?,?,?,?,?,?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, stats.getUuid().toString());
            ps.setInt(2, stats.getWins());
            ps.setInt(3, stats.getLosses());
            ps.setInt(4, stats.getDraws());
            ps.setInt(5, stats.getStreak());
            ps.setInt(6, stats.getBestStreak());
            if (mysql) {
                ps.setInt(7, stats.getWins());
                ps.setInt(8, stats.getLosses());
                ps.setInt(9, stats.getDraws());
                ps.setInt(10, stats.getStreak());
                ps.setInt(11, stats.getBestStreak());
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Duel] Failed to save stats: " + e.getMessage());
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }
}
