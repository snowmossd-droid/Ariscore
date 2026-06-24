package me.vennlmao.ariscore.warp.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.vennlmao.ariscore.warp.WarpModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.sql.*;
import java.util.*;

public class WarpDatabaseManager {

    private final WarpModule module;
    private HikariDataSource dataSource;
    private boolean mysql;

    public WarpDatabaseManager(WarpModule module) { this.module = module; }

    private String tableName() {
        return module.getConfig().getString("mysql.table-prefix", "ariscore_")
             + module.getConfig().getString("mysql.table-name", "warps");
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
            File dbFile = new File(module.getPlugin().getDataFolder(),
                    "warp/" + module.getConfig().getString("mysql.table-name", "warps") + ".db");
            dbFile.getParentFile().mkdirs();
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
        }

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("ArisWarp-Pool");
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
                "pitch FLOAT NOT NULL)";
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Warp] Failed to create table: " + e.getMessage());
        }
    }

    public Map<String, Location> getAllWarps() {
        Map<String, Location> warps = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM " + tableName());
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                World world = Bukkit.getWorld(rs.getString("world"));
                if (world == null) continue;
                warps.put(rs.getString("name"), new Location(world,
                        rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                        rs.getFloat("yaw"), rs.getFloat("pitch")));
            }
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Warp] Failed to get warps: " + e.getMessage());
        }
        return warps;
    }

    public void setWarp(String name, Location loc) {
        String sql = mysql
                ? "INSERT INTO " + tableName() + " (name,world,x,y,z,yaw,pitch) VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE world=?,x=?,y=?,z=?,yaw=?,pitch=?"
                : "INSERT OR REPLACE INTO " + tableName() + " (name,world,x,y,z,yaw,pitch) VALUES (?,?,?,?,?,?,?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            String w = loc.getWorld() != null ? loc.getWorld().getName() : "";
            ps.setString(1, name); ps.setString(2, w);
            ps.setDouble(3, loc.getX()); ps.setDouble(4, loc.getY()); ps.setDouble(5, loc.getZ());
            ps.setFloat(6, loc.getYaw()); ps.setFloat(7, loc.getPitch());
            if (mysql) {
                ps.setString(8, w);
                ps.setDouble(9, loc.getX()); ps.setDouble(10, loc.getY()); ps.setDouble(11, loc.getZ());
                ps.setFloat(12, loc.getYaw()); ps.setFloat(13, loc.getPitch());
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Warp] Failed to set warp: " + e.getMessage());
        }
    }

    public void deleteWarp(String name) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM " + tableName() + " WHERE name = ?")) {
            ps.setString(1, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Warp] Failed to delete warp: " + e.getMessage());
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }
}
