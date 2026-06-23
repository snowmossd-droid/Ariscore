package me.vennlmao.ariscore.shop.managers;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.shop.ShopModule;
import org.bukkit.entity.Player;

public class ShardsManager {

    private final ShopModule module;

    public ShardsManager(ShopModule module) {
        this.module = module;
    }

    public double getShards(Player player) {
        return (double) ArisCore.getInstance().getShardsModule().getShardsManager().getShards(player);
    }

    public void takeShards(Player player, int amount) {
        ArisCore.getInstance().getShardsModule().getShardsManager().takeShards(player, (long) amount);
    }
}
