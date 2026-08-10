package me.vennlmao.ariscore.crates.managers;

import me.vennlmao.ariscore.crates.models.GamerModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GamerDataManager {

    private final Map<UUID, GamerModel> cache = new HashMap<>();

    public void cache(GamerModel gamerModel) {
        cache.put(gamerModel.getUniqueId(), gamerModel);
    }

    public void remove(UUID uuid) {
        cache.remove(uuid);
    }

    public GamerModel find(UUID uuid) {
        return cache.get(uuid);
    }

    public Collection<GamerModel> values() {
        return cache.values();
    }
}
