package me.vennlmao.ariscore.order.gui;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.order.managers.OrderItem;
import me.vennlmao.ariscore.order.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DeliveryGUI implements Listener {

    private final ArisCore plugin;
    private final Map<UUID, OrderItem> pendingOrder = new ConcurrentHashMap<>();

    public DeliveryGUI(ArisCore plugin) { this.plugin = plugin; }

    public void open(Player player, OrderItem order) {
        FileConfiguration cfg = plugin.getOrderModule().getConfigManager().getGuiConfig("confirm-delivery");
        String title = ColorUtil.color(cfg.getString("title", "&8Deliver Items"));
        int size = cfg.getInt("rows", 6) * 9;
        OrderHolder holder = new OrderHolder("confirm-delivery");
        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inv);

        pendingOrder.put(player.getUniqueId(), order);

        if (cfg.getBoolean("filler.enabled", true)) {
            ItemStack filler = GuiUtil.buildFiller(cfg.getConfigurationSection("filler"));
            for (int i = 0; i < size; i++) inv.setItem(i, filler);
        }

        Map<String, String> ph = order.getPlaceholders(plugin.getOrderModule().getOrderManager());
        if (cfg.getConfigurationSection("buttons") != null) {
            for (String key : cfg.getConfigurationSection("buttons").getKeys(false)) {
                int slot = cfg.getInt("buttons." + key + ".slot", -1);
                if (slot >= 0) inv.setItem(slot, GuiUtil.buildItem(cfg.getConfigurationSection("buttons." + key), ph));
            }
        }

        List<Integer> deliverySlots = GuiUtil.parseSlots(cfg.getString("delivery-slots", "10-16,19-25,28-34"));
        for (int slot : deliverySlots) inv.setItem(slot, null);

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isOurInventory(event.getInventory())) return;

        FileConfiguration cfg = plugin.getOrderModule().getConfigManager().getGuiConfig("confirm-delivery");
        int slot = event.getRawSlot();
        List<Integer> deliverySlots = GuiUtil.parseSlots(cfg.getString("delivery-slots", "10-16,19-25,28-34"));
        int size = cfg.getInt("rows", 6) * 9;

        if (slot >= size) return;
        if (deliverySlots.contains(slot)) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        String action = getButtonAction(cfg, slot);
        if ("confirm".equals(action)) {
            processDelivery(player, event.getInventory(), deliverySlots, cfg);
        } else if ("cancel".equals(action)) {
            returnItems(player, event.getInventory(), deliverySlots);
            plugin.getOrderModule().getOrderViewGUI().open(player);
        }
        plugin.getOrderModule().getSoundManager().play(player, "click");
    }

    private void processDelivery(Player player, Inventory inv, List<Integer> deliverySlots, FileConfiguration cfg) {
        OrderItem order = pendingOrder.get(player.getUniqueId());
        if (order == null) { player.closeInventory(); return; }

        List<ItemStack> deliveredItems = new ArrayList<>();
        int totalDelivered = 0;

        for (int slot : deliverySlots) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() != order.getMaterial()) continue;
            int canDeliver = Math.min(item.getAmount(), order.getRemainingAmount() - totalDelivered);
            if (canDeliver <= 0) break;
            deliveredItems.add(new ItemStack(item.getType(), canDeliver));
            if (canDeliver < item.getAmount()) {
                item.setAmount(item.getAmount() - canDeliver);
            } else {
                inv.setItem(slot, null);
            }
            totalDelivered += canDeliver;
        }

        if (totalDelivered == 0) {
            player.sendMessage(plugin.getOrderModule().getConfigManager().msg("messages.no-valid-items"));
            returnItems(player, inv, deliverySlots);
            return;
        }

        returnItems(player, inv, deliverySlots);

        double payment = order.getPricePerItem() * totalDelivered;
        order.addDeliveredAmount(totalDelivered);
        order.addPaidAmount(payment);

        if (plugin.getOrderModule().getOrderManager().getEconomy() != null)
            plugin.getOrderModule().getOrderManager().getEconomy().depositPlayer(player, payment);

        plugin.getOrderModule().getDataManager().saveOrder(order);

        player.sendMessage(plugin.getOrderModule().getConfigManager().msg("messages.delivery-success",
                "%amount%", String.valueOf(totalDelivered),
                "%item%", OrderItem.formatMaterialName(order.getMaterial().name()),
                "%payment%", plugin.getOrderModule().getOrderManager().formatCurrency(payment)));

        plugin.getOrderModule().getSoundManager().play(player, "delivery-success");

        Player owner = Bukkit.getPlayer(order.getCreator());
        if (owner != null && owner.isOnline()) {
            owner.sendMessage(plugin.getOrderModule().getConfigManager().msg("messages.order-received",
                    "%player%", player.getName(),
                    "%amount%", String.valueOf(totalDelivered),
                    "%item%", OrderItem.formatMaterialName(order.getMaterial().name())));
        }

        if (order.isFullyDelivered()) {
            order.setActive(false);
            plugin.getOrderModule().getDataManager().saveOrder(order);
        }

        pendingOrder.remove(player.getUniqueId());
        plugin.getOrderModule().getOrderViewGUI().open(player);
    }

    private void returnItems(Player player, Inventory inv, List<Integer> deliverySlots) {
        for (int slot : deliverySlots) {
            ItemStack item = inv.getItem(slot);
            if (item != null) {
                for (ItemStack overflow : player.getInventory().addItem(item).values())
                    player.getWorld().dropItemNaturally(player.getLocation(), overflow);
                inv.setItem(slot, null);
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!isOurInventory(event.getInventory())) return;
        FileConfiguration cfg = plugin.getOrderModule().getConfigManager().getGuiConfig("confirm-delivery");
        List<Integer> deliverySlots = GuiUtil.parseSlots(cfg.getString("delivery-slots", "10-16,19-25,28-34"));
        returnItems(player, event.getInventory(), deliverySlots);
        pendingOrder.remove(player.getUniqueId());
    }

    private String getButtonAction(FileConfiguration cfg, int slot) {
        if (cfg.getConfigurationSection("buttons") == null) return null;
        for (String key : cfg.getConfigurationSection("buttons").getKeys(false))
            if (cfg.getInt("buttons." + key + ".slot") == slot) return cfg.getString("buttons." + key + ".action", key);
        return null;
    }

    private boolean isOurInventory(Inventory inv) {
        return inv != null && inv.getHolder() instanceof OrderHolder h && "confirm-delivery".equals(h.getGuiId());
    }
}
