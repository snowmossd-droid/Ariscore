package me.vennlmao.ariscore.crates.utils;

import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public class FoliaUtil {

    private static final boolean FOLIA;

    static {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        FOLIA = folia;
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    public static void runForEntity(Plugin plugin, Entity entity, Runnable task) {
        if (FOLIA) {
            entity.getScheduler().run(plugin, scheduledTask -> task.run(), null);
        } else {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }

    public static void runAsync(Plugin plugin, Runnable task) {
        if (FOLIA) {
            plugin.getServer().getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
        } else {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
        }
    }

    public static void runSync(Plugin plugin, Runnable task) {
        if (FOLIA) {
            plugin.getServer().getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
        } else {
            plugin.getServer().getScheduler().runTask(plugin, task);
        }
    }
}
