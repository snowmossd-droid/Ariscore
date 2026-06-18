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

public class EditOrderGUI implements Listener {

    private final ArisCore plugin;

    public EditOrderGUI(ArisCore plugin) { this.plugin = plugin; }

    public void open(Player player, OrderItem order) {
        FileConfiguration cfg = plugin.getOrderModule().getConfigManager().getGuiConfig("edit-order");
        String title = ColorUtil.color(cfg.getString("title", "&8Edit Order"));
        int size = cfg.getInt("rows", 4) * 9;
        Inventory inv = Bukkit.createInventory(null, size, title);

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

        FileConfiguration cfg = plugin.getOrderModule().getConfigManager().getGuiConfig("edit-order");
        int slot = event.getRawSlot();
        String action = getButtonAction(cfg, slot);
        if (action == null) return;

        OrderItem order = getOrderFromInv(event.getInventory(), cfg);
        if (order == null) { player.closeInventory(); return; }

        switch (action) {
            case "back": plugin.getOrderModule().getYourOrdersGUI().open(player); break;
            case "cancel-order": plugin.getOrderModule().getConfirmCancelGUI().open(player, order); break;
            case "collect": plugin.getOrderModule().getCollectItemsGUI().open(player, order); break;
            default: break;
        }
        plugin.getOrderModule().getSoundManager().play(player, "click");
    }

    private OrderItem getOrderFromInv(Inventory inv, FileConfiguration cfg) {
        int slot = cfg.getInt("order-item-slot", 13);
        ItemStack orderItem = inv.getItem(slot);
        if (orderItem == null) return null;
        for (OrderItem o : plugin.getOrderModule().getOrderManager().getAllOrders())
            if (o.getMaterial() == orderItem.getType()) return o;
        return null;
    }

    private String getButtonAction(FileConfiguration cfg, int slot) {
        if (cfg.getConfigurationSection("buttons") == null) return null;
        for (String key : cfg.getConfigurationSection("buttons").getKeys(false))
            if (cfg.getInt("buttons." + key + ".slot") == slot) return cfg.getString("buttons." + key + ".action", key);
        return null;
    }

    private boolean isOurInventory(Inventory inv) {
        if (inv == null) return false;
        String title = plugin.getOrderModule().getConfigManager().getGuiConfig("edit-order").getString("title", "&8Edit Order");
        return inv.getViewers().stream().anyMatch(v -> {
            try { return v.getOpenInventory().getTitle().equals(ColorUtil.color(title)); } catch (Exception e) { return false; }
        });
    }
}
