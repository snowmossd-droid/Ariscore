package me.vennlmao.ariscore.home.managers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.vennlmao.ariscore.home.HomeModule;
import me.vennlmao.ariscore.home.utils.MessageUtil;
import me.vennlmao.ariscore.home.utils.SoundUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HomeWarmupManager {

    private final HomeModule module;
    private final Map<UUID, ScheduledTask> tasks = new HashMap<>();
    private final Map<UUID, Integer> countdowns = new HashMap<>();
    private final Map<UUID, Location> startLocations = new HashMap<>();

    public HomeWarmupManager(HomeModule module) {
        this.module = module;
    }

    public void startWarmup(Player player, String homeName) {
        UUID id = player.getUniqueId();
        int seconds = module.getConfig().getInt("warmup", 3);

        countdowns.put(id, seconds);
        startLocations.put(id, player.getLocation().clone());

        sendCountdown(player, homeName, seconds);

        ScheduledTask task = player.getScheduler().runAtFixedRate(module.getPlugin(), t -> {
            if (!player.isOnline()) { cancelWarmup(id); return; }

            int remaining = countdowns.getOrDefault(id, 0) - 1;

            if (remaining <= 0) {
                cancelWarmup(id);
                Location loc = module.getHomeManager().getHome(player, homeName);
                if (loc == null || loc.getWorld() == null) {
                    MessageUtil.sendChat(player, "world_not_found");
                    MessageUtil.sendActionbar(player, "world_not_found_ab");
                    return;
                }
                player.teleportAsync(loc);
                MessageUtil.sendChat(player, "teleport_success", s -> s.replace("%home%", homeName));
                MessageUtil.sendActionbar(player, "teleport_success_ab", s -> s.replace("%home%", homeName));
                SoundUtil.play(player, "teleport_success");
                return;
            }

            countdowns.put(id, remaining);
            sendCountdown(player, homeName, remaining);

        }, null, 20L, 20L);

        tasks.put(id, task);
    }

    private void sendCountdown(Player player, String homeName, int seconds) {
        MessageUtil.sendChat(player, "teleporting_chat",
                s -> s.replace("%home%", homeName).replace("{seconds}", String.valueOf(seconds)));
        MessageUtil.sendActionbar(player, "teleporting_ab",
                s -> s.replace("%home%", homeName).replace("{seconds}", String.valueOf(seconds)));
        SoundUtil.play(player, "countdown");
    }

    public void cancelWarmup(UUID id) {
        ScheduledTask t = tasks.remove(id);
        if (t != null) t.cancel();
        countdowns.remove(id);
        startLocations.remove(id);
    }

    public boolean isInWarmup(Player player) {
        return tasks.containsKey(player.getUniqueId());
    }

    public Location getStartLocation(Player player) {
        return startLocations.get(player.getUniqueId());
    }

    public void cancelAll() {
        tasks.values().forEach(ScheduledTask::cancel);
        tasks.clear();
        countdowns.clear();
        startLocations.clear();
    }
}
