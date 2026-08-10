package me.vennlmao.ariscore.rtp.listeners;

import me.vennlmao.ariscore.rtp.RtpModule;
import me.vennlmao.ariscore.rtp.utils.MessageUtil;
import me.vennlmao.ariscore.rtp.utils.SoundUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class DamageListener implements Listener {

    private final RtpModule plugin;

    public DamageListener(RtpModule plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!plugin.getWarmupManager().isInWarmup(player)) return;

        plugin.getWarmupManager().cancelWarmup(player.getUniqueId());
        MessageUtil.sendChatList(player, "teleport_cancelled_damaged");
        MessageUtil.sendActionbar(player, "teleport_cancelled_damaged_ab");
        SoundUtil.play(player, "damaged");
    }
}
