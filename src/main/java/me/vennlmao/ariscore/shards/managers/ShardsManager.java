package me.vennlmao.ariscore.shards.managers;

import me.vennlmao.ariscore.shards.ShardsModule;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ShardsManager {

    private final ShardsModule module;
    private final ShardsDatabaseManager db;

    public ShardsManager(ShardsModule module, ShardsDatabaseManager db) {
        this.module = module;
        this.db = db;
    }

    public long getShards(UUID uuid) {
        return db.getShards(uuid);
    }

    public long getShards(Player player) {
        return db.getShards(player.getUniqueId());
    }

    public void addShards(Player player, long amount) {
        db.addShards(player.getUniqueId(), amount);
    }

    public void addShards(UUID uuid, long amount) {
        db.addShards(uuid, amount);
    }

    public boolean takeShards(Player player, long amount) {
        return db.takeShards(player.getUniqueId(), amount);
    }

    public boolean takeShards(UUID uuid, long amount) {
        return db.takeShards(uuid, amount);
    }

    public void setShards(UUID uuid, long amount) {
        db.setShards(uuid, amount);
    }

    public void resetShards(UUID uuid) {
        db.resetShards(uuid);
    }
}
