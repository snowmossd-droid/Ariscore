package me.vennlmao.ariscore.team.listeners;

import me.vennlmao.ariscore.team.TeamModule;
import me.vennlmao.ariscore.team.utils.MessageUtil;
import me.vennlmao.ariscore.team.utils.SoundUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class TeamMoveListener implements Listener {

    private final TeamModule module;

    public TeamMoveListener(TeamModule module) {
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
            SoundUtil.play(player, "error");
        }
    }
}
