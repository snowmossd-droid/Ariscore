package me.vennlmao.ariscore.afk.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.vennlmao.ariscore.afk.AfkModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.sql.*;
import java.util.*;

public class AfkDatabaseManager {

    private final AfkModule module;
    private HikariDataSource dataSource;
    private boolean mysql;

    public AfkDatabaseManager(AfkModule module) {
        this.module = module;
    }

    private String prefix() {
        return module.getConfig().getString("mysql.table-prefix", "ariscore_");
    }

    private String tableName() {
        return prefix() + module.getConfig().getString("mysql.table-name", "afkzones");
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

            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=" + ssl + "&autoReconnect=true");
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            File dbFile = new File(module.getPlugin().getDataFolder(),
                    "afk/" + module.getConfig().getString("mysql.table-name", "afkzones") + ".db");
            dbFile.getParentFile().mkdirs();
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
        }

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("ArisAfk-Pool");

        dataSource = new HikariDataSource(config);
        createTable();
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName() + " (" +
                "name VARCHAR(64) NOT NULL PRIMARY KEY," +
                "world VARCHAR(64) NOT NULL," +
                "x DOUBLE NOT NULL," +
                "y DOUBLE NOT NULL," +
                "z DOUBLE NOT NULL," +
                "yaw FLOAT NOT NULL," +
                "pitch FLOAT NOT NULL" +
                ")";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[AFK] Failed to create table: " + e.getMessage());
        }
    }

    public Map<String, Location> getAllZones() {
        Map<String, Location> zones = new LinkedHashMap<>();
        String sql = "SELECT * FROM " + tableName();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString("name");
                World world = Bukkit.getWorld(rs.getString("world"));
                if (world == null) continue;
                zones.put(name, new Location(world,
                        rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                        rs.getFloat("yaw"), rs.getFloat("pitch")));
            }
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[AFK] Failed to get zones: " + e.getMessage());
        }
        return zones;
    }

    public void deleteZone(String name) {
        String sql = "DELETE FROM " + tableName() + " WHERE name = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[AFK] Failed to delete zone: " + e.getMessage());
        }
    }

    public void setZone(String name, Location loc) {
        String sql = mysql
                ? "INSERT INTO " + tableName() + " (name,world,x,y,z,yaw,pitch) VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE world=?,x=?,y=?,z=?,yaw=?,pitch=?"
                : "INSERT OR REPLACE INTO " + tableName() + " (name,world,x,y,z,yaw,pitch) VALUES (?,?,?,?,?,?,?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "";
            ps.setString(1, name);
            ps.setString(2, worldName);
            ps.setDouble(3, loc.getX());
            ps.setDouble(4, loc.getY());
            ps.setDouble(5, loc.getZ());
            ps.setFloat(6, loc.getYaw());
            ps.setFloat(7, loc.getPitch());
            if (mysql) {
                ps.setString(8, worldName);
                ps.setDouble(9, loc.getX());
                ps.setDouble(10, loc.getY());
                ps.setDouble(11, loc.getZ());
                ps.setFloat(12, loc.getYaw());
                ps.setFloat(13, loc.getPitch());
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[AFK] Failed to set zone: " + e.getMessage());
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }
}
