package me.vennlmao.ariscore.home.managers;

import me.vennlmao.ariscore.home.HomeModule;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class HomeManager {

    private final HomeModule plugin;
    private final DatabaseManager db;

    public HomeManager(HomeModule plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
    }

    public int getMaxHomes(Player player) {
        Map<String, Object> slots = plugin.getConfig().getConfigurationSection("permission-slots") != null
                ? plugin.getConfig().getConfigurationSection("permission-slots").getValues(false)
                : Map.of();

        int max = plugin.getConfig().getInt("max_homes_default", 1);
        for (Map.Entry<String, Object> entry : slots.entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                int val = (entry.getValue() instanceof Integer) ? (int) entry.getValue() : Integer.parseInt(entry.getValue().toString());
                if (val > max) max = val;
            }
        }
        return max;
    }

    public List<String> getHomes(Player player) {
        return db.getHomes(player.getUniqueId());
    }

    public boolean homeExists(Player player, String name) {
        return getHomes(player).stream().anyMatch(h -> h.equalsIgnoreCase(name));
    }

    public Location getHome(Player player, String name) {
        return db.getHome(player.getUniqueId(), name);
    }

    public boolean setHome(Player player, String name) {
        List<String> blockedWorlds = plugin.getConfig().getStringList("blocked_worlds");
        if (blockedWorlds.contains(player.getWorld().getName())) return false;
        db.setHome(player.getUniqueId(), name, player.getLocation());
        return true;
    }

    public void deleteHome(Player player, String name) {
        db.deleteHome(player.getUniqueId(), name);
    }

    public void teleportHome(Player player, String name) {
        Location loc = getHome(player, name);
        if (loc == null) return;
        player.teleportAsync(loc);
    }
}
