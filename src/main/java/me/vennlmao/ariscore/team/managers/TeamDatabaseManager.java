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

    public TeamDatabaseManager(TeamModule module) { this.module = module; }

    public void init() {
        mysql = module.getConfig().getBoolean("mysql.enabled", false);
        HikariConfig config = new HikariConfig();
        if (mysql) {
            config.setJdbcUrl("jdbc:mysql://" + module.getConfig().getString("mysql.host") + ":" +
                    module.getConfig().getInt("mysql.port") + "/" +
                    module.getConfig().getString("mysql.database") + "?useSSL=" +
                    module.getConfig().getBoolean("mysql.use-ssl") + "&autoReconnect=true");
            config.setUsername(module.getConfig().getString("mysql.username"));
            config.setPassword(module.getConfig().getString("mysql.password"));
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

    private String p() { return module.getConfig().getString("mysql.table-prefix", "ariscore_"); }

    private void createTables() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS " + p() + "teams (" +
                    "name VARCHAR(64) PRIMARY KEY," +
                    "home_world VARCHAR(64)," +
                    "home_x DOUBLE DEFAULT 0," +
                    "home_y DOUBLE DEFAULT 0," +
                    "home_z DOUBLE DEFAULT 0," +
                    "home_yaw FLOAT DEFAULT 0," +
                    "home_pitch FLOAT DEFAULT 0," +
                    "pvp_enabled TINYINT DEFAULT 0" +
                    ")");
            stmt.execute("CREATE TABLE IF NOT EXISTS " + p() + "team_members (" +
                    "uuid VARCHAR(36) PRIMARY KEY," +
                    "team_name VARCHAR(64) NOT NULL," +
                    "role VARCHAR(16) NOT NULL," +
                    "join_date BIGINT DEFAULT 0," +
                    "perm_edit_home TINYINT DEFAULT 0," +
                    "perm_kick TINYINT DEFAULT 0," +
                    "perm_manage_teammates TINYINT DEFAULT 0," +
                    "perm_pvp_toggle TINYINT DEFAULT 0," +
                    "perm_visit_home TINYINT DEFAULT 1," +
                    "perm_team_chat TINYINT DEFAULT 1," +
                    "perm_invite TINYINT DEFAULT 0" +
                    ")");
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Team] Tables error: " + e.getMessage());
        }
    }

    public Map<String, TeamData> loadAllTeams() {
        Map<String, TeamData> teams = new HashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM " + p() + "teams");
            while (rs.next()) {
                String name = rs.getString("name");
                TeamData team = new TeamData(name);
                team.setPvpEnabled(rs.getInt("pvp_enabled") == 1);
                String world = rs.getString("home_world");
                if (world != null) {
                    World w = Bukkit.getWorld(world);
                    if (w != null) team.setHome(new Location(w, rs.getDouble("home_x"), rs.getDouble("home_y"),
                            rs.getDouble("home_z"), rs.getFloat("home_yaw"), rs.getFloat("home_pitch")));
                }
                teams.put(name.toLowerCase(), team);
            }
            ResultSet mrs = conn.createStatement().executeQuery("SELECT * FROM " + p() + "team_members");
            while (mrs.next()) {
                TeamData team = teams.get(mrs.getString("team_name").toLowerCase());
                if (team == null) continue;
                TeamData.MemberData md = new TeamData.MemberData(
                        UUID.fromString(mrs.getString("uuid")),
                        TeamData.Role.valueOf(mrs.getString("role")),
                        mrs.getLong("join_date"));
                md.permEditHome = mrs.getInt("perm_edit_home") == 1;
                md.permKick = mrs.getInt("perm_kick") == 1;
                md.permManageTeammates = mrs.getInt("perm_manage_teammates") == 1;
                md.permPvpToggle = mrs.getInt("perm_pvp_toggle") == 1;
                md.permVisitHome = mrs.getInt("perm_visit_home") == 1;
                md.permTeamChat = mrs.getInt("perm_team_chat") == 1;
                md.permInvite = mrs.getInt("perm_invite") == 1;
                team.addMember(md);
            }
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Team] Load error: " + e.getMessage());
        }
        return teams;
    }

    public void saveTeam(TeamData team) {
        String sql = mysql
                ? "INSERT INTO " + p() + "teams (name,home_world,home_x,home_y,home_z,home_yaw,home_pitch,pvp_enabled) VALUES (?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE home_world=?,home_x=?,home_y=?,home_z=?,home_yaw=?,home_pitch=?,pvp_enabled=?"
                : "INSERT OR REPLACE INTO " + p() + "teams (name,home_world,home_x,home_y,home_z,home_yaw,home_pitch,pvp_enabled) VALUES (?,?,?,?,?,?,?,?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            Location h = team.getHome();
            String w = h != null && h.getWorld() != null ? h.getWorld().getName() : null;
            double x = h != null ? h.getX() : 0, y = h != null ? h.getY() : 0, z = h != null ? h.getZ() : 0;
            float yaw = h != null ? h.getYaw() : 0, pitch = h != null ? h.getPitch() : 0;
            int pvp = team.isPvpEnabled() ? 1 : 0;
            ps.setString(1, team.getName()); ps.setString(2, w);
            ps.setDouble(3, x); ps.setDouble(4, y); ps.setDouble(5, z);
            ps.setFloat(6, yaw); ps.setFloat(7, pitch); ps.setInt(8, pvp);
            if (mysql) {
                ps.setString(9, w); ps.setDouble(10, x); ps.setDouble(11, y); ps.setDouble(12, z);
                ps.setFloat(13, yaw); ps.setFloat(14, pitch); ps.setInt(15, pvp);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Team] Save team error: " + e.getMessage());
        }
    }

    public void deleteTeam(String name) {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement p1 = conn.prepareStatement("DELETE FROM " + p() + "teams WHERE name=?");
            p1.setString(1, name); p1.executeUpdate();
            PreparedStatement p2 = conn.prepareStatement("DELETE FROM " + p() + "team_members WHERE team_name=?");
            p2.setString(1, name); p2.executeUpdate();
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Team] Delete error: " + e.getMessage());
        }
    }

    public void saveMember(TeamData.MemberData md, String teamName) {
        String sql = mysql
                ? "INSERT INTO " + p() + "team_members (uuid,team_name,role,join_date,perm_edit_home,perm_kick,perm_manage_teammates,perm_pvp_toggle,perm_visit_home,perm_team_chat,perm_invite) VALUES (?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE team_name=?,role=?,join_date=?,perm_edit_home=?,perm_kick=?,perm_manage_teammates=?,perm_pvp_toggle=?,perm_visit_home=?,perm_team_chat=?,perm_invite=?"
                : "INSERT OR REPLACE INTO " + p() + "team_members (uuid,team_name,role,join_date,perm_edit_home,perm_kick,perm_manage_teammates,perm_pvp_toggle,perm_visit_home,perm_team_chat,perm_invite) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, md.uuid.toString()); ps.setString(2, teamName); ps.setString(3, md.role.name());
            ps.setLong(4, md.joinDate);
            ps.setInt(5, md.permEditHome ? 1 : 0); ps.setInt(6, md.permKick ? 1 : 0);
            ps.setInt(7, md.permManageTeammates ? 1 : 0); ps.setInt(8, md.permPvpToggle ? 1 : 0);
            ps.setInt(9, md.permVisitHome ? 1 : 0); ps.setInt(10, md.permTeamChat ? 1 : 0);
            ps.setInt(11, md.permInvite ? 1 : 0);
            if (mysql) {
                ps.setString(12, teamName); ps.setString(13, md.role.name());
                ps.setLong(14, md.joinDate);
                ps.setInt(15, md.permEditHome ? 1 : 0); ps.setInt(16, md.permKick ? 1 : 0);
                ps.setInt(17, md.permManageTeammates ? 1 : 0); ps.setInt(18, md.permPvpToggle ? 1 : 0);
                ps.setInt(19, md.permVisitHome ? 1 : 0); ps.setInt(20, md.permTeamChat ? 1 : 0);
                ps.setInt(21, md.permInvite ? 1 : 0);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Team] Save member error: " + e.getMessage());
        }
    }

    public void removeMember(UUID uuid) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM " + p() + "team_members WHERE uuid=?")) {
            ps.setString(1, uuid.toString()); ps.executeUpdate();
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Team] Remove member error: " + e.getMessage());
        }
    }

    public void close() { if (dataSource != null && !dataSource.isClosed()) dataSource.close(); }
}
