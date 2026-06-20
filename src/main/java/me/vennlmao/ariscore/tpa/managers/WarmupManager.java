package me.vennlmao.ariscore.tpa.managers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.vennlmao.ariscore.tpa.TpaModule;
import me.vennlmao.ariscore.tpa.utils.MessageUtil;
import me.vennlmao.ariscore.tpa.utils.SoundUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WarmupManager {

    private final TpaModule plugin;
    private final Map<UUID, ScheduledTask> warmupTasks = new HashMap<>();
    private final Map<UUID, Location> warmupLocations = new HashMap<>();
    private final Map<UUID, Integer> warmupCountdowns = new HashMap<>();

    public WarmupManager(TpaModule plugin) {
        this.plugin = plugin;
    }

    public void startWarmup(Player teleporter, Player destination, boolean toDestination) {
        UUID id = teleporter.getUniqueId();
        int seconds = plugin.getConfig().getInt("warmup", 5);

        warmupLocations.put(id, teleporter.getLocation().clone());
        warmupCountdowns.put(id, seconds);

        sendCountdown(teleporter, seconds);

        ScheduledTask task = teleporter.getScheduler().runAtFixedRate(plugin.getPlugin(), scheduledTask -> {
            if (!teleporter.isOnline()) { cancelWarmup(id); return; }

            int remaining = warmupCountdowns.getOrDefault(id, 0) - 1;

            if (remaining <= 0) {
                cancelWarmup(id);
                if (!destination.isOnline()) {
                    MessageUtil.sendChatList(teleporter, "target_offline");
                    MessageUtil.sendActionbar(teleporter, "target_offline_ab");
                    return;
                }
                if (toDestination) {
                    teleporter.teleportAsync(destination.getLocation());
                } else {
                    destination.teleportAsync(teleporter.getLocation());
                }
                MessageUtil.sendChatList(teleporter, "teleport_success");
                MessageUtil.sendActionbar(teleporter, "teleport_success_ab");
                SoundUtil.play(teleporter, "teleport_success");
                return;
            }

            warmupCountdowns.put(id, remaining);
            sendCountdown(teleporter, remaining);

        }, null, 20L, 20L);

        warmupTasks.put(id, task);
    }

    private void sendCountdown(Player player, int seconds) {
        MessageUtil.sendChatList(player, "teleporting_chat", s -> s.replace("{seconds}", String.valueOf(seconds)));
        MessageUtil.sendActionbar(player, "teleporting_ab", s -> s.replace("{seconds}", String.valueOf(seconds)));
        SoundUtil.play(player, "countdown");
    }

    public void cancelWarmup(UUID playerId) {
        ScheduledTask task = warmupTasks.remove(playerId);
        if (task != null) task.cancel();
        warmupLocations.remove(playerId);
        warmupCountdowns.remove(playerId);
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
    }
}
