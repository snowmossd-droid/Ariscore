package me.vennlmao.ariscore.crates.models;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GamerModel {

    private final UUID uniqueId;
    private final Map<String, Integer> keys;

    public GamerModel(UUID uniqueId) {
        this.uniqueId = uniqueId;
        this.keys = new HashMap<>();
    }

    public GamerModel(UUID uniqueId, Map<String, Integer> keys) {
        this.uniqueId = uniqueId;
        this.keys = new HashMap<>(keys);
    }

    public UUID getUniqueId() { return uniqueId; }

    public int getKeyAmount(String crateName) {
        return keys.getOrDefault(crateName, 0);
    }

    public void addKeyAmount(String crateName, int amount) {
        keys.merge(crateName, amount, Integer::sum);
    }

    public void removeKeyAmount(String crateName, int amount) {
        int current = keys.getOrDefault(crateName, 0);
        keys.put(crateName, Math.max(0, current - amount));
    }

    public Map<String, Integer> getKeys() { return keys; }
}
