package me.vennlmao.ariscore.auction.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private final Map<UUID, Boolean> fastSell = new HashMap<>();
    private final Map<UUID, Boolean> fastBuy = new HashMap<>();

    public boolean getFastSell(UUID uuid) { return fastSell.getOrDefault(uuid, false); }
    public void setFastSell(UUID uuid, boolean value) { fastSell.put(uuid, value); }

    public boolean getFastBuy(UUID uuid) { return fastBuy.getOrDefault(uuid, false); }
    public void setFastBuy(UUID uuid, boolean value) { fastBuy.put(uuid, value); }

    public void closeConnection() {}
}
