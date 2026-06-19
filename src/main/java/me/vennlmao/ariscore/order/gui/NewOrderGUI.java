package me.vennlmao.ariscore.order.gui;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.order.managers.MaterialsManager;
import me.vennlmao.ariscore.order.managers.OrderConfigManager;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NewOrderGUI implements Listener {

    private final ArisCore plugin;
    private final Map<UUID, MaterialsManager.ItemEntry> pendingEntry = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> pendingAmount = new ConcurrentHashMap<>();
    private final Map<UUID, Double> pendingPrice = new ConcurrentHashMap<>();

    public NewOrderGUI(ArisCore plugin) { this.plugin = plugin; }

    public void open(Player player, MaterialsManager.ItemEntry entry) {
        pendingEntry.put(player.getUniqueId(), entry);
        pendingAmount.put(player.getUniqueId(), plugin.getOrderModule().getConfigManager().getConfig().getInt("order.default-amount", 64));
        pendingPrice.put(player.getUniqueId(), entry.getDefaultPrice() > 0 ? entry.getDefaultPrice() : plugin.getOrderModule().getConfigManager().getConfig().getDouble("order.default-price", 100.0));
        render(player);
    }

    private void render(Player player) {
        FileConfiguration cfg = plugin.getOrderModule().getConfigManager().getGuiConfig("new-order");
        String title = ColorUtil.color(cfg.getString("title", "&8New Order"));
        int size = cfg.getInt("rows", 6) * 9;
        Inventory inv = Bukkit.createInventory(null, size, title);

        MaterialsManager.ItemEntry entry = pendingEntry.get(player.getUniqueId());
        if (entry == null) { player.closeInventory(); return; }

        int amount = pendingAmount.getOrDefault(player.getUniqueId(), 64);
        double price = pendingPrice.getOrDefault(player.getUniqueId(), 100.0);

        if (cfg.getBoolean("filler.enabled", true)) {
            ItemStack filler = GuiUtil.buildFiller(cfg.getConfigurationSection("filler"));
            for (int i = 0; i < size; i++) inv.setItem(i, filler);
        }

        Map<String, String> ph = new java.util.HashMap<>();
        ph.put("%amount%", String.valueOf(amount));
        ph.put("%price%", plugin.getOrderModule().getOrderManager().formatCurrency(price));
        ph.put("%total%", plugin.getOrderModule().getOrderManager().formatCurrency(price * amount));
        ph.put("%material%", ColorUtil.color(entry.getDisplayName()));
        ph.put("%min-price%", plugin.getOrderModule().getOrderManager().formatCurrency(entry.getMinPrice()));
        ph.put("%max-price%", entry.getMaxPrice() == Double.MAX_VALUE ? "∞" : plugin.getOrderModule().getOrderManager().formatCurrency(entry.getMaxPrice()));

        for (String key : cfg.getConfigurationSection("buttons") != null ? cfg.getConfigurationSection("buttons").getKeys(false) : new ArrayList<String>()) {
            int slot = cfg.getInt("buttons." + key + ".slot", -1);
            if (slot < 0) continue;
            inv.setItem(slot, GuiUtil.buildItem(cfg.getConfigurationSection("buttons." + key), ph));
        }

        int itemSlot = cfg.getInt("selected-item-slot", 13);
        inv.setItem(itemSlot, plugin.getOrderModule().getMaterialsManager().buildItemStack(entry, plugin.getOrderModule().getConfigManager()));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isOurInventory(event.getInventory())) return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        FileConfiguration cfg = plugin.getOrderModule().getConfigManager().getGuiConfig("new-order");
        int slot = event.getRawSlot();
        MaterialsManager.ItemEntry entry = pendingEntry.get(player.getUniqueId());
        if (entry == null) { player.closeInventory(); return; }

        int amount = pendingAmount.getOrDefault(player.getUniqueId(), 64);
        double price = pendingPrice.getOrDefault(player.getUniqueId(), 100.0);

        String action = getButtonAction(cfg, slot);
        if (action == null) return;

        switch (action) {
            case "close": player.closeInventory(); break;
            case "back": plugin.getOrderModule().getListMaterialsGUI().open(player); break;
            case "confirm": placeOrder(player, entry, amount, price); break;
            case "amount-up": {
                int inc = cfg.getInt("buttons." + getButtonKey(cfg, slot) + ".increment", 1);
                int max = cfg.getInt("order.max-amount", plugin.getOrderModule().getConfigManager().getConfig().getInt("order.max-amount", 6400));
                pendingAmount.put(player.getUniqueId(), Math.min(amount + inc, max));
                render(player); break;
            }
            case "amount-down": {
                int dec = cfg.getInt("buttons." + getButtonKey(cfg, slot) + ".decrement", 1);
                int min = Math.max(1, cfg.getInt("buttons." + getButtonKey(cfg, slot) + ".min", 1));
                pendingAmount.put(player.getUniqueId(), Math.max(amount - dec, min));
                render(player); break;
            }
            case "price-up": {
                double inc = cfg.getDouble("buttons." + getButtonKey(cfg, slot) + ".increment", 10.0);
                double max = entry.getMaxPrice() == Double.MAX_VALUE ? Double.MAX_VALUE : entry.getMaxPrice();
                pendingPrice.put(player.getUniqueId(), Math.min(price + inc, max));
                render(player); break;
            }
            case "price-down": {
                double dec = cfg.getDouble("buttons." + getButtonKey(cfg, slot) + ".decrement", 10.0);
                double min = Math.max(entry.getMinPrice(), cfg.getDouble("buttons." + getButtonKey(cfg, slot) + ".min", 1.0));
                pendingPrice.put(player.getUniqueId(), Math.max(price - dec, min));
                render(player); break;
            }
            case "set-amount": {
                player.closeInventory();
                plugin.getOrderModule().getSignManager().requestInput(player, plugin.getOrderModule().getConfigManager().msg("messages.enter-amount"), input -> {
                    try {
                        int val = Integer.parseInt(input.trim());
                        int max = plugin.getOrderModule().getConfigManager().getConfig().getInt("order.max-amount", 6400);
                        pendingAmount.put(player.getUniqueId(), Math.max(1, Math.min(val, max)));
                    } catch (NumberFormatException e) {
                        player.sendMessage(plugin.getOrderModule().getConfigManager().msg("messages.invalid-number"));
                    }
                    open(player, entry);
                });
                break;
            }
            case "set-price": {
                player.closeInventory();
                plugin.getOrderModule().getSignManager().requestInput(player, plugin.getOrderModule().getConfigManager().msg("messages.enter-price"), input -> {
                    try {
                        double val = Double.parseDouble(input.trim().replace(",", "."));
                        double min = entry.getMinPrice(), max = entry.getMaxPrice();
                        if (val < min) { player.sendMessage(plugin.getOrderModule().getConfigManager().msg("messages.price-too-low")); }
                        else if (val > max) { player.sendMessage(plugin.getOrderModule().getConfigManager().msg("messages.price-too-high")); }
                        else { pendingPrice.put(player.getUniqueId(), val); }
                    } catch (NumberFormatException e) {
                        player.sendMessage(plugin.getOrderModule().getConfigManager().msg("messages.invalid-number"));
                    }
                    open(player, entry);
                });
                break;
            }
        }
        plugin.getOrderModule().getSoundManager().play(player, "click");
    }

    private void placeOrder(Player player, MaterialsManager.ItemEntry entry, int amount, double price) {
        OrderConfigManager cfg = plugin.getOrderModule().getConfigManager();
        int maxOrders = cfg.getConfig().getInt("order.max-orders-per-player", 5);
        if (plugin.getOrderModule().getOrderManager().countActiveOrdersByPlayer(player.getUniqueId()) >= maxOrders) {
            player.sendMessage(cfg.msg("messages.max-orders-reached"));
            player.closeInventory();
            return;
        }
        double totalCost = price * amount;
        if (plugin.getOrderModule().getOrderManager().getEconomy() != null && !plugin.getOrderModule().getOrderManager().getEconomy().has(player, totalCost)) {
            player.sendMessage(cfg.msg("messages.not-enough-money"));
            player.closeInventory();
            return;
        }
        if (plugin.getOrderModule().getOrderManager().getEconomy() != null)
            plugin.getOrderModule().getOrderManager().getEconomy().withdrawPlayer(player, totalCost);

        OrderItem order = new OrderItem(entry.getDisplayName(), new ArrayList<>(), price, entry.getMaterial(), entry.getId(), new ArrayList<>(), player.getUniqueId(), entry.getItemType(), entry.getSubType(), amount);
        order.initExpiry(cfg.getConfig().getInt("order.expire-hours", 24) * 3600, cfg.getConfig().getInt("order.deletion-hours", 48) * 3600);
        plugin.getOrderModule().getOrderManager().addOrder(order);
        plugin.getOrderModule().getDataManager().saveOrder(order);

        player.sendMessage(cfg.msg("messages.order-placed", "%item%", OrderItem.formatMaterialName(entry.getMaterial().name()), "%amount%", String.valueOf(amount), "%price%", plugin.getOrderModule().getOrderManager().formatCurrency(totalCost)));
        plugin.getOrderModule().getSoundManager().play(player, "order-placed");
        pendingEntry.remove(player.getUniqueId());
        pendingAmount.remove(player.getUniqueId());
        pendingPrice.remove(player.getUniqueId());
        plugin.getOrderModule().getYourOrdersGUI().open(player);
    }

    private String getButtonAction(FileConfiguration cfg, int slot) {
        if (cfg.getConfigurationSection("buttons") == null) return null;
        for (String key : cfg.getConfigurationSection("buttons").getKeys(false)) {
            if (cfg.getInt("buttons." + key + ".slot") == slot) return cfg.getString("buttons." + key + ".action", key);
        }
        return null;
    }

    private String getButtonKey(FileConfiguration cfg, int slot) {
        if (cfg.getConfigurationSection("buttons") == null) return "";
        for (String key : cfg.getConfigurationSection("buttons").getKeys(false)) {
            if (cfg.getInt("buttons." + key + ".slot") == slot) return key;
        }
        return "";
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!isOurInventory(event.getInventory())) {
            pendingEntry.remove(player.getUniqueId());
            pendingAmount.remove(player.getUniqueId());
            pendingPrice.remove(player.getUniqueId());
        }
    }

    private boolean isOurInventory(Inventory inv) {
        if (inv == null) return false;
        String title = plugin.getOrderModule().getConfigManager().getGuiConfig("new-order").getString("title", "&8New Order");
        return inv.getViewers().stream().anyMatch(v -> {
            try { return v.getOpenInventory().getTitle().equals(ColorUtil.color(title)); } catch (Exception e) { return false; }
        });
    }

}
