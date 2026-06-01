package me.vennlmao.ariscore.home.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.vennlmao.ariscore.home.HomeModule;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.sql.*;
import java.util.*;

public class DatabaseManager {

    private final HomeModule plugin;
    private HikariDataSource dataSource;
    private boolean mysql;

    public DatabaseManager(HomeModule plugin) {
        this.plugin = plugin;
    }

    public void init() {
        mysql = plugin.getConfig().getBoolean("mysql.enabled", false);

        HikariConfig config = new HikariConfig();

        if (mysql) {
            String host = plugin.getConfig().getString("mysql.host", "localhost");
            int port = plugin.getConfig().getInt("mysql.port", 3306);
            String database = plugin.getConfig().getString("mysql.database", "arishomes");
            String username = plugin.getConfig().getString("mysql.username", "root");
            String password = plugin.getConfig().getString("mysql.password", "");
            boolean ssl = plugin.getConfig().getBoolean("mysql.use-ssl", false);

            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + ssl + "&autoReconnect=true");
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            File dbFile = new File(plugin.getPlugin().getDataFolder(), "homes.db");
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
        }

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("ArisHomes-Pool");

        dataSource = new HikariDataSource(config);
        createTable();
    }

    private void createTable() {
        String prefix = plugin.getConfig().getString("mysql.table-prefix", "arishomes_");
        String sql = "CREATE TABLE IF NOT EXISTS " + prefix + "homes (" +
                "uuid VARCHAR(36) NOT NULL," +
                "name VARCHAR(64) NOT NULL," +
                "world VARCHAR(64) NOT NULL," +
                "x DOUBLE NOT NULL," +
                "y DOUBLE NOT NULL," +
                "z DOUBLE NOT NULL," +
                "yaw FLOAT NOT NULL," +
                "pitch FLOAT NOT NULL," +
                "PRIMARY KEY (uuid, name)" +
                ")";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getPlugin().getLogger().severe("Failed to create table: " + e.getMessage());
        }
    }

    public List<String> getHomes(UUID uuid) {
        String prefix = plugin.getConfig().getString("mysql.table-prefix", "arishomes_");
        List<String> homes = new ArrayList<>();
        String sql = "SELECT name FROM " + prefix + "homes WHERE uuid = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) homes.add(rs.getString("name"));
        } catch (SQLException e) {
            plugin.getPlugin().getLogger().severe("Failed to get homes: " + e.getMessage());
        }
        return homes;
    }

    public Location getHome(UUID uuid, String name) {
        String prefix = plugin.getConfig().getString("mysql.table-prefix", "arishomes_");
        String sql = "SELECT * FROM " + prefix + "homes WHERE uuid = ? AND name = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                World world = plugin.getPlugin().getServer().getWorld(rs.getString("world"));
                if (world == null) return null;
                return new Location(world,
                        rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                        rs.getFloat("yaw"), rs.getFloat("pitch"));
            }
        } catch (SQLException e) {
            plugin.getPlugin().getLogger().severe("Failed to get home: " + e.getMessage());
        }
        return null;
    }

    public void setHome(UUID uuid, String name, Location loc) {
        String prefix = plugin.getConfig().getString("mysql.table-prefix", "arishomes_");
        String sql = mysql
                ? "INSERT INTO " + prefix + "homes (uuid,name,world,x,y,z,yaw,pitch) VALUES (?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE world=?,x=?,y=?,z=?,yaw=?,pitch=?"
                : "INSERT OR REPLACE INTO " + prefix + "homes (uuid,name,world,x,y,z,yaw,pitch) VALUES (?,?,?,?,?,?,?,?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "";
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setString(3, worldName);
            ps.setDouble(4, loc.getX());
            ps.setDouble(5, loc.getY());
            ps.setDouble(6, loc.getZ());
            ps.setFloat(7, loc.getYaw());
            ps.setFloat(8, loc.getPitch());
            if (mysql) {
                ps.setString(9, worldName);
                ps.setDouble(10, loc.getX());
                ps.setDouble(11, loc.getY());
                ps.setDouble(12, loc.getZ());
                ps.setFloat(13, loc.getYaw());
                ps.setFloat(14, loc.getPitch());
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getPlugin().getLogger().severe("Failed to set home: " + e.getMessage());
        }
    }

    public void deleteHome(UUID uuid, String name) {
        String prefix = plugin.getConfig().getString("mysql.table-prefix", "arishomes_");
        String sql = "DELETE FROM " + prefix + "homes WHERE uuid = ? AND name = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getPlugin().getLogger().severe("Failed to delete home: " + e.getMessage());
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }
}
