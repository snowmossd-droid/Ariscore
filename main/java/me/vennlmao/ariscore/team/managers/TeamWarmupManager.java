package me.vennlmao.ariscore.team.managers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.vennlmao.ariscore.team.TeamModule;
import me.vennlmao.ariscore.team.utils.MessageUtil;
import me.vennlmao.ariscore.team.utils.SoundUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeamWarmupManager {

    private final TeamModule module;
    private final Map<UUID, ScheduledTask> tasks = new HashMap<>();
    private final Map<UUID, Integer> countdowns = new HashMap<>();

    public TeamWarmupManager(TeamModule module) {
        this.module = module;
    }

    public void startWarmup(Player player, Location destination) {
        UUID id = player.getUniqueId();
        int seconds = module.getConfig().getInt("warmup", 5);

        countdowns.put(id, seconds);

        MessageUtil.sendChat(player, "teleporting_chat",
                s -> s.replace("{seconds}", String.valueOf(seconds)));
        MessageUtil.sendActionbar(player, "teleporting_ab",
                s -> s.replace("{seconds}", String.valueOf(seconds)));
        SoundUtil.play(player, "countdown");

        ScheduledTask task = player.getScheduler().runAtFixedRate(module.getPlugin(), t -> {
            if (!player.isOnline()) { cancelWarmup(id); return; }

            int remaining = countdowns.getOrDefault(id, 0) - 1;

            if (remaining <= 0) {
                cancelWarmup(id);
                player.teleportAsync(destination);
                MessageUtil.sendChat(player, "teleport_home");
                MessageUtil.sendActionbar(player, "teleport_home_ab");
                SoundUtil.play(player, "teleport");
                return;
            }

            countdowns.put(id, remaining);
            int rem = remaining;
            MessageUtil.sendActionbar(player, "teleporting_ab",
                    s -> s.replace("{seconds}", String.valueOf(rem)));
            SoundUtil.play(player, "countdown");

        }, null, 20L, 20L);

        tasks.put(id, task);
    }

    public void cancelWarmup(UUID id) {
        ScheduledTask t = tasks.remove(id);
        if (t != null) t.cancel();
        countdowns.remove(id);
    }

    public boolean isInWarmup(Player player) {
        return tasks.containsKey(player.getUniqueId());
    }

    public void cancelAll() {
        tasks.values().forEach(ScheduledTask::cancel);
        tasks.clear();
        countdowns.clear();
    }
}
