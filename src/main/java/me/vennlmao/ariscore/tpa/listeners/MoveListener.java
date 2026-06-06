package me.vennlmao.ariscore.tpa.listeners;

import me.vennlmao.ariscore.tpa.TpaModule;
import me.vennlmao.ariscore.tpa.utils.MessageUtil;
import me.vennlmao.ariscore.tpa.utils.SoundUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class MoveListener implements Listener {

    private final TpaModule plugin;

    public MoveListener(TpaModule plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getWarmupManager().isInWarmup(player)) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        double distance = plugin.getConfig().getDouble("cancel_distance", 0.1);
        if (from.distance(to) >= distance) {
            plugin.getWarmupManager().cancelWarmup(player.getUniqueId());
            MessageUtil.sendChatList(player, "teleport_cancelled_moved");
            MessageUtil.sendActionbar(player, "teleport_cancelled_moved_ab");
            SoundUtil.play(player, "cancel");
        }
    }
}
