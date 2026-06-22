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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

public class CollectItemsGUI implements Listener {

    private final ArisCore plugin;

    public CollectItemsGUI(ArisCore plugin) { this.plugin = plugin; }

    public void open(Player player, OrderItem order) {
        FileConfiguration cfg = plugin.getOrderModule().getConfigManager().getGuiConfig("collect-items");
        String title = ColorUtil.color(cfg.getString("title", "&8Collect Items"));
        int size = cfg.getInt("rows", 4) * 9;
        OrderHolder holder = new OrderHolder("collect-items");
        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inv);

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

        int itemSlot = cfg.getInt("order-item-slot", 13);
        inv.setItem(itemSlot, order.toItemStack(plugin.getOrderModule().getOrderManager()));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isOurInventory(event.getInventory())) return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        FileConfiguration cfg = plugin.getOrderModule().getConfigManager().getGuiConfig("collect-items");
        int slot = event.getRawSlot();
        String action = getButtonAction(cfg, slot);
        if (action == null) return;

        int orderItemSlot = cfg.getInt("order-item-slot", 13);
        if (slot == orderItemSlot) return;

        UUID orderUuid = getOrderFromInventory(event.getInventory(), cfg);
        if (orderUuid == null) { player.closeInventory(); return; }
        OrderItem order = plugin.getOrderModule().getOrderManager().getOrder(orderUuid);
        if (order == null) { player.closeInventory(); return; }

        switch (action) {
            case "collect-all":
                collectItems(player, order, order.getAvailableToCollect());
                break;
            case "back":
                plugin.getOrderModule().getYourOrdersGUI().open(player);
                break;
            case "cancel-order":
                plugin.getOrderModule().getConfirmCancelGUI().open(player, order);
                break;
            default:
                break;
        }
        plugin.getOrderModule().getSoundManager().play(player, "click");
    }

    private void collectItems(Player player, OrderItem order, int amount) {
        if (amount <= 0) {
            player.sendMessage(plugin.getOrderModule().getConfigManager().msg("messages.nothing-to-collect"));
            return;
        }
        int remaining = amount;
        while (remaining > 0) {
            int stack = Math.min(remaining, order.getMaterial().getMaxStackSize());
            ItemStack item = new ItemStack(order.getMaterial(), stack);
            for (ItemStack overflow : player.getInventory().addItem(item).values())
                player.getWorld().dropItemNaturally(player.getLocation(), overflow);
            remaining -= stack;
        }
        order.addCollectedAmount(amount);
        plugin.getOrderModule().getDataManager().saveOrder(order);
        player.sendMessage(plugin.getOrderModule().getConfigManager().msg("messages.collected",
                "%amount%", String.valueOf(amount),
                "%item%", OrderItem.formatMaterialName(order.getMaterial().name())));
        plugin.getOrderModule().getSoundManager().play(player, "collect");
        plugin.getOrderModule().getYourOrdersGUI().open(player);
    }

    private java.util.UUID getOrderFromInventory(Inventory inv, FileConfiguration cfg) {
        int itemSlot = cfg.getInt("order-item-slot", 13);
        ItemStack orderItem = inv.getItem(itemSlot);
        if (orderItem == null || !orderItem.hasItemMeta()) return null;
        for (OrderItem order : plugin.getOrderModule().getOrderManager().getAllOrders()) {
            if (order.getMaterial() == orderItem.getType() && order.getAvailableToCollect() > 0) return order.getOrderUuid();
        }
        return null;
    }

    private String getButtonAction(FileConfiguration cfg, int slot) {
        if (cfg.getConfigurationSection("buttons") == null) return null;
        for (String key : cfg.getConfigurationSection("buttons").getKeys(false))
            if (cfg.getInt("buttons." + key + ".slot") == slot) return cfg.getString("buttons." + key + ".action", key);
        return null;
    }

    private boolean isOurInventory(Inventory inv) {
        return inv != null && inv.getHolder() instanceof OrderHolder h && "collect-items".equals(h.getGuiId());
    }
}
