package me.vennlmao.ariscore.team.managers;

import org.bukkit.Location;

import java.util.*;

public class TeamData {

    public enum Role { OWNER, CO_OWNER, MEMBER }

    public static class MemberData {
        public final UUID uuid;
        public Role role;
        public long joinDate;
        public boolean permEditHome = false;
        public boolean permKick = false;
        public boolean permManageTeammates = false;
        public boolean permPvpToggle = false;
        public boolean permVisitHome = true;
        public boolean permTeamChat = true;
        public boolean permInvite = false;

        public MemberData(UUID uuid, Role role, long joinDate) {
            this.uuid = uuid;
            this.role = role;
            this.joinDate = joinDate;
        }
    }

    private final String name;
    private final Map<UUID, MemberData> members = new LinkedHashMap<>();
    private Location home;
    private boolean pvpEnabled = false;

    public TeamData(String name) { this.name = name; }

    public String getName() { return name; }
    public Map<UUID, MemberData> getMembers() { return members; }
    public void addMember(UUID uuid, Role role) {
        members.put(uuid, new MemberData(uuid, role, System.currentTimeMillis()));
    }
    public void addMember(MemberData data) { members.put(data.uuid, data); }
    public void removeMember(UUID uuid) { members.remove(uuid); }
    public MemberData getMemberData(UUID uuid) { return members.get(uuid); }
    public Role getRole(UUID uuid) { MemberData d = members.get(uuid); return d != null ? d.role : null; }
    public boolean isMember(UUID uuid) { return members.containsKey(uuid); }
    public boolean isOwner(UUID uuid) { Role r = getRole(uuid); return r == Role.OWNER; }
    public boolean isCoOwner(UUID uuid) { Role r = getRole(uuid); return r == Role.CO_OWNER; }
    public boolean isOwnerOrCoOwner(UUID uuid) { Role r = getRole(uuid); return r == Role.OWNER || r == Role.CO_OWNER; }
    public UUID getOwner() {
        return members.values().stream().filter(m -> m.role == Role.OWNER).map(m -> m.uuid).findFirst().orElse(null);
    }
    public int getMemberCount() { return members.size(); }
    public Location getHome() { return home; }
    public void setHome(Location home) { this.home = home; }
    public void removeHome() { this.home = null; }
    public boolean isPvpEnabled() { return pvpEnabled; }
    public void setPvpEnabled(boolean pvpEnabled) { this.pvpEnabled = pvpEnabled; }
    public String getRoleString(UUID uuid) {
        Role r = getRole(uuid);
        if (r == null) return "None";
        return switch (r) { case OWNER -> "Owner"; case CO_OWNER -> "Co-Owner"; case MEMBER -> "Member"; };
    }
    }
    
