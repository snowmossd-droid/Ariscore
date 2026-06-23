package me.vennlmao.ariscore.sell.listeners;

import me.vennlmao.ariscore.sell.SellModule;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

public class JoinListener implements Listener {

    private final SellModule module;

    public JoinListener(SellModule module) {
        this.module = module;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getAsyncScheduler().runNow((Plugin) module.getPlugin(),
                task -> module.getDataManager().preloadData(event.getPlayer().getUniqueId()));
    }
}
