package me.vennlmao.ariscore.tab.listeners;

import me.vennlmao.ariscore.tab.TabModule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class TabListener implements Listener {

    private final TabModule module;

    public TabListener(TabModule module) {
        this.module = module;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        module.getPlugin().getServer().getGlobalRegionScheduler().runDelayed(module.getPlugin(), t -> {
            if (!event.getPlayer().isOnline()) return;
            module.getScoreboardManager().startFor(event.getPlayer());
            module.getTabManager().onPlayerJoin(event.getPlayer());
        }, 2L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        module.getScoreboardManager().stopFor(event.getPlayer());
        module.getTabManager().onPlayerQuit(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        module.getScoreboardManager().refreshWorld(event.getPlayer());
    }
}
