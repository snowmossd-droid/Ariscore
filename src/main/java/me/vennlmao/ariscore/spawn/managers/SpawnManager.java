package me.vennlmao.ariscore.spawn.managers;

import me.vennlmao.ariscore.spawn.SpawnModule;
import org.bukkit.Location;

import java.util.*;

public class SpawnManager {

    private final SpawnModule module;
    private final SpawnDatabaseManager db;
    private final Map<String, Location> cache = new LinkedHashMap<>();

    public SpawnManager(SpawnModule module, SpawnDatabaseManager db) {
        this.module = module;
        this.db = db;
        cache.putAll(db.getAllSpawns());
    }

    public void setSpawn(String name, Location loc) {
        db.setSpawn(name, loc);
        cache.put(name, loc);
    }

    public Location getSpawn(String name) {
        return cache.get(name);
    }

    public boolean spawnExists(String name) {
        return cache.containsKey(name);
    }

    public Map<String, Location> getAllSpawns() {
        return Collections.unmodifiableMap(cache);
    }

    public List<String> getSpawnNames() {
        return new ArrayList<>(cache.keySet());
    }

    public boolean deleteSpawn(String name) {
        if (!cache.containsKey(name)) return false;
        db.deleteSpawn(name);
        cache.remove(name);
        return true;
    }

    public Location getRandomSpawn() {
        if (cache.isEmpty()) return null;
        List<Location> locs = new ArrayList<>(cache.values());
        return locs.get(new Random().nextInt(locs.size()));
    }
}
