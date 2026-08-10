package me.vennlmao.ariscore.spawners.managers;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * In-memory representation of a single placed (virtual) spawner block.
 * All production math and storage happens here; the physical block is only
 * used for placement/break/interact detection, never for real mob spawning.
 */
public class SpawnerData {

    private final String world;
    private final int x, y, z;
    private EntityType entityType;
    private long amount;          // how many spawners are stacked on this block
    private UUID owner;           // who placed it (informational only)
    private long storedXp;
    private final Map<Material, Long> storage = new HashMap<>();
    private int ticksUntilProduction;
    private boolean dirty;        // needs saving to DB

    public SpawnerData(String world, int x, int y, int z, EntityType entityType, long amount, UUID owner) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.entityType = entityType;
        this.amount = amount;
        this.owner = owner;
    }

    public String key() {
        return world + ":" + x + ":" + y + ":" + z;
    }

    public String getWorld() { return world; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }

    public EntityType getEntityType() { return entityType; }
    public void setEntityType(EntityType entityType) { this.entityType = entityType; markDirty(); }

    public long getAmount() { return amount; }
    public void setAmount(long amount) { this.amount = Math.max(0, amount); markDirty(); }
    public void addAmount(long add) { this.amount = Math.max(0, this.amount + add); markDirty(); }

    public UUID getOwner() { return owner; }
    public void setOwner(UUID owner) { this.owner = owner; markDirty(); }

    public long getStoredXp() { return storedXp; }
    public void addXp(long xp) { this.storedXp += xp; markDirty(); }
    public void clearXp() { this.storedXp = 0; markDirty(); }

    public Map<Material, Long> getStorage() { return storage; }

    public void addItem(Material material, long qty) {
        if (qty <= 0) return;
        storage.merge(material, qty, Long::sum);
        markDirty();
    }

    public long getStoredCount(Material material) {
        return storage.getOrDefault(material, 0L);
    }

    public void removeItem(Material material, long qty) {
        Long cur = storage.get(material);
        if (cur == null) return;
        long left = cur - qty;
        if (left <= 0) storage.remove(material);
        else storage.put(material, left);
        markDirty();
    }

    public void clearStorage() {
        storage.clear();
        markDirty();
    }

    public long totalStoredItems() {
        long total = 0;
        for (long v : storage.values()) total += v;
        return total;
    }

    public int getTicksUntilProduction() { return ticksUntilProduction; }
    public void setTicksUntilProduction(int t) { this.ticksUntilProduction = t; }

    public boolean isDirty() { return dirty; }
    public void markDirty() { this.dirty = true; }
    public void clearDirty() { this.dirty = false; }

    /** Serializes storage map to a compact "MAT:qty;MAT:qty" string for DB storage. */
    public String serializeStorage() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Material, Long> e : storage.entrySet()) {
            if (sb.length() > 0) sb.append(';');
            sb.append(e.getKey().name()).append(':').append(e.getValue());
        }
        return sb.toString();
    }

    public static void deserializeStorage(SpawnerData data, String raw) {
        if (raw == null || raw.isEmpty()) return;
        for (String part : raw.split(";")) {
            if (part.isEmpty()) continue;
            String[] kv = part.split(":");
            if (kv.length != 2) continue;
            try {
                Material mat = Material.valueOf(kv[0]);
                long qty = Long.parseLong(kv[1]);
                if (qty > 0) data.storage.put(mat, qty);
            } catch (IllegalArgumentException ignored) {}
        }
    }
}
