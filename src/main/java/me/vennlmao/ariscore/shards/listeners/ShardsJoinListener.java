package me.vennlmao.ariscore.shards.listeners;

import me.vennlmao.ariscore.shards.ShardsModule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ShardsJoinListener implements Listener {

    private final ShardsModule module;

    public ShardsJoinListener(ShardsModule module) {
        this.module = module;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        module.getAutoShardsManager().addPlayer(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        module.getAutoShardsManager().removePlayer(event.getPlayer());
    }
}
