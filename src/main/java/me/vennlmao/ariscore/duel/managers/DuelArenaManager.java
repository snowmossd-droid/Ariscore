package me.vennlmao.ariscore.duel.managers;

import java.util.*;

public class DuelArenaManager {

    private final DuelArenaDatabaseManager db;
    private final Map<String, DuelArena> cache = new LinkedHashMap<>();
    private final Random random = new Random();

    public DuelArenaManager(DuelArenaDatabaseManager db) {
        this.db = db;
        for (Map.Entry<String, DuelArena> entry : db.getAllArenas().entrySet()) {
            cache.put(entry.getKey().toLowerCase(), entry.getValue());
        }
    }

    public void createArena(String name, org.bukkit.Location pos1, org.bukkit.Location pos2) {
        String key = name.toLowerCase();
        DuelArena arena = new DuelArena(key, pos1, pos2);
        db.saveArena(arena);
        cache.put(key, arena);
    }

    public boolean deleteArena(String name) {
        String key = name.toLowerCase();
        DuelArena removed = cache.remove(key);
        if (removed == null) return false;
        db.deleteArena(key);
        return true;
    }

    public boolean exists(String name) {
        return cache.containsKey(name.toLowerCase());
    }

    public DuelArena getArena(String name) {
        return cache.get(name.toLowerCase());
    }

    public List<String> getArenaNames() {
        return new ArrayList<>(cache.keySet());
    }

    public Collection<DuelArena> getArenas() {
        return Collections.unmodifiableCollection(cache.values());
    }

    public DuelArena getRandomArena() {
        if (cache.isEmpty()) return null;
        List<DuelArena> arenas = new ArrayList<>(cache.values());
        return arenas.get(random.nextInt(arenas.size()));
    }

    public boolean hasArenas() {
        return !cache.isEmpty();
    }
}
