package me.vennlmao.ariscore.team.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.vennlmao.ariscore.team.TeamModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.sql.*;
import java.util.*;

public class TeamDatabaseManager {

    private final TeamModule module;
    private HikariDataSource dataSource;
    private boolean mysql;

    public TeamDatabaseManager(TeamModule module) {
        this.module = module;
    }

    public void init() {
        mysql = module.getConfig().getBoolean("mysql.enabled", false);
        HikariConfig config = new HikariConfig();

        if (mysql) {
            String host = module.getConfig().getString("mysql.host");
            int port = module.getConfig().getInt("mysql.port");
            String database = module.getConfig().getString("mysql.database");
            String username = module.getConfig().getString("mysql.username");
            String password = module.getConfig().getString("mysql.password");
            boolean ssl = module.getConfig().getBoolean("mysql.use-ssl");
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + ssl + "&autoReconnect=true");
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            File dbFile = new File(module.getPlugin().getDataFolder(), "team/team.db");
            dbFile.getParentFile().mkdirs();
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
        }

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setPoolName("ArisTeam-Pool");
        dataSource = new HikariDataSource(config);
        createTables();
    }

    private String prefix() {
        return module.getConfig().getString("mysql.table-prefix", "ariscore_");
    }

    private void createTables() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS " + prefix() + "teams (" +
                    "name VARCHAR(64) PRIMARY KEY," +
                    "home_world VARCHAR(64)," +
                    "home_x DOUBLE," +
                    "home_y DOUBLE," +
                    "home_z DOUBLE," +
                    "home_yaw FLOAT," +
                    "home_pitch FLOAT" +
                    ")");
            stmt.execute("CREATE TABLE IF NOT EXISTS " + prefix() + "team_members (" +
                    "uuid VARCHAR(36) NOT NULL," +
                    "team_name VARCHAR(64) NOT NULL," +
                    "role VARCHAR(16) NOT NULL," +
                    "PRIMARY KEY (uuid)" +
                    ")");
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Team] Failed to create tables: " + e.getMessage());
        }
    }

    public Map<String, TeamData> loadAllTeams() {
        Map<String, TeamData> teams = new HashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM " + prefix() + "teams");
            while (rs.next()) {
                String name = rs.getString("name");
                TeamData team = new TeamData(name);
                String world = rs.getString("home_world");
                if (world != null) {
                    World w = Bukkit.getWorld(world);
                    if (w != null) {
                        team.setHome(new Location(w,
                                rs.getDouble("home_x"), rs.getDouble("home_y"), rs.getDouble("home_z"),
                                rs.getFloat("home_yaw"), rs.getFloat("home_pitch")));
                    }
                }
                teams.put(name.toLowerCase(), team);
            }

            ResultSet mrs = conn.createStatement().executeQuery("SELECT * FROM " + prefix() + "team_members");
            while (mrs.next()) {
                String teamName = mrs.getString("team_name");
                TeamData team = teams.get(teamName.toLowerCase());
                if (team == null) continue;
                UUID uuid = UUID.fromString(mrs.getString("uuid"));
                TeamData.Role role = TeamData.Role.valueOf(mrs.getString("role"));
                team.addMember(uuid, role);
            }
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Team] Failed to load teams: " + e.getMessage());
        }
        return teams;
    }

    public void saveTeam(TeamData team) {
        String sql = mysql
                ? "INSERT INTO " + prefix() + "teams (name,home_world,home_x,home_y,home_z,home_yaw,home_pitch) VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE home_world=?,home_x=?,home_y=?,home_z=?,home_yaw=?,home_pitch=?"
                : "INSERT OR REPLACE INTO " + prefix() + "teams (name,home_world,home_x,home_y,home_z,home_yaw,home_pitch) VALUES (?,?,?,?,?,?,?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            Location home = team.getHome();
            String world = home != null && home.getWorld() != null ? home.getWorld().getName() : null;
            double x = home != null ? home.getX() : 0;
            double y = home != null ? home.getY() : 0;
            double z = home != null ? home.getZ() : 0;
            float yaw = home != null ? home.getYaw() : 0;
            float pitch = home != null ? home.getPitch() : 0;
            ps.setString(1, team.getName());
            ps.setString(2, world);
            ps.setDouble(3, x); ps.setDouble(4, y); ps.setDouble(5, z);
            ps.setFloat(6, yaw); ps.setFloat(7, pitch);
            if (mysql) {
                ps.setString(8, world);
                ps.setDouble(9, x); ps.setDouble(10, y); ps.setDouble(11, z);
                ps.setFloat(12, yaw); ps.setFloat(13, pitch);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Team] Failed to save team: " + e.getMessage());
        }
    }

    public void deleteTeam(String name) {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement ps1 = conn.prepareStatement("DELETE FROM " + prefix() + "teams WHERE name=?");
            ps1.setString(1, name); ps1.executeUpdate();
            PreparedStatement ps2 = conn.prepareStatement("DELETE FROM " + prefix() + "team_members WHERE team_name=?");
            ps2.setString(1, name); ps2.executeUpdate();
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Team] Failed to delete team: " + e.getMessage());
        }
    }

    public void saveMember(UUID uuid, String teamName, TeamData.Role role) {
        String sql = mysql
                ? "INSERT INTO " + prefix() + "team_members (uuid,team_name,role) VALUES (?,?,?) ON DUPLICATE KEY UPDATE team_name=?,role=?"
                : "INSERT OR REPLACE INTO " + prefix() + "team_members (uuid,team_name,role) VALUES (?,?,?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, teamName);
            ps.setString(3, role.name());
            if (mysql) { ps.setString(4, teamName); ps.setString(5, role.name()); }
            ps.executeUpdate();
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Team] Failed to save member: " + e.getMessage());
        }
    }

    public void removeMember(UUID uuid) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM " + prefix() + "team_members WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Team] Failed to remove member: " + e.getMessage());
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }
}
