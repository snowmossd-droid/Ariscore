package me.vennlmao.ariscore.sell.listeners;

import me.vennlmao.ariscore.sell.SellModule;
import me.vennlmao.ariscore.sell.menus.SellMenu;
import me.vennlmao.ariscore.sell.utils.ColorUtil;
import me.vennlmao.ariscore.sell.utils.FormatUtils;
import me.vennlmao.ariscore.sell.utils.SaleEntry;
import me.vennlmao.ariscore.sell.utils.SoundUtil;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SellListener implements Listener {

    private final SellModule module;
    private final Map<UUID, Long> lastWorthPreviewAt = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastWorthPreviewQuickKey = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastWorthPreviewMessage = new ConcurrentHashMap<>();

    public SellListener(SellModule module) {
        this.module = module;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity whoClicked = event.getWhoClicked();
        if (!(whoClicked instanceof Player player)) return;
        if (event.getView().getTopInventory() != null && event.getView().getTopInventory().getType() != InventoryType.CRAFTING) return;

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) {
            sendWorthPreview(player, item);
            return;
        }

        if (event.getInventory().getHolder() instanceof SellMenu) {
            sendWorthPreview(player, item);
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (player.getOpenInventory() != null && player.getOpenInventory().getTopInventory() != null
                && player.getOpenInventory().getTopInventory().getType() != InventoryType.CRAFTING) return;

        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        if (item == null || item.getType() == Material.AIR) return;

        sendWorthPreview(player, item);
    }

    private void sendWorthPreview(Player player, ItemStack item) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        String quickKey = item.getType().name() + ":" + item.getAmount();
        Long lastAt = lastWorthPreviewAt.get(uuid);
        if (lastAt != null && now - lastAt < 150L && quickKey.equals(lastWorthPreviewQuickKey.get(uuid))) return;

        Material type = item.getType();
        double basePrice = module.getPriceManager().getPrice(type);
        if (basePrice <= 0.0) return;

        String category = module.getPriceManager().getCategory(type);
        if (category == null) return;

        double multiplier = module.getDataManager().getMultiplier(uuid, category);
        double total = basePrice * multiplier * item.getAmount();

        String message = module.getConfig().getString("actionbar-message", "&eworth: &6%price%$");
        message = message.replace("%price%", FormatUtils.formatPrice(total));

        if (lastAt != null && now - lastAt < 500L && message.equals(lastWorthPreviewMessage.get(uuid))) {
            lastWorthPreviewAt.put(uuid, now);
            lastWorthPreviewQuickKey.put(uuid, quickKey);
            return;
        }

        player.sendActionBar(ColorUtil.component(message));
        lastWorthPreviewAt.put(uuid, now);
        lastWorthPreviewQuickKey.put(uuid, quickKey);
        lastWorthPreviewMessage.put(uuid, message);
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
            double multiplier = categoryMultipliers.computeIfAbsent(category, c -> module.getDataManager().getMultiplier(player.getUniqueId(), c));
            double unitPrice = basePrice * multiplier;
            double total = unitPrice * item.getAmount();

            totalEarned += total;
            sales.add(new SaleEntry(item.getType().name(), item.getAmount(), total, now));
            categoryEarnings.put(category, categoryEarnings.getOrDefault(category, 0.0) + total);
            inventory.setItem(i, null);
        }

        if (!sales.isEmpty()) module.getDataManager().addSales(player.getUniqueId(), sales);
        module.getDataManager().updateProgressBulk(player.getUniqueId(), categoryEarnings);

        if (totalEarned > 0.0) {
            if (module.getEconomy() != null) module.getEconomy().depositPlayer(player, totalEarned);
            String message = module.getConfig().getString("actionbar-message", "&eworth: &6%price%$")
                    .replace("%price%", FormatUtils.formatPrice(totalEarned));
            player.sendActionBar(ColorUtil.component(message));
            SoundUtil.play(player, "sell-sound");
        }
    }
}
