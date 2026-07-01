package me.vennlmao.ariscore.amethyst.managers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.vennlmao.ariscore.amethyst.AmethystModule;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class AmethystExpiryManager {

    private final AmethystModule module;
    private ScheduledTask globalTask;

    public AmethystExpiryManager(AmethystModule module) {
        this.module = module;
    }

    public void start() {
        int intervalTicks = module.getConfig().getInt("self-destruct.update-interval-seconds", 1) * 20;
        globalTask = module.getPlugin().getServer().getGlobalRegionScheduler()
                .runAtFixedRate(module.getPlugin(), task -> tick(), 20L, intervalTicks);
    }

    public void stop() {
        if (globalTask != null) globalTask.cancel();
    }

    private void tick() {
        if (!module.getConfig().getBoolean("self-destruct.enabled", true)) return;

        for (Player player : module.getPlugin().getServer().getOnlinePlayers()) {
            PlayerInventory inventory = player.getInventory();
            ItemStack[] contents = inventory.getContents();

            for (int slot = 0; slot < contents.length; slot++) {
                ItemStack item = contents[slot];
                if (item == null) continue;
                if (module.getItemManager().getToolType(item) == null) continue;
                if (!module.getItemManager().hasExpiry(item)) continue;

                if (module.getItemManager().isExpired(item)) {
                    inventory.setItem(slot, null);
                } else {
                    module.getItemManager().refreshLore(item);
                }
            }
        }
    }
}
