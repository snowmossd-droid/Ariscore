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
        org.bukkit.configuration.ConfigurationSection section =
                plugin.getConfig().getConfigurationSection("permission-slots");
        int defaultMax = plugin.getConfig().getInt("max_homes_default", 1);

        // Check explicit permission-slots from config
        int max = defaultMax;
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Object val = section.get(key);
                if (!(val instanceof Number)) continue;
                int amount = ((Number) val).intValue();
                if (player.hasPermission(key) && amount > max) {
                    max = amount;
                }
            }
        }

        // Fallback: scan arishomes.homes.<N> permission nodes directly (1-100)
        // This handles LuckPerms nodes not declared in plugin.yml
        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission("arishomes.homes." + i)) {
                if (i > max) max = i;
                break;
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
