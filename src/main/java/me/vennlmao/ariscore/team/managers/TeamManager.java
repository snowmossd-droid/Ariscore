package me.vennlmao.ariscore.team.managers;

import me.vennlmao.ariscore.team.TeamModule;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class TeamManager {

    private final TeamModule module;
    private final TeamDatabaseManager db;
    private final Map<String, TeamData> teams = new HashMap<>();
    private final Map<UUID, String> playerTeam = new HashMap<>();
    private final Map<UUID, Set<UUID>> pendingInvites = new HashMap<>();

    public TeamManager(TeamModule module, TeamDatabaseManager db) {
        this.module = module;
        this.db = db;
        load();
    }

    private void load() {
        Map<String, TeamData> loaded = db.loadAllTeams();
        teams.putAll(loaded);
        for (TeamData team : loaded.values())
            for (UUID uuid : team.getMembers().keySet())
                playerTeam.put(uuid, team.getName());
    }

    public boolean createTeam(Player owner, String name) {
        if (teams.containsKey(name.toLowerCase())) return false;
        if (playerTeam.containsKey(owner.getUniqueId())) return false;
        TeamData team = new TeamData(name);
        TeamData.MemberData md = new TeamData.MemberData(owner.getUniqueId(), TeamData.Role.OWNER, System.currentTimeMillis());
        md.permEditHome = true; md.permKick = true; md.permManageTeammates = true;
        md.permPvpToggle = true; md.permVisitHome = true; md.permTeamChat = true; md.permInvite = true;
        team.addMember(md);
        teams.put(name.toLowerCase(), team);
        playerTeam.put(owner.getUniqueId(), name);
        db.saveTeam(team);
        db.saveMember(md, name);
        return true;
    }

    public void disbandTeam(String name) {
        TeamData team = getTeam(name);
        if (team == null) return;
        for (UUID uuid : team.getMembers().keySet()) { playerTeam.remove(uuid); db.removeMember(uuid); }
        teams.remove(name.toLowerCase());
        db.deleteTeam(name);
    }

    public boolean addMember(String teamName, Player player) {
        TeamData team = getTeam(teamName);
        if (team == null) return false;
        if (team.getMemberCount() >= module.getConfig().getInt("team.max-members", 45)) return false;
        TeamData.MemberData md = new TeamData.MemberData(player.getUniqueId(), TeamData.Role.MEMBER, System.currentTimeMillis());
        md.permVisitHome = true; md.permTeamChat = true;
        team.addMember(md);
        playerTeam.put(player.getUniqueId(), teamName);
        db.saveMember(md, teamName);
        return true;
    }

    public void removeMember(UUID uuid) {
        String teamName = playerTeam.remove(uuid);
        if (teamName == null) return;
        TeamData team = getTeam(teamName);
        if (team == null) return;
        team.removeMember(uuid);
        db.removeMember(uuid);
    }

    public void saveMemberPerms(TeamData.MemberData md, String teamName) {
        db.saveMember(md, teamName);
    }

    public void setHome(String teamName, Location loc) {
        TeamData team = getTeam(teamName);
        if (team == null) return;
        team.setHome(loc);
        db.saveTeam(team);
    }

    public void deleteHome(String teamName) {
        TeamData team = getTeam(teamName);
        if (team == null) return;
        team.removeHome();
        db.saveTeam(team);
    }

    public void setPvp(String teamName, boolean enabled) {
        TeamData team = getTeam(teamName);
        if (team == null) return;
        team.setPvpEnabled(enabled);
        db.saveTeam(team);
    }

    public TeamData getTeam(String name) { return teams.get(name.toLowerCase()); }
    public TeamData getPlayerTeam(UUID uuid) { String n = playerTeam.get(uuid); return n != null ? getTeam(n) : null; }
    public String getPlayerTeamName(UUID uuid) { return playerTeam.get(uuid); }
    public boolean hasTeam(UUID uuid) { return playerTeam.containsKey(uuid); }
    public boolean teamExists(String name) { return teams.containsKey(name.toLowerCase()); }
    public Collection<TeamData> getAllTeams() { return teams.values(); }

    public void addInvite(UUID invited, UUID inviter) {
        pendingInvites.computeIfAbsent(invited, k -> new HashSet<>()).add(inviter);
    }

    public boolean hasInvite(UUID uuid, String teamName) {
        Set<UUID> invites = pendingInvites.get(uuid);
        if (invites == null) return false;
        return invites.stream().anyMatch(o -> { TeamData t = getPlayerTeam(o); return t != null && t.getName().equalsIgnoreCase(teamName); });
    }

    public boolean hasAnyInvite(UUID uuid) {
        Set<UUID> invites = pendingInvites.get(uuid);
        return invites != null && !invites.isEmpty();
    }

    public String getInviteTeam(UUID uuid) {
        Set<UUID> invites = pendingInvites.get(uuid);
        if (invites == null || invites.isEmpty()) return null;
        UUID o = invites.iterator().next();
        TeamData t = getPlayerTeam(o);
        return t != null ? t.getName() : null;
    }

    public void removeInvite(UUID uuid) { pendingInvites.remove(uuid); }
}
