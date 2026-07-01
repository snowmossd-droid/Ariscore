package me.vennlmao.ariscore.warp.managers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.vennlmao.ariscore.warp.WarpModule;
import me.vennlmao.ariscore.warp.utils.MessageUtil;
import me.vennlmao.ariscore.warp.utils.SoundUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WarpWarmupManager {

    private final WarpModule module;
    private final Map<UUID, ScheduledTask> tasks         = new HashMap<>();
    private final Map<UUID, Integer>       countdowns    = new HashMap<>();
    private final Map<UUID, Location>      startLocs     = new HashMap<>();
    private final Map<UUID, Location>      targetLocs    = new HashMap<>();
    private final Map<UUID, String>        targetNames   = new HashMap<>();

    public WarpWarmupManager(WarpModule module) { this.module = module; }

    public void startWarmup(Player player, String warpName, Location target) {
        UUID id = player.getUniqueId();
        int seconds = module.getConfig().getInt("warmup", 5);

        countdowns.put(id, seconds);
        startLocs.put(id, player.getLocation().clone());
        targetLocs.put(id, target);
        targetNames.put(id, warpName);

        sendCountdown(player, warpName, seconds);

        ScheduledTask task = player.getScheduler().runAtFixedRate(module.getPlugin(), t -> {
            if (!player.isOnline()) { cancelWarmup(id); return; }

            int remaining = countdowns.getOrDefault(id, 0) - 1;
            if (remaining <= 0) {
                Location dest = targetLocs.remove(id);
                String name   = targetNames.remove(id);
                cancelWarmup(id);
                if (dest == null || dest.getWorld() == null) {
                    MessageUtil.sendChat(player, "world_not_found");
                    MessageUtil.sendActionbar(player, "world_not_found_ab");
                    return;
                }
                String displayName = (name != null && !name.isEmpty())
                        ? name : module.getConfig().getString("default-name", "warp");
                player.teleportAsync(dest);
                MessageUtil.sendChat(player, "teleport_success", s -> s.replace("%name%", displayName));
                MessageUtil.sendActionbar(player, "teleport_success_ab", s -> s.replace("%name%", displayName));
                SoundUtil.play(player, "teleport_success");
                return;
            }

            countdowns.put(id, remaining);
            sendCountdown(player, warpName, remaining);

        }, null, 20L, 20L);

        tasks.put(id, task);
    }

    private void sendCountdown(Player player, String name, int seconds) {
        MessageUtil.sendChat(player, "teleporting_chat",
                s -> s.replace("%name%", name).replace("{seconds}", String.valueOf(seconds)));
        MessageUtil.sendActionbar(player, "teleporting_ab",
                s -> s.replace("%name%", name).replace("{seconds}", String.valueOf(seconds)));
        SoundUtil.play(player, "countdown");
    }

    public void cancelWarmup(UUID id) {
        ScheduledTask t = tasks.remove(id);
        if (t != null) t.cancel();
        countdowns.remove(id);
        startLocs.remove(id);
        targetLocs.remove(id);
        targetNames.remove(id);
    }

    public boolean isInWarmup(Player player) { return tasks.containsKey(player.getUniqueId()); }
    public Location getStartLocation(Player player) { return startLocs.get(player.getUniqueId()); }

    public void cancelAll() {
        tasks.values().forEach(ScheduledTask::cancel);
        tasks.clear(); countdowns.clear(); startLocs.clear(); targetLocs.clear(); targetNames.clear();
    }
}
