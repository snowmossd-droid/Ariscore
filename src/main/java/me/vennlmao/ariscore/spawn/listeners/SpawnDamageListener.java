package me.vennlmao.ariscore.spawn.listeners;

import me.vennlmao.ariscore.spawn.SpawnModule;
import me.vennlmao.ariscore.spawn.utils.MessageUtil;
import me.vennlmao.ariscore.spawn.utils.SoundUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class SpawnDamageListener implements Listener {

    private final SpawnModule module;

    public SpawnDamageListener(SpawnModule module) {
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
