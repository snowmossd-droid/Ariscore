package me.vennlmao.ariscore.auction.listeners;

import me.vennlmao.ariscore.ArisCore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class AuctionJoinListener implements Listener {

    private final ArisCore plugin;

    public AuctionJoinListener(ArisCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getAuctionModule().getAuctionManager().processPendingPayments(event.getPlayer());
    }
}
