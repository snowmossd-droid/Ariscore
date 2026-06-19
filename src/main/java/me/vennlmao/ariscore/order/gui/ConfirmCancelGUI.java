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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ConfirmCancelGUI implements Listener {

    private final ArisCore plugin;
    private final Map<UUID, OrderItem> pendingCancel = new ConcurrentHashMap<>();

    public ConfirmCancelGUI(ArisCore plugin) { this.plugin = plugin; }

    public void open(Player player, OrderItem order) {
        FileConfiguration cfg = plugin.getOrderModule().getConfigManager().getGuiConfig("confirm-cancel");
        String title = ColorUtil.color(cfg.getString("title", "&8Confirm Cancel"));
        int size = cfg.getInt("rows", 3) * 9;
        Inventory inv = Bukkit.createInventory(null, size, title);

        pendingCancel.put(player.getUniqueId(), order);

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

        FileConfiguration cfg = plugin.getOrderModule().getConfigManager().getGuiConfig("confirm-cancel");
        int slot = event.getRawSlot();
        String action = getButtonAction(cfg, slot);
        if (action == null) return;

        OrderItem order = pendingCancel.get(player.getUniqueId());
        if (order == null) { player.closeInventory(); return; }

        switch (action) {
            case "confirm": cancelOrder(player, order); break;
            case "deny": plugin.getOrderModule().getEditOrderGUI().open(player, order); break;
            default: break;
        }
        plugin.getOrderModule().getSoundManager().play(player, "click");
    }

    private void cancelOrder(Player player, OrderItem order) {
        double refund = order.getRemainingPayment();
        if (refund > 0 && plugin.getOrderModule().getOrderManager().getEconomy() != null)
            plugin.getOrderModule().getOrderManager().getEconomy().depositPlayer(player, refund);

        if (order.getAvailableToCollect() > 0) {
            int toCollect = order.getAvailableToCollect();
            int remaining = toCollect;
            while (remaining > 0) {
                int stack = Math.min(remaining, order.getMaterial().getMaxStackSize());
                for (ItemStack overflow : player.getInventory().addItem(new ItemStack(order.getMaterial(), stack)).values())
                    player.getWorld().dropItemNaturally(player.getLocation(), overflow);
                remaining -= stack;
            }
        }

        order.setActive(false);
        plugin.getOrderModule().getOrderManager().removeOrder(order.getOrderUuid());
        plugin.getOrderModule().getDataManager().deleteOrder(order.getOrderUuid());

        player.sendMessage(plugin.getOrderModule().getConfigManager().msg("messages.order-cancelled",
                "%refund%", plugin.getOrderModule().getOrderManager().formatCurrency(refund)));
        plugin.getOrderModule().getSoundManager().play(player, "cancel");

        pendingCancel.remove(player.getUniqueId());
        plugin.getOrderModule().getYourOrdersGUI().open(player);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) pendingCancel.remove(player.getUniqueId());
    }

    private String getButtonAction(FileConfiguration cfg, int slot) {
        if (cfg.getConfigurationSection("buttons") == null) return null;
        for (String key : cfg.getConfigurationSection("buttons").getKeys(false))
            if (cfg.getInt("buttons." + key + ".slot") == slot) return cfg.getString("buttons." + key + ".action", key);
        return null;
    }

    private boolean isOurInventory(Inventory inv) {
        if (inv == null) return false;
        String title = plugin.getOrderModule().getConfigManager().getGuiConfig("confirm-cancel").getString("title", "&8Confirm Cancel");
        return inv.getViewers().stream().anyMatch(v -> {
            try { return v.getOpenInventory().getTitle().equals(ColorUtil.color(title)); } catch (Exception e) { return false; }
        });
    }
}
