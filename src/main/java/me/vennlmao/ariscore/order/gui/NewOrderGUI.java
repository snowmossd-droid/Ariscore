package me.vennlmao.ariscore.order.gui;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.order.managers.MaterialsManager;
import me.vennlmao.ariscore.order.managers.OrderConfigManager;
import me.vennlmao.ariscore.order.managers.OrderItem;
import me.vennlmao.ariscore.order.utils.ColorUtil;
import me.vennlmao.ariscore.order.utils.EcoUtil;
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
import java.util.HashMap;
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
        if (!pendingAmount.containsKey(player.getUniqueId())) {
            pendingAmount.put(player.getUniqueId(), plugin.getOrderModule().getConfigManager().getConfig().getInt("order.default-amount", 64));
        }
        if (!pendingPrice.containsKey(player.getUniqueId())) {
            double defaultPrice = entry.getDefaultPrice() > 0 ? entry.getDefaultPrice() : plugin.getOrderModule().getConfigManager().getConfig().getDouble("order.default-price", 100.0);
            pendingPrice.put(player.getUniqueId(), defaultPrice);
        }
        render(player);
    }

    private void render(Player player) {
        FileConfiguration cfg = plugin.getOrderModule().getConfigManager().getGuiConfig("new-order");
        OrderConfigManager configManager = plugin.getOrderModule().getConfigManager();

        MaterialsManager.ItemEntry entry = pendingEntry.get(player.getUniqueId());
        if (entry == null) { player.closeInventory(); return; }

        String title = ColorUtil.color(cfg.getString("title", "&8New Order"));
        int size = cfg.getInt("rows", 3) * 9;
        OrderHolder holder = new OrderHolder("new-order");
        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inv);

        int amount = pendingAmount.getOrDefault(player.getUniqueId(), 64);
        double price = pendingPrice.getOrDefault(player.getUniqueId(), 100.0);

        if (cfg.getBoolean("filler.enabled", true)) {
            ItemStack filler = GuiUtil.buildFiller(cfg.getConfigurationSection("filler"));
            for (int i = 0; i < size; i++) inv.setItem(i, filler);
        }

        double minPrice = Math.max(entry.getMinPrice(), configManager.getConfig().getDouble("minimum-price-per-item", 1.0));
        double maxPriceRaw = configManager.getConfig().getInt("maximum-price-per-item", -1);
        double maxPrice = entry.getMaxPrice();
        if (maxPriceRaw > 0 && maxPriceRaw < maxPrice) maxPrice = maxPriceRaw;

        int minAmount = Math.max(1, configManager.getConfig().getInt("minimum-item-per-order", 1));
        int maxAmountRaw = configManager.getConfig().getInt("maximum-item-per-order", -1);
        int maxAmount = configManager.getConfig().getInt("order.max-amount", 6400);
        if (maxAmountRaw > 0 && maxAmountRaw < maxAmount) maxAmount = maxAmountRaw;

        Map<String, String> ph = new HashMap<>();
        ph.put("%amount%", String.valueOf(amount));
        ph.put("%price%", EcoUtil.format(price, configManager));
        ph.put("%total%", EcoUtil.format(price * amount, configManager));
        ph.put("%material%", ColorUtil.color(entry.getDisplayName()));
        ph.put("%min-price%", EcoUtil.format(minPrice, configManager));
        ph.put("%max-price%", maxPrice == Double.MAX_VALUE ? "∞" : EcoUtil.format(maxPrice, configManager));
        ph.put("%min-amount%", String.valueOf(minAmount));
        ph.put("%max-amount%", String.valueOf(maxAmount));

        if (cfg.getConfigurationSection("buttons") != null) {
            for (String key : cfg.getConfigurationSection("buttons").getKeys(false)) {
                int slot = cfg.getInt("buttons." + key + ".slot", -1);
                if (slot < 0) continue;
                inv.setItem(slot, GuiUtil.buildItem(cfg.getConfigurationSection("buttons." + key), ph));
            }
        }

        int itemSlot = cfg.getInt("selected-item-slot", 13);
        inv.setItem(itemSlot, plugin.getOrderModule().getMaterialsManager().buildItemStack(entry, configManager));

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
            case "back": {
                pendingEntry.remove(player.getUniqueId());
                pendingAmount.remove(player.getUniqueId());
                pendingPrice.remove(player.getUniqueId());
                plugin.getOrderModule().getListMaterialsGUI().open(player);
                break;
            }
            case "confirm": placeOrder(player, entry, amount, price); break;
            case "set-amount": {
                plugin.getOrderModule().getSignManager().requestInput(player, "amount", input -> {
                    Double val = parsePlainNumber(input);
                    OrderConfigManager configManager = plugin.getOrderModule().getConfigManager();
                    int minAmount = Math.max(1, configManager.getConfig().getInt("minimum-item-per-order", 1));
                    int maxAmountRaw = configManager.getConfig().getInt("maximum-item-per-order", -1);
                    int maxAmount = configManager.getConfig().getInt("order.max-amount", 6400);
                    if (maxAmountRaw > 0 && maxAmountRaw < maxAmount) maxAmount = maxAmountRaw;
                    if (val == null) {
                        player.sendMessage(configManager.msg("messages.invalid-number"));
                    } else {
                        int intVal = val.intValue();
                        if (intVal < minAmount) player.sendMessage(configManager.msg("messages.amount-too-low"));
                        else if (intVal > maxAmount) player.sendMessage(configManager.msg("messages.amount-too-high"));
                        else pendingAmount.put(player.getUniqueId(), intVal);
                    }
                    render(player);
                });
                break;
            }
            case "set-price": {
                plugin.getOrderModule().getSignManager().requestInput(player, "price", input -> {
                    OrderConfigManager configManager = plugin.getOrderModule().getConfigManager();
                    Double val = EcoUtil.parsePrice(input, configManager);
                    double minPrice = Math.max(entry.getMinPrice(), configManager.getConfig().getDouble("minimum-price-per-item", 1.0));
                    double maxPriceRaw = configManager.getConfig().getInt("maximum-price-per-item", -1);
                    double maxPrice = entry.getMaxPrice();
                    if (maxPriceRaw > 0 && maxPriceRaw < maxPrice) maxPrice = maxPriceRaw;
                    if (val == null) {
                        player.sendMessage(configManager.msg("messages.invalid-number"));
                    } else if (val < minPrice) {
                        player.sendMessage(configManager.msg("messages.price-too-low"));
                    } else if (val > maxPrice) {
                        player.sendMessage(configManager.msg("messages.price-too-high"));
                    } else {
                        pendingPrice.put(player.getUniqueId(), val);
                    }
                    render(player);
                });
                break;
            }
            default: break;
        }
        plugin.getOrderModule().getSoundManager().play(player, "click");
    }

    private Double parsePlainNumber(String input) {
        try {
            return Double.parseDouble(input.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
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

        player.sendMessage(cfg.msg("messages.order-placed", "%item%", OrderItem.formatMaterialName(entry.getMaterial().name()), "%amount%", String.valueOf(amount), "%price%", EcoUtil.format(totalCost, cfg)));

        if (cfg.getConfig().getBoolean("announce-orders", true)) {
            String announce = ColorUtil.color(cfg.getConfig().getString("announce-message", "")
                    .replace("%owner%", player.getName())
                    .replace("%requested-amount%", String.valueOf(amount))
                    .replace("%requested-material%", OrderItem.formatMaterialName(entry.getMaterial().name()))
                    .replace("%price%", EcoUtil.format(price, cfg)));
            if (!announce.isEmpty()) Bukkit.broadcastMessage(announce);
        }

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

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!isOurInventory(event.getInventory())) return;
        if (plugin.getOrderModule().getSignManager().hasPending(player.getUniqueId())) return;
    }

    private boolean isOurInventory(Inventory inv) {
        return inv != null && inv.getHolder() instanceof OrderHolder h && "new-order".equals(h.getGuiId());
    }
}
