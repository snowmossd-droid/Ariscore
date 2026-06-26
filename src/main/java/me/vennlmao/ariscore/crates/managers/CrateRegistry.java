package me.vennlmao.ariscore.crates.managers;

import me.vennlmao.ariscore.crates.models.CrateModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CrateRegistry {

    private final Map<String, CrateModel> registry = new HashMap<>();

    public void cache(CrateModel crateModel) {
        registry.put(crateModel.getName(), crateModel);
    }

    public CrateModel find(String name) {
        return registry.get(name);
    }

    public Collection<CrateModel> values() {
        return registry.values();
    }

    public void clear() {
        registry.clear();
    }
}
