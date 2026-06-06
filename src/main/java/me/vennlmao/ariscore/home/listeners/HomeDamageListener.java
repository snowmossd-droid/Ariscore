package me.vennlmao.ariscore.home.listeners;

import me.vennlmao.ariscore.home.HomeModule;
import me.vennlmao.ariscore.home.utils.MessageUtil;
import me.vennlmao.ariscore.home.utils.SoundUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class HomeDamageListener implements Listener {

    private final HomeModule module;

    public HomeDamageListener(HomeModule module) {
        this.module = module;
    }

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
