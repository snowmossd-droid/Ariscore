package me.vennlmao.ariscore.sell.managers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SearchManager {

    public enum SearchTarget { HISTORY, WORTH }

    private final Map<UUID, SearchTarget> searchingPlayers = new ConcurrentHashMap<>();

    public void startSearching(UUID uuid, SearchTarget target) {
        searchingPlayers.put(uuid, target);
    }

    public boolean isSearching(UUID uuid) {
        return searchingPlayers.containsKey(uuid);
    }

    public SearchTarget stopSearching(UUID uuid) {
        return searchingPlayers.remove(uuid);
    }
}
