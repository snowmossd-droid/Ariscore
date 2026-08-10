package me.vennlmao.ariscore.duel.managers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DuelStatsManager {

    private final DuelStatsDatabaseManager db;
    private final Map<UUID, DuelStats> cache = new ConcurrentHashMap<>();

    public DuelStatsManager(DuelStatsDatabaseManager db) { this.db = db; }

    public DuelStats getStats(UUID uuid) {
        return cache.computeIfAbsent(uuid, db::loadStats);
    }

    public void recordWin(UUID uuid) {
        DuelStats stats = getStats(uuid);
        stats.addWin();
        db.saveStats(stats);
    }

    public void recordLoss(UUID uuid) {
        DuelStats stats = getStats(uuid);
        stats.addLoss();
        db.saveStats(stats);
    }

    public void recordDraw(UUID uuid) {
        DuelStats stats = getStats(uuid);
        stats.addDraw();
        db.saveStats(stats);
    }

    public void unload(UUID uuid) {
        cache.remove(uuid);
    }
}
