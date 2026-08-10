package me.vennlmao.ariscore.spawners.managers;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.spawners.SpawnersModule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnerManager {

    private final SpawnersModule module;
    private final SpawnerDatabaseManager db;
    private final Map<String, SpawnerData> cache = new ConcurrentHashMap<>();

    public SpawnerManager(SpawnersModule module, SpawnerDatabaseManager db) {
        this.module = module;
        this.db = db;
    }

    public void loadAll() {
        cache.clear();
        for (SpawnerData data : db.loadAll()) {
            cache.put(data.key(), data);
        }
        module.getPlugin().getLogger().info("[Spawners] Loaded " + cache.size() + " spawners.");
    }

    public void saveAllSync() {
        for (SpawnerData data : cache.values()) {
            db.save(data);
        }
    }

    public void saveDirty() {
        for (SpawnerData data : cache.values()) {
            if (data.isDirty()) {
                db.save(data);
                data.clearDirty();
            }
        }
    }

    public String key(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    public SpawnerData get(Location loc) {
        return cache.get(key(loc));
    }

    public Map<String, SpawnerData> getAll() {
        return cache;
    }

    public SpawnerData register(Location loc, EntityType type, long amount, Player owner) {
        SpawnerData data = new SpawnerData(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                type, amount, owner != null ? owner.getUniqueId() : null);
        cache.put(data.key(), data);
        db.save(data);
        return data;
    }

    public void unregister(SpawnerData data) {
        cache.remove(data.key());
        db.delete(data);
    }

    public boolean isMaxStack(SpawnerData data, long addAmount) {
        long max = module.getConfig().getLong("max-stack-amount", 5000);
        return data.getAmount() + addAmount > max;
    }

    /**
     * Isolation bonus multiplier: spawners placed far from other spawners produce faster.
     * Every neighbor within the configured radius reduces the multiplier.
     */
    public double getIsolationMultiplier(SpawnerData data) {
        if (!module.getConfig().getBoolean("isolation.enabled", true)) return 1.0;

        double radius = module.getConfig().getDouble("isolation.radius", 10);
        double penaltyPerNeighbor = module.getConfig().getDouble("isolation.penalty-per-neighbor", 0.12);
        double minMultiplier = module.getConfig().getDouble("isolation.min-multiplier", 0.25);
        double baseMultiplier = module.getConfig().getDouble("isolation.base-multiplier", 1.0);

        int neighbors = 0;
        for (SpawnerData other : cache.values()) {
            if (other == data) continue;
            if (!other.getWorld().equals(data.getWorld())) continue;
            double dx = other.getX() - data.getX();
            double dy = other.getY() - data.getY();
            double dz = other.getZ() - data.getZ();
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq <= radius * radius) neighbors++;
        }

        double multiplier = baseMultiplier - (neighbors * penaltyPerNeighbor);
        return Math.max(minMultiplier, multiplier);
    }

    public long sellPrice(Material material) {
        ArisCore core = (ArisCore) module.getPlugin();
        if (core.getSellModule() == null || core.getSellModule().getPriceManager() == null) return 0;
        double price = core.getSellModule().getPriceManager().getPrice(material);
        return (long) price;
    }

    private double sellPriceExact(Material material) {
        ArisCore core = (ArisCore) module.getPlugin();
        if (core.getSellModule() == null || core.getSellModule().getPriceManager() == null) return 0;
        return core.getSellModule().getPriceManager().getPrice(material);
    }

    public double totalStorageValue(SpawnerData data) {
        double total = 0;
        for (Map.Entry<Material, Long> entry : data.getStorage().entrySet()) {
            total += sellPriceExact(entry.getKey()) * entry.getValue();
        }
        return total;
    }

    /** Sells the entire storage of a spawner through the Sell module's economy. Returns total earned. */
    public double sellAll(Player player, SpawnerData data) {
        double total = 0;
        Map<Material, Long> snapshot = new HashMap<>(data.getStorage());
        for (Map.Entry<Material, Long> entry : snapshot.entrySet()) {
            double price = sellPriceExact(entry.getKey());
            if (price <= 0) continue;
            total += price * entry.getValue();
        }
        if (total <= 0) return 0;

        data.clearStorage();
        db.save(data);

        ArisCore core = (ArisCore) module.getPlugin();
        if (core.getSellModule() != null && core.getSellModule().getEconomy() != null) {
            core.getSellModule().getEconomy().depositPlayer(player, total);
        }
        return total;
    }

    /** Drops the entire storage of a spawner on the ground at the player's location. */
    public void dropAll(Player player, SpawnerData data) {
        Map<Material, Long> snapshot = new HashMap<>(data.getStorage());
        for (Map.Entry<Material, Long> entry : snapshot.entrySet()) {
            Material mat = entry.getKey();
            long remaining = entry.getValue();
            int maxStack = mat.getMaxStackSize();
            while (remaining > 0) {
                int give = (int) Math.min(remaining, maxStack);
                player.getWorld().dropItemNaturally(player.getLocation(), new org.bukkit.inventory.ItemStack(mat, give));
                remaining -= give;
            }
        }
        data.clearStorage();
        db.save(data);
    }

    public void collectXp(Player player, SpawnerData data) {
        long xp = data.getStoredXp();
        if (xp <= 0) return;
        int give = (int) Math.min(xp, Integer.MAX_VALUE);
        player.giveExp(give);
        data.clearXp();
        db.save(data);
    }

    public void addXpCapped(SpawnerData data, long xp) {
        long max = module.getConfig().getLong("xp.max-amount", 1000000);
        long current = data.getStoredXp();
        long space = max - current;
        if (space <= 0) return;
        data.addXp(Math.min(xp, space));
    }

    public void saveNow(SpawnerData data) {
        db.save(data);
        data.clearDirty();
    }
}
