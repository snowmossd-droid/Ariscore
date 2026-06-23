package me.vennlmao.ariscore.order.gui;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.order.managers.OrderItem;
import me.vennlmao.ariscore.order.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class YourOrdersGUI implements Listener {

    private final ArisCore plugin;
    private final Map<UUID, Integer> playerPage = new ConcurrentHashMap<>();
    private final Map<UUID, List<OrderItem>> pageCache = new ConcurrentHashMap<>();

    public YourOrdersGUI(ArisCore plugin) { this.plugin = plugin; }

    public void open(Player player) { open(player, 0); }

    public void open(Player player, int page) {
        FileConfiguration cfg = plugin.getOrderModule().getConfigManager().getGuiConfig("your-orders");
        String title = ColorUtil.color(cfg.getString("title", "&8Your Orders"));
        int size = cfg.getInt("rows", 6) * 9;
        OrderHolder holder = new OrderHolder("your-orders");
        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inv);

        List<OrderItem> orders = plugin.getOrderModule().getOrderManager().getOrdersByPlayer(player.getUniqueId());
        orders.removeIf(o -> !o.shouldBeInYourOrders());
        pageCache.put(player.getUniqueId(), orders);

        List<Integer> itemSlots = GuiUtil.parseSlots(cfg.getString("item-slots", "0-44"));
        int perPage = itemSlots.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) orders.size() / perPage));
        page = Math.max(0, Math.min(page, totalPages - 1));
        playerPage.put(player.getUniqueId(), page);

        if (cfg.getBoolean("filler.enabled", true)) {
            ItemStack filler = GuiUtil.buildFiller(cfg.getConfigurationSection("filler"));
            for (int i = 0; i < size; i++) inv.setItem(i, filler);
        }

        int start = page * perPage;
        for (int i = 0; i < perPage && start + i < orders.size(); i++) {
            OrderItem order = orders.get(start + i);
            inv.setItem(itemSlots.get(i), buildOrderItem(order, cfg));
        }

        if (page > 0) inv.setItem(cfg.getInt("prev-page-slot", 45), GuiUtil.buildItem(cfg.getConfigurationSection("prev-page")));
        if (page < totalPages - 1) inv.setItem(cfg.getInt("next-page-slot", 53), GuiUtil.buildItem(cfg.getConfigurationSection("next-page")));
        if (cfg.getConfigurationSection("close-button") != null) inv.setItem(cfg.getInt("close-button-slot", 49), GuiUtil.buildItem(cfg.getConfigurationSection("close-button")));
        if (cfg.getConfigurationSection("new-order-button") != null) inv.setItem(cfg.getInt("new-order-button-slot", 50), GuiUtil.buildItem(cfg.getConfigurationSection("new-order-button")));

        player.openInventory(inv);
    }

    private ItemStack buildOrderItem(OrderItem order, FileConfiguration cfg) {
        Map<String, String> ph = order.getPlaceholders(plugin.getOrderModule().getOrderManager());
        ConfigurationSection orderItemCfg = cfg.getConfigurationSection("order-item");
        if (orderItemCfg == null) {
            ItemStack item = new ItemStack(order.getMaterial());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) { meta.setDisplayName(ColorUtil.color("&f" + OrderItem.formatMaterialName(order.getMaterial().name()))); item.setItemMeta(meta); }
            return item;
        }
        return order.toItemStack(plugin.getOrderModule().getOrderManager(), buildConfigMap(orderItemCfg, ph));
    }

    private Map<Object, Object> buildConfigMap(ConfigurationSection section, Map<String, String> ph) {
        Map<Object, Object> map = new HashMap<>();
        for (String key : section.getKeys(false)) map.put(key, section.get(key));
        return map;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isOurInventory(event.getInventory())) return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        FileConfiguration cfg = plugin.getOrderModule().getConfigManager().getGuiConfig("your-orders");
        int slot = event.getRawSlot();
        int page = playerPage.getOrDefault(player.getUniqueId(), 0);

        if (slot == cfg.getInt("prev-page-slot", 45)) { open(player, page - 1); return; }
        if (slot == cfg.getInt("next-page-slot", 53)) { open(player, page + 1); return; }
        if (slot == cfg.getInt("close-button-slot", 49)) { player.closeInventory(); return; }
        if (slot == cfg.getInt("new-order-button-slot", 50)) {
            plugin.getOrderModule().getListMaterialsGUI().open(player);
            plugin.getOrderModule().getSoundManager().play(player, "click");
            return;
        }

        List<Integer> itemSlots = GuiUtil.parseSlots(cfg.getString("item-slots", "0-44"));
        int idx = itemSlots.indexOf(slot);
        if (idx < 0) return;
        List<OrderItem> orders = pageCache.get(player.getUniqueId());
        int orderIdx = page * itemSlots.size() + idx;
        if (orders == null || orderIdx >= orders.size()) return;

        OrderItem order = orders.get(orderIdx);
        if (order.getAvailableToCollect() > 0) {
            plugin.getOrderModule().getCollectItemsGUI().open(player, order);
        } else {
            plugin.getOrderModule().getEditOrderGUI().open(player, order);
        }
        plugin.getOrderModule().getSoundManager().play(player, "click");
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (isOurInventory(event.getInventory())) {
            playerPage.remove(player.getUniqueId());
            pageCache.remove(player.getUniqueId());
        }
    }

    private boolean isOurInventory(Inventory inv) {
        return inv != null && inv.getHolder() instanceof OrderHolder h && "your-orders".equals(h.getGuiId());
    }
}
