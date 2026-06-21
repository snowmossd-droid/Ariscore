package me.vennlmao.ariscore.sell.listeners;

import me.vennlmao.ariscore.sell.SellModule;
import me.vennlmao.ariscore.sell.utils.ColorUtil;
import me.vennlmao.ariscore.sell.utils.FormatUtils;
import me.vennlmao.ariscore.sell.utils.SaleEntry;
import me.vennlmao.ariscore.sell.utils.SoundUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SellWandListener implements Listener {

    private final SellModule module;

    public SellWandListener(SellModule module) {
        this.module = module;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Container)) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (!module.getWandManager().isWand(item)) return;

        if (!module.getWandManager().isValid(meta)) {
            player.getInventory().setItemInMainHand(null);
            player.sendMessage(ColorUtil.colorize(module.getConfig().getString("sellwand.expired-removed", "&cThis sell wand has expired or has no uses left and has been removed.")));
            return;
        }

        event.setCancelled(true);

        Container container = (Container) block.getState();
        Inventory inventory = container.getInventory();

        double totalValue = 0.0;
        int totalAmount = 0;
        Map<String, Double> categoryValueMap = new HashMap<>(8);
        Map<String, Double> multiplierCache = new HashMap<>(8);
        List<SaleEntry> sales = new ArrayList<>();
        long timestamp = System.currentTimeMillis();

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack slotItem = inventory.getItem(i);
            if (slotItem == null || slotItem.getType() == Material.AIR) continue;

            double basePrice = module.getPriceManager().getPrice(slotItem.getType());
            if (basePrice <= 0.0) continue;

            String category = module.getPriceManager().getCategory(slotItem.getType());
            double multiplier = multiplierCache.computeIfAbsent(category, cat -> module.getDataManager().getMultiplier(player.getUniqueId(), cat));
            double finalPrice = basePrice * multiplier;
            double itemTotalValue = finalPrice * slotItem.getAmount();

            totalValue += itemTotalValue;
            totalAmount += slotItem.getAmount();
            sales.add(new SaleEntry(slotItem.getType().name(), slotItem.getAmount(), itemTotalValue, timestamp));
            categoryValueMap.put(category, categoryValueMap.getOrDefault(category, 0.0) + itemTotalValue);
            inventory.setItem(i, null);
        }

        if (!sales.isEmpty()) {
            module.getDataManager().addSales(player.getUniqueId(), sales);
            module.getDataManager().updateProgressBulk(player.getUniqueId(), categoryValueMap);
        }

        if (totalValue > 0.0) {
            if (module.getEconomy() != null) module.getEconomy().depositPlayer(player, totalValue);

            module.getWandManager().updateStats(item, player.getName(), totalAmount, totalValue);
            if (!module.getWandManager().isValid(item)) {
                player.getInventory().setItemInMainHand(null);
                player.sendMessage(ColorUtil.colorize(module.getConfig().getString("sellwand.broken", "&cYour sell wand has broken!")));
            }

            new SellListener(module).sendSellNotification(player, totalValue);
            SoundUtil.play(player, "sell-sound");
        } else {
            player.sendMessage(ColorUtil.colorize(module.getConfig().getString("sellwand.no-sellable-items", "&cNo sellable items found in this container.")));
        }
    }
}
