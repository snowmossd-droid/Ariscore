package me.vennlmao.ariscore.team.managers;

import org.bukkit.Location;

import java.util.*;

public class TeamData {

    public enum Role { OWNER, CO_OWNER, MEMBER }

    private final String name;
    private final Map<UUID, Role> members = new LinkedHashMap<>();
    private Location home;

    public TeamData(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    public Map<UUID, Role> getMembers() { return members; }

    public void addMember(UUID uuid, Role role) { members.put(uuid, role); }

    public void removeMember(UUID uuid) { members.remove(uuid); }

    public Role getRole(UUID uuid) { return members.getOrDefault(uuid, null); }

    public boolean isMember(UUID uuid) { return members.containsKey(uuid); }

    public boolean isOwner(UUID uuid) { return members.get(uuid) == Role.OWNER; }

    public boolean isCoOwner(UUID uuid) { return members.get(uuid) == Role.CO_OWNER; }

    public boolean isOwnerOrCoOwner(UUID uuid) {
        Role r = members.get(uuid);
        return r == Role.OWNER || r == Role.CO_OWNER;
    }

    public UUID getOwner() {
        return members.entrySet().stream()
                .filter(e -> e.getValue() == Role.OWNER)
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }

    public int getMemberCount() { return members.size(); }

    public Location getHome() { return home; }

    public void setHome(Location home) { this.home = home; }

    public void removeHome() { this.home = null; }

    public String getRoleString(UUID uuid) {
        Role r = getRole(uuid);
        if (r == null) return "None";
        return switch (r) {
            case OWNER -> "Owner";
            case CO_OWNER -> "Co-Owner";
            case MEMBER -> "Member";
        };
    }
}
