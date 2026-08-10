package me.vennlmao.ariscore.duel.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.vennlmao.ariscore.duel.DuelModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.sql.*;
import java.util.*;

public class DuelArenaDatabaseManager implements AutoCloseable {

    private final DuelModule module;
    private HikariDataSource dataSource;
    private boolean mysql;

    public DuelArenaDatabaseManager(DuelModule module) { this.module = module; }

    private String tableName() {
        return module.getConfig().getString("mysql.table-prefix", "ariscore_") + "duel_arenas";
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
        config.setPoolName("ArisDuelArena-Pool");
        dataSource = new HikariDataSource(config);
        createTable();
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName() + " (" +
                "name VARCHAR(64) NOT NULL PRIMARY KEY," +
                "world VARCHAR(64) NOT NULL," +
                "x1 DOUBLE NOT NULL, y1 DOUBLE NOT NULL, z1 DOUBLE NOT NULL, yaw1 FLOAT NOT NULL, pitch1 FLOAT NOT NULL," +
                "x2 DOUBLE NOT NULL, y2 DOUBLE NOT NULL, z2 DOUBLE NOT NULL, yaw2 FLOAT NOT NULL, pitch2 FLOAT NOT NULL)";
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Duel] Failed to create arena table: " + e.getMessage());
        }
    }

    public Map<String, DuelArena> getAllArenas() {
        Map<String, DuelArena> arenas = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM " + tableName());
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                World world = Bukkit.getWorld(rs.getString("world"));
                if (world == null) continue;
                Location pos1 = new Location(world, rs.getDouble("x1"), rs.getDouble("y1"), rs.getDouble("z1"), rs.getFloat("yaw1"), rs.getFloat("pitch1"));
                Location pos2 = new Location(world, rs.getDouble("x2"), rs.getDouble("y2"), rs.getDouble("z2"), rs.getFloat("yaw2"), rs.getFloat("pitch2"));
                String name = rs.getString("name");
                arenas.put(name, new DuelArena(name, pos1, pos2));
            }
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Duel] Failed to load arenas: " + e.getMessage());
        }
        return arenas;
    }

    public void saveArena(DuelArena arena) {
        String sql = mysql
                ? "INSERT INTO " + tableName() + " (name,world,x1,y1,z1,yaw1,pitch1,x2,y2,z2,yaw2,pitch2) VALUES (?,?,?,?,?,?,?,?,?,?,?,?) " +
                  "ON DUPLICATE KEY UPDATE world=?,x1=?,y1=?,z1=?,yaw1=?,pitch1=?,x2=?,y2=?,z2=?,yaw2=?,pitch2=?"
                : "INSERT OR REPLACE INTO " + tableName() + " (name,world,x1,y1,z1,yaw1,pitch1,x2,y2,z2,yaw2,pitch2) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            Location p1 = arena.getPos1();
            Location p2 = arena.getPos2();
            String world = p1.getWorld().getName();
            ps.setString(1, arena.getName());
            ps.setString(2, world);
            ps.setDouble(3, p1.getX()); ps.setDouble(4, p1.getY()); ps.setDouble(5, p1.getZ());
            ps.setFloat(6, p1.getYaw()); ps.setFloat(7, p1.getPitch());
            ps.setDouble(8, p2.getX()); ps.setDouble(9, p2.getY()); ps.setDouble(10, p2.getZ());
            ps.setFloat(11, p2.getYaw()); ps.setFloat(12, p2.getPitch());
            if (mysql) {
                ps.setString(13, world);
                ps.setDouble(14, p1.getX()); ps.setDouble(15, p1.getY()); ps.setDouble(16, p1.getZ());
                ps.setFloat(17, p1.getYaw()); ps.setFloat(18, p1.getPitch());
                ps.setDouble(19, p2.getX()); ps.setDouble(20, p2.getY()); ps.setDouble(21, p2.getZ());
                ps.setFloat(22, p2.getYaw()); ps.setFloat(23, p2.getPitch());
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Duel] Failed to save arena: " + e.getMessage());
        }
    }

    public void deleteArena(String name) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM " + tableName() + " WHERE name = ?")) {
            ps.setString(1, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Duel] Failed to delete arena: " + e.getMessage());
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }
}
