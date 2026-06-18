package me.vennlmao.ariscore.sell.menus;

import me.vennlmao.ariscore.sell.SellModule;
import me.vennlmao.ariscore.sell.utils.ColorUtil;
import me.vennlmao.ariscore.sell.utils.FormatUtils;
import me.vennlmao.ariscore.sell.utils.SaleEntry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class SellHistoryMenu implements InventoryHolder {

    public enum SortType { PRICE_HIGHEST, PRICE_LOWEST, NAME_AZ, NAME_ZA }

    private final SellModule module;
    private final Player player;
    private final Inventory inventory;
    private final FileConfiguration config;
    private int page = 0;
    private SortType sortType = SortType.PRICE_HIGHEST;
    private String filter = null;
    private List<SaleEntry> cachedHistory;
    private List<SaleEntry> processedHistory;
    private SortType lastSortType;
    private String lastFilter;

    public SellHistoryMenu(SellModule module, Player player) {
        this.module = module;
        this.player = player;
        this.config = module.getGuiManager().getGuiConfig("sellhistory");
        int rows = config.getInt("sellhistory.rows", 6);
        this.inventory = Bukkit.createInventory(this, rows * 9, ColorUtil.colorize(config.getString("sellhistory.title")));
        update();
    }

    public void update() {
        if (cachedHistory == null) {
            inventory.clear();
            inventory.setItem(22, createLoadingItem());
            Bukkit.getAsyncScheduler().runNow((Plugin) module.getPlugin(), task -> {
                cachedHistory = module.getDataManager().getHistory(player.getUniqueId());
                Bukkit.getGlobalRegionScheduler().run((Plugin) module.getPlugin(), t -> update());
            });
            return;
        }

        inventory.clear();
        if (config.getBoolean("sellhistory.filler.enabled")) {
            ItemStack filler = createFiller();
            for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler);
        }

        boolean needsReprocess = processedHistory == null
                || sortType != lastSortType
                || !Objects.equals(filter, lastFilter);

        if (needsReprocess) {
            List<SaleEntry> history = new ArrayList<>(cachedHistory);
            if (filter != null && !filter.isEmpty()) {
                String filterLower = filter.toLowerCase();
                history.removeIf(entry -> !entry.itemName().toLowerCase().contains(filterLower));
            }
            if (history.size() > 1) {
                switch (sortType) {
                    case PRICE_HIGHEST: history.sort((a, b) -> Double.compare(b.price(), a.price())); break;
                    case PRICE_LOWEST: history.sort(Comparator.comparingDouble(SaleEntry::price)); break;
                    case NAME_AZ: history.sort(Comparator.comparing(SaleEntry::itemName)); break;
                    case NAME_ZA: history.sort((a, b) -> b.itemName().compareTo(a.itemName())); break;
                }
            }
            processedHistory = history;
            lastSortType = sortType;
            lastFilter = filter;
        }

        int start = page * 45;
        int end = Math.min(start + 45, processedHistory.size());
        for (int j = start; j < end; j++) {
            inventory.setItem(j - start, createHistoryItem(processedHistory.get(j)));
        }

        setupControls(processedHistory.size());
    }

    private ItemStack createLoadingItem() {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ColorUtil.colorize("&eLoading..."));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createHistoryItem(SaleEntry entry) {
        Material material = Material.matchMaterial(entry.itemName());
        if (material == null) material = Material.BARRIER;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize(config.getString("sellhistory.item.displayname")
                    .replace("%item%", FormatUtils.formatItemName(entry.itemName()))));
            List<String> rawLore = config.getStringList("sellhistory.item.lore");
            List<String> lore = new ArrayList<>(rawLore.size());
            for (String line : rawLore) {
                lore.add(ColorUtil.colorize(line
                        .replace("%item-value%", FormatUtils.formatPrice(entry.price()))
                        .replace("%item-quantity%", String.valueOf(entry.quantity()))));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void setupControls(int totalItems) {
        if (page > 0) {
            inventory.setItem(config.getInt("sellhistory.previous-page-slot"), createControlItem("previous-page"));
        }
        if (totalItems > (page + 1) * 45) {
            inventory.setItem(config.getInt("sellhistory.next-page-slot"), createControlItem("next-page"));
        }
        inventory.setItem(config.getInt("sellhistory.stats-book.slot"), createStatsItem());
        inventory.setItem(config.getInt("sellhistory.sort-button.slot"), createSortItem());
        inventory.setItem(config.getInt("sellhistory.back-button.slot"), createControlItem("back-button"));
        inventory.setItem(config.getInt("sellhistory.search-button.slot"), createSearchItem());
    }

    private ItemStack createControlItem(String key) {
        Material material = Material.valueOf(config.getString("sellhistory." + key + ".material"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ColorUtil.colorize(config.getString("sellhistory." + key + ".displayname")));
        List<String> rawLore = config.getStringList("sellhistory." + key + ".lore");
        List<String> lore = new ArrayList<>(rawLore.size());
        for (String line : rawLore) lore.add(ColorUtil.colorize(line));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSearchItem() {
        ItemStack item = createControlItem("search-button");
        ItemMeta meta = item.getItemMeta();
        List<String> rawLore = config.getStringList("sellhistory.search-button.lore");
        List<String> lore = new ArrayList<>(rawLore.size());
        String currentFilter = filter == null ? "None" : filter;
        for (String line : rawLore) lore.add(ColorUtil.colorize(line.replace("{filter}", currentFilter)));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStatsItem() {
        ItemStack item = createControlItem("stats-book");
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer((OfflinePlayer) player);
        }
        List<SaleEntry> history = module.getDataManager().getHistory(player.getUniqueId());
        int totalSold = history.stream().mapToInt(SaleEntry::quantity).sum();
        double totalMade = history.stream().mapToDouble(SaleEntry::price).sum();
        List<String> rawLore = config.getStringList("sellhistory.stats-book.lore");
        List<String> lore = new ArrayList<>(rawLore.size());
        for (String line : rawLore) {
            lore.add(ColorUtil.colorize(line
                    .replace("{total-sold}", String.valueOf(totalSold))
                    .replace("{total-made}", FormatUtils.formatPrice(totalMade))));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSortItem() {
        ItemStack item = createControlItem("sort-button");
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();
        lore.add(ColorUtil.colorize(config.getString("sellhistory.sort-button.switch")));
        List<String> activeLore = config.getStringList("sellhistory.sort-button.lore-active");
        List<String> inactiveLore = config.getStringList("sellhistory.sort-button.lore-inactive");
        for (int i = 0; i < SortType.values().length; i++) {
            if (i == sortType.ordinal()) lore.add(ColorUtil.colorize(activeLore.get(i + 1)));
            else lore.add(ColorUtil.colorize(inactiveLore.get(i + 1)));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFiller() {
        Material material = Material.valueOf(config.getString("sellhistory.filler.material", "GRAY_STAINED_GLASS_PANE"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ColorUtil.colorize(config.getString("sellhistory.filler.displayname", " ")));
        item.setItemMeta(meta);
        return item;
    }

    public void setFilter(String filter) {
        this.filter = filter;
        this.page = 0;
        this.processedHistory = null;
        update();
    }

    public void clearFilter() {
        this.filter = null;
        this.page = 0;
        this.processedHistory = null;
        update();
    }

    public void nextPage() {
        int total = processedHistory == null ? 0 : processedHistory.size();
        if ((page + 1) * 45 < total) {
            page++;
            update();
        }
    }

    public void prevPage() {
        if (page > 0) {
            page--;
            update();
        }
    }

    public void cycleSort() {
        sortType = SortType.values()[(sortType.ordinal() + 1) % SortType.values().length];
        processedHistory = null;
        update();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
