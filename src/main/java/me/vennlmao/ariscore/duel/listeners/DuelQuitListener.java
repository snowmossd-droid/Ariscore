package me.vennlmao.ariscore.duel.listeners;

import me.vennlmao.ariscore.duel.DuelModule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class DuelQuitListener implements Listener {

    private final DuelModule module;

    public DuelQuitListener(DuelModule module) { this.module = module; }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        module.getSessionManager().handleDisconnect(event.getPlayer().getUniqueId());
        module.getGuiListener().clearPending(event.getPlayer().getUniqueId());
        module.getStatsManager().unload(event.getPlayer().getUniqueId());
    }
}
