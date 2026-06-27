package me.vennlmao.ariscore.warp.listeners;

import me.vennlmao.ariscore.warp.WarpModule;
import me.vennlmao.ariscore.warp.utils.MessageUtil;
import me.vennlmao.ariscore.warp.utils.SoundUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class WarpDamageListener implements Listener {

    private final WarpModule module;

    public WarpDamageListener(WarpModule module) { this.module = module; }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!module.getWarmupManager().isInWarmup(player)) return;
        module.getWarmupManager().cancelWarmup(player.getUniqueId());
        MessageUtil.sendChat(player, "teleport_cancelled_damaged");
        MessageUtil.sendActionbar(player, "teleport_cancelled_damaged_ab");
        SoundUtil.play(player, "damaged");
    }
}
