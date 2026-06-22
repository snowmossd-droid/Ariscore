package me.vennlmao.ariscore.sell.listeners;

import me.vennlmao.ariscore.sell.SellModule;
import me.vennlmao.ariscore.sell.menus.SellMenu;
import me.vennlmao.ariscore.sell.utils.ColorUtil;
import me.vennlmao.ariscore.sell.utils.FormatUtils;
import me.vennlmao.ariscore.sell.utils.SaleEntry;
import me.vennlmao.ariscore.sell.utils.SoundUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SellListener implements Listener {

    private final SellModule module;

    public SellListener(SellModule module) {
        this.module = module;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof SellMenu)) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        double totalEarned = 0.0;
        Map<String, Double> categoryEarnings = new HashMap<>();
        Map<String, Double> categoryMultipliers = new HashMap<>();
        List<SaleEntry> sales = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;

            double basePrice = module.getPriceManager().getPrice(item.getType());
            if (basePrice <= 0.0) {
                for (ItemStack overflow : player.getInventory().addItem(item).values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), overflow);
                }
                continue;
            }

            String category = module.getPriceManager().getCategory(item.getType());
            double multiplier = categoryMultipliers.computeIfAbsent(category,
                    c -> module.getDataManager().getMultiplier(player.getUniqueId(), c));
            double total = basePrice * multiplier * item.getAmount();

            totalEarned += total;
            sales.add(new SaleEntry(item.getType().name(), item.getAmount(), total, now));
            categoryEarnings.put(category, categoryEarnings.getOrDefault(category, 0.0) + total);
            inventory.setItem(i, null);
        }

        if (!sales.isEmpty()) module.getDataManager().addSales(player.getUniqueId(), sales);
        module.getDataManager().updateProgressBulk(player.getUniqueId(), categoryEarnings);

        if (totalEarned > 0.0) {
            if (module.getEconomy() != null) module.getEconomy().depositPlayer(player, totalEarned);
            sendSellNotification(player, totalEarned);
            SoundUtil.play(player, "sell-sound");
        }
    }

    public void sendSellNotification(Player player, double amount) {
        String raw = module.getConfig().getString("sell-message", "&fđã bán được &a%price%");
        String message = ColorUtil.colorize(raw.replace("%price%", FormatUtils.formatPrice(amount)));
        player.sendMessage(message);
        player.sendActionBar(ColorUtil.component(raw.replace("%price%", FormatUtils.formatPrice(amount))));
    }
}
