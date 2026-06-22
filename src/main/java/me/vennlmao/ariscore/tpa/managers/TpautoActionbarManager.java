package me.vennlmao.ariscore.tpa.managers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.vennlmao.ariscore.tpa.TpaModule;
import me.vennlmao.ariscore.tpa.utils.MessageUtil;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TpautoActionbarManager {

    private final TpaModule plugin;
    private final Map<UUID, ScheduledTask> tasks = new HashMap<>();

    public TpautoActionbarManager(TpaModule plugin) {
        this.plugin = plugin;
    }

    public void start(Player player) {
        stop(player);
        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin.getPlugin(), t -> {
            if (!player.isOnline()) {
                stop(player);
                return;
            }
            if (!plugin.getWarmupManager().isInWarmup(player)) {
                MessageUtil.sendActionbar(player, "tpauto_enabled_ab");
            }
        }, null, 1L, 40L);
        tasks.put(player.getUniqueId(), task);
    }

    public void stop(Player uuid) {
        ScheduledTask task = tasks.remove(uuid.getUniqueId());
        if (task != null) task.cancel();
    }

    public void cancelAll() {
        tasks.values().forEach(ScheduledTask::cancel);
        tasks.clear();
    }
}
