package me.vennlmao.ariscore.warp.managers;

import me.vennlmao.ariscore.warp.WarpModule;
import org.bukkit.Location;

import java.util.*;

public class WarpManager {

    private final WarpModule module;
    private final WarpDatabaseManager db;
    private final Map<String, Location> cache = new LinkedHashMap<>();

    public WarpManager(WarpModule module, WarpDatabaseManager db) {
        this.module = module;
        this.db = db;
        cache.putAll(db.getAllWarps());
    }

    public void setWarp(String name, Location loc) {
        db.setWarp(name, loc);
        cache.put(name, loc);
    }

    public boolean deleteWarp(String name) {
        if (!cache.containsKey(name)) return false;
        db.deleteWarp(name);
        cache.remove(name);
        return true;
    }

    public Location getWarp(String name) { return cache.get(name); }
    public boolean warpExists(String name) { return cache.containsKey(name); }
    public Map<String, Location> getAllWarps() { return Collections.unmodifiableMap(cache); }
    public List<String> getWarpNames() { return new ArrayList<>(cache.keySet()); }

    public Location getRandomWarp() {
        if (cache.isEmpty()) return null;
        List<Location> locs = new ArrayList<>(cache.values());
        return locs.get(new Random().nextInt(locs.size()));
    }
}
