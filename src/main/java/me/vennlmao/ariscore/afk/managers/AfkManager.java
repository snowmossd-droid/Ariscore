package me.vennlmao.ariscore.afk.managers;

import me.vennlmao.ariscore.afk.AfkModule;
import org.bukkit.Location;

import java.util.*;

public class AfkManager {

    private final AfkModule module;
    private final AfkDatabaseManager db;
    private final Map<String, Location> cache = new LinkedHashMap<>();

    public AfkManager(AfkModule module, AfkDatabaseManager db) {
        this.module = module;
        this.db = db;
        cache.putAll(db.getAllZones());
    }

    public void setZone(String name, Location loc) {
        db.setZone(name, loc);
        cache.put(name, loc);
    }

    public Location getZone(String name) {
        return cache.get(name);
    }

    public boolean zoneExists(String name) {
        return cache.containsKey(name);
    }

    public Map<String, Location> getAllZones() {
        return Collections.unmodifiableMap(cache);
    }

    public List<String> getZoneNames() {
        return new ArrayList<>(cache.keySet());
    }

    public boolean deleteZone(String name) {
        if (!cache.containsKey(name)) return false;
        db.deleteZone(name);
        cache.remove(name);
        return true;
    }

    public Location getRandomZone() {
        if (cache.isEmpty()) return null;
        List<Location> locs = new ArrayList<>(cache.values());
        return locs.get(new Random().nextInt(locs.size()));
    }
}
