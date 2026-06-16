package me.vennlmao.ariscore.shards.managers;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.vennlmao.ariscore.shards.ShardsModule;
import me.vennlmao.ariscore.shards.utils.ColorUtil;
import me.vennlmao.ariscore.shards.utils.SoundUtil;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AutoShardsManager {

    private final ShardsModule module;
    private final Map<UUID, Integer> autoCountdowns = new HashMap<>();
    private final Map<UUID, Integer> afkCountdowns = new HashMap<>();
    private ScheduledTask globalTask;

    public AutoShardsManager(ShardsModule module) {
        this.module = module;
    }

    public void start() {
        for (Player p : module.getPlugin().getServer().getOnlinePlayers()) {
            addPlayer(p);
        }
        globalTask = module.getPlugin().getServer().getGlobalRegionScheduler()
                .runAtFixedRate(module.getPlugin(), task -> tick(), 20L, 20L);
    }

    public void stop() {
        if (globalTask != null) globalTask.cancel();
        autoCountdowns.clear();
        afkCountdowns.clear();
    }

    public void addPlayer(Player player) {
        autoCountdowns.put(player.getUniqueId(), module.getConfig().getInt("auto-shards.interval"));
        afkCountdowns.put(player.getUniqueId(), module.getConfig().getInt("afk-shards.interval"));
    }

    public void removePlayer(Player player) {
        autoCountdowns.remove(player.getUniqueId());
        afkCountdowns.remove(player.getUniqueId());
    }

    private void tick() {
        for (Player player : module.getPlugin().getServer().getOnlinePlayers()) {
            tickAuto(player);
            tickAfk(player);
        }
    }

    private void tickAuto(Player player) {
        if (!module.getConfig().getBoolean("auto-shards.enabled")) return;
        if (!player.hasPermission(module.getConfig().getString("auto-shards.permission"))) return;
        if (isBlocked(player, "auto-shards")) return;

        UUID uuid = player.getUniqueId();
        int interval = module.getConfig().getInt("auto-shards.interval");
        int remaining = autoCountdowns.getOrDefault(uuid, interval) - 1;

        if (remaining <= 0) {
            autoCountdowns.put(uuid, interval);
            long amount = module.getConfig().getLong("auto-shards.amount");
            module.getShardsManager().addShards(player, amount);
            long total = module.getShardsManager().getShards(player);

            String msg = module.getConfig().getString("messages.auto_receive_ab")
                    .replace("{amount}", String.valueOf(amount))
                    .replace("{total}", String.valueOf(total));
            player.sendActionBar(ColorUtil.parse(msg));
            SoundUtil.play(player, "receive");
        } else {
            autoCountdowns.put(uuid, remaining);
        }
    }

    private void tickAfk(Player player) {
        if (!module.getConfig().getBoolean("afk-shards.enabled")) return;
        if (!player.hasPermission(module.getConfig().getString("afk-shards.permission"))) return;
        if (isBlocked(player, "afk-shards")) return;

        UUID uuid = player.getUniqueId();
        int interval = module.getConfig().getInt("afk-shards.interval");
        int remaining = afkCountdowns.getOrDefault(uuid, interval) - 1;

        if (remaining <= 0) {
            afkCountdowns.put(uuid, interval);
            long amount = module.getConfig().getLong("afk-shards.amount");
            module.getShardsManager().addShards(player, amount);
            long total = module.getShardsManager().getShards(player);

            String msg = module.getConfig().getString("messages.afk_receive_ab")
                    .replace("{amount}", String.valueOf(amount))
                    .replace("{total}", String.valueOf(total));
            player.sendActionBar(ColorUtil.parse(msg));
            SoundUtil.play(player, "receive");
        } else {
            afkCountdowns.put(uuid, remaining);
            String msg = module.getConfig().getString("messages.afk_countdown_ab")
                    .replace("{time}", String.valueOf(remaining));
            player.sendActionBar(ColorUtil.parse(msg));
        }
    }

    private boolean isBlocked(Player player, String section) {
        List<String> blockedWorlds = module.getConfig().getStringList(section + ".blocked_worlds");
        if (!blockedWorlds.isEmpty() && blockedWorlds.contains(player.getWorld().getName())) return true;

        List<String> blockedRegions = module.getConfig().getStringList(section + ".blocked_regions");
        if (blockedRegions.isEmpty()) return false;

        try {
            RegionManager rm = WorldGuard.getInstance().getPlatform()
                    .getRegionContainer().get(BukkitAdapter.adapt(player.getWorld()));
            if (rm == null) return false;
            com.sk89q.worldguard.protection.ApplicableRegionSet regions =
                    rm.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()).toVector().toBlockPoint());
            for (ProtectedRegion region : regions) {
                if (blockedRegions.contains(region.getId())) return true;
            }
        } catch (Exception ignored) {}

        return false;
    }
}
