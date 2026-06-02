package me.vennlmao.ariscore.team.listeners;

import me.vennlmao.ariscore.team.TeamModule;
import me.vennlmao.ariscore.team.utils.MessageUtil;
import me.vennlmao.ariscore.team.utils.SoundUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class TeamDamageListener implements Listener {

    private final TeamModule module;

    public TeamDamageListener(TeamModule module) {
        this.module = module;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!module.getWarmupManager().isInWarmup(player)) return;
        module.getWarmupManager().cancelWarmup(player.getUniqueId());
        MessageUtil.sendChat(player, "teleport_cancelled_damaged");
        MessageUtil.sendActionbar(player, "teleport_cancelled_damaged_ab");
        SoundUtil.play(player, "error");
    }
}
