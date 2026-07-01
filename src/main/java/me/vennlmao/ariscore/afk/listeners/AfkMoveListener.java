package me.vennlmao.ariscore.afk.listeners;

import me.vennlmao.ariscore.afk.AfkModule;
import me.vennlmao.ariscore.afk.utils.MessageUtil;
import me.vennlmao.ariscore.afk.utils.SoundUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class AfkMoveListener implements Listener {

    private final AfkModule module;

    public AfkMoveListener(AfkModule module) {
        this.module = module;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!module.getWarmupManager().isInWarmup(player)) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        double distance = module.getConfig().getDouble("cancel_distance", 0.1);
        if (from.distance(to) >= distance) {
            module.getWarmupManager().cancelWarmup(player.getUniqueId());
            MessageUtil.sendChat(player, "teleport_cancelled_moved");
            MessageUtil.sendActionbar(player, "teleport_cancelled_moved_ab");
            SoundUtil.play(player, "cancel");
        }
    }
}
