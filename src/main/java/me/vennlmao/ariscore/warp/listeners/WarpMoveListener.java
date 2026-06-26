package me.vennlmao.ariscore.warp.listeners;

import me.vennlmao.ariscore.warp.WarpModule;
import me.vennlmao.ariscore.warp.utils.MessageUtil;
import me.vennlmao.ariscore.warp.utils.SoundUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class WarpMoveListener implements Listener {

    private final WarpModule module;

    public WarpMoveListener(WarpModule module) { this.module = module; }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!module.getWarmupManager().isInWarmup(player)) return;
        Location from = event.getFrom(), to = event.getTo();
        if (to == null) return;
        if (from.distance(to) >= module.getConfig().getDouble("cancel_distance", 0.1)) {
            module.getWarmupManager().cancelWarmup(player.getUniqueId());
            MessageUtil.sendChat(player, "teleport_cancelled_moved");
            MessageUtil.sendActionbar(player, "teleport_cancelled_moved_ab");
            SoundUtil.play(player, "cancel");
        }
    }
}
