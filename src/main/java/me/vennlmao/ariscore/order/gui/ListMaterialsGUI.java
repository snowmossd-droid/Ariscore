package me.vennlmao.ariscore.order.gui;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.order.managers.MaterialsManager;
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

public class ListMaterialsGUI implements Listener {

    private final ArisCore plugin;
    private final Map<UUID, Integer> playerPage = new ConcurrentHashMap<>();
    private final Map<UUID, List<MaterialsManager.ItemEntry>> pageCache = new ConcurrentHashMap<>();

    public ListMaterialsGUI(ArisCore plugin) { this.plugin = plugin; }

    public void open(Player player) { open(player, 0); }

    public void open(Player player, int page) {
        FileConfiguration cfg = plugin.getOrderModule().getConfigManager().getGuiConfig("list-materials");
        String title = ColorUtil.color(cfg.getString("title", "&8Select Item"));
        int size = cfg.getInt("rows", 6) * 9;
        Inventory inv = Bukkit.createInventory(null, size, title);

        List<MaterialsManager.ItemEntry> entries = new ArrayList<>(plugin.getOrderModule().getMaterialsManager().getEntries().values());
        pageCache.put(player.getUniqueId(), entries);

        List<Integer> itemSlots = GuiUtil.parseSlots(cfg.getString("item-slots", "0-44"));
        int perPage = itemSlots.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / perPage));
        page = Math.max(0, Math.min(page, totalPages - 1));
        playerPage.put(player.getUniqueId(), page);

        if (cfg.getBoolean("filler.enabled", true)) {
            ItemStack filler = GuiUtil.buildFiller(cfg.getConfigurationSection("filler"));
            for (int i = 0; i < size; i++) inv.setItem(i, filler);
        }

        int start = page * perPage;
        for (int i = 0; i < perPage && start + i < entries.size(); i++) {
            MaterialsManager.ItemEntry entry = entries.get(start + i);
            inv.setItem(itemSlots.get(i), plugin.getOrderModule().getMaterialsManager().buildItemStack(entry, plugin.getOrderModule().getConfigManager()));
        }

        if (page > 0) inv.setItem(cfg.getInt("prev-page-slot", 45), GuiUtil.buildItem(cfg.getConfigurationSection("prev-page")));
        if (page < totalPages - 1) inv.setItem(cfg.getInt("next-page-slot", 53), GuiUtil.buildItem(cfg.getConfigurationSection("next-page")));
        if (cfg.getConfigurationSection("close-button") != null) inv.setItem(cfg.getInt("close-button-slot", 49), GuiUtil.buildItem(cfg.getConfigurationSection("close-button")));
        if (cfg.getConfigurationSection("back-button") != null) inv.setItem(cfg.getInt("back-button-slot", 45), GuiUtil.buildItem(cfg.getConfigurationSection("back-button")));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isOurInventory(event.getInventory())) return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        FileConfiguration cfg = plugin.getOrderModule().getConfigManager().getGuiConfig("list-materials");
        int slot = event.getRawSlot();
        int page = playerPage.getOrDefault(player.getUniqueId(), 0);

        if (slot == cfg.getInt("prev-page-slot", 45)) { open(player, page - 1); return; }
        if (slot == cfg.getInt("next-page-slot", 53)) { open(player, page + 1); return; }
        if (slot == cfg.getInt("close-button-slot", 49)) { player.closeInventory(); return; }
        if (slot == cfg.getInt("back-button-slot", 45)) { plugin.getOrderModule().getYourOrdersGUI().open(player); return; }

        List<Integer> itemSlots = GuiUtil.parseSlots(cfg.getString("item-slots", "0-44"));
        int idx = itemSlots.indexOf(slot);
        if (idx < 0) return;
        List<MaterialsManager.ItemEntry> entries = pageCache.get(player.getUniqueId());
        int entryIdx = page * itemSlots.size() + idx;
        if (entries == null || entryIdx >= entries.size()) return;

        MaterialsManager.ItemEntry entry = entries.get(entryIdx);
        plugin.getOrderModule().getNewOrderGUI().open(player, entry);
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
        if (inv == null) return false;
        String title = plugin.getOrderModule().getConfigManager().getGuiConfig("list-materials").getString("title", "&8Select Item");
        return inv.getViewers().stream().anyMatch(v -> {
            try { return v.getOpenInventory().getTitle().equals(ColorUtil.color(title)); } catch (Exception e) { return false; }
        });
    }
}
