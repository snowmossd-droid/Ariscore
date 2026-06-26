package me.vennlmao.ariscore.rtp.managers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.vennlmao.ariscore.rtp.RtpModule;
import me.vennlmao.ariscore.rtp.utils.MessageUtil;
import me.vennlmao.ariscore.rtp.utils.SoundUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WarmupManager {

    private final RtpModule plugin;
    private final Map<UUID, ScheduledTask> warmupTasks = new HashMap<>();
    private final Map<UUID, Location> warmupLocations = new HashMap<>();
    private final Map<UUID, Integer> warmupCountdowns = new HashMap<>();
    private final Map<UUID, Location> targetLocations = new HashMap<>();
    private final Map<UUID, String> targetWorlds = new HashMap<>();

    public WarmupManager(RtpModule plugin) {
        this.plugin = plugin;
    }

    public void startWarmup(Player player, Location destination, String worldName) {
        UUID id = player.getUniqueId();
        int seconds = plugin.getConfig().getInt("warmup", 5);

        warmupLocations.put(id, player.getLocation().clone());
        warmupCountdowns.put(id, seconds);
        targetLocations.put(id, destination);
        targetWorlds.put(id, worldName);

        sendCountdown(player, seconds);

        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin.getPlugin(), scheduledTask -> {
            if (!player.isOnline()) {
                cleanupMaps(id);
                scheduledTask.cancel();
                return;
            }

            int remaining = warmupCountdowns.getOrDefault(id, 0) - 1;

            if (remaining <= 0) {
                Location dest = targetLocations.remove(id);
                String world = targetWorlds.remove(id);
                warmupLocations.remove(id);
                warmupCountdowns.remove(id);
                warmupTasks.remove(id);
                scheduledTask.cancel();

                if (dest == null) return;

                player.teleportAsync(dest).thenAccept(success -> {
                    if (!success) return;
                    player.getScheduler().run(plugin.getPlugin(), t -> {
                        plugin.getCooldownManager().setCooldown(player.getUniqueId());
                        MessageUtil.sendChatList(player, "teleport_success",
                                s -> s.replace("{world}", world != null ? world : ""));
                        MessageUtil.sendActionbar(player, "teleport_success_ab");
                        SoundUtil.play(player, "teleport_success");
                    }, null);
                });
                return;
            }

            warmupCountdowns.put(id, remaining);
            sendCountdown(player, remaining);

        }, null, 20L, 20L);

        warmupTasks.put(id, task);
    }

    private void sendCountdown(Player player, int seconds) {
        MessageUtil.sendChatList(player, "warmup",
                s -> s.replace("{seconds}", String.valueOf(seconds)));
        MessageUtil.sendActionbar(player, "warmup_ab",
                s -> s.replace("{seconds}", String.valueOf(seconds)));
        SoundUtil.play(player, "countdown");
    }

    private void cleanupMaps(UUID id) {
        warmupLocations.remove(id);
        warmupCountdowns.remove(id);
        targetLocations.remove(id);
        targetWorlds.remove(id);
        warmupTasks.remove(id);
    }

    public void cancelWarmup(UUID id) {
        ScheduledTask task = warmupTasks.remove(id);
        if (task != null) task.cancel();
        warmupLocations.remove(id);
        warmupCountdowns.remove(id);
        targetLocations.remove(id);
        targetWorlds.remove(id);
    }

    public boolean isInWarmup(Player player) {
        return warmupTasks.containsKey(player.getUniqueId());
    }

    public Location getWarmupLocation(Player player) {
        return warmupLocations.get(player.getUniqueId());
    }

    public void cancelAll() {
        warmupTasks.values().forEach(ScheduledTask::cancel);
        warmupTasks.clear();
        warmupLocations.clear();
        warmupCountdowns.clear();
        targetLocations.clear();
        targetWorlds.clear();
    }
}
