package me.vennlmao.ariscore.sell.listeners;

import me.vennlmao.ariscore.sell.SellModule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class QuitListener implements Listener {

    private final SellModule module;

    public QuitListener(SellModule module) {
        this.module = module;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        module.getDataManager().clearCache(event.getPlayer().getUniqueId());
    }
}
