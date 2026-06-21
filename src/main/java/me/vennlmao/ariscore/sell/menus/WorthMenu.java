package me.vennlmao.ariscore.sell.menus;

import me.vennlmao.ariscore.sell.SellModule;
import me.vennlmao.ariscore.sell.utils.ColorUtil;
import me.vennlmao.ariscore.sell.utils.FormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class WorthMenu implements InventoryHolder {

    public enum SortType { PRICE_HIGHEST, PRICE_LOWEST, NAME_AZ, NAME_ZA }

    private final SellModule module;
    private final Inventory inventory;
    private final FileConfiguration config;
    private int page = 0;
    private SortType sortType = SortType.PRICE_HIGHEST;
    private String filter = null;
    private List<Map.Entry<Material, Double>> cachedEntries;
    private List<Map.Entry<Material, Double>> filteredEntries;
    private SortType lastSortType;
    private String lastFilter;
    private final Map<String, ItemStack> controlItemCache = new HashMap<>();
    private ItemStack fillerItem;

    public WorthMenu(SellModule module) {
        this.module = module;
        this.config = module.getGuiManager().getGuiConfig("worth");
        int rows = config.getInt("worth.rows", 6);
        this.inventory = Bukkit.createInventory(this, rows * 9, ColorUtil.colorize(config.getString("worth.title")));
        update();
    }

    public void update() {
        if (fillerItem == null && config.getBoolean("worth.filler.enabled")) {
            fillerItem = createFiller();
        }
        for (int i = 0; i < 45; i++) {
            inventory.setItem(i, fillerItem);
        }

        if (cachedEntries == null) {
            cachedEntries = new ArrayList<>(module.getPriceManager().getPrices().entrySet());
        }

        if (filteredEntries == null || !Objects.equals(filter, lastFilter) || sortType != lastSortType) {
            List<Map.Entry<Material, Double>> entries = new ArrayList<>(cachedEntries);
            if (filter != null && !filter.isEmpty()) {
                String lowerFilter = filter.toLowerCase();
                entries.removeIf(entry -> !entry.getKey().name().toLowerCase().contains(lowerFilter));
            }
            if (entries.size() > 1) {
                switch (sortType) {
                    case PRICE_HIGHEST: entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue())); break;
                    case PRICE_LOWEST: entries.sort(Comparator.comparingDouble(Map.Entry::getValue)); break;
                    case NAME_AZ: entries.sort(Comparator.comparing(a -> a.getKey().name())); break;
                    case NAME_ZA: entries.sort((a, b) -> b.getKey().name().compareTo(a.getKey().name())); break;
                }
            }
            filteredEntries = entries;
            lastFilter = filter;
            lastSortType = sortType;
        }

        int start = page * 45;
        int end = Math.min(start + 45, filteredEntries.size());
        for (int j = start; j < end; j++) {
            Map.Entry<Material, Double> entry = filteredEntries.get(j);
            inventory.setItem(j - start, createPriceItem(entry.getKey(), entry.getValue()));
        }

        setupControls(filteredEntries.size());
    }

    private ItemStack createPriceItem(Material material, double price) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize(config.getString("worth.item.displayname")
                    .replace("{item-name}", FormatUtils.formatItemName(material.name()))));
            List<String> loreStrings = config.getStringList("worth.item.lore");
            List<String> lore = new ArrayList<>(loreStrings.size());
            String formattedPrice = FormatUtils.formatPrice(price);
            for (String line : loreStrings) {
                lore.add(ColorUtil.colorize(line.replace("{item-price}", formattedPrice)));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void setupControls(int totalItems) {
        if (page > 0) {
            inventory.setItem(config.getInt("worth.navigation.previous-page-slot"), getControlItem("previous-page"));
        } else {
            inventory.setItem(config.getInt("worth.navigation.previous-page-slot"), fillerItem);
        }

        if (totalItems > (page + 1) * 45) {
            inventory.setItem(config.getInt("worth.navigation.next-page-slot"), getControlItem("next-page"));
        } else {
            inventory.setItem(config.getInt("worth.navigation.next-page-slot"), fillerItem);
        }

        inventory.setItem(config.getInt("worth.navigation.sort-button.slot"), createSortItem());
        inventory.setItem(config.getInt("worth.navigation.close-button.slot"), getControlItem("close-button"));
        inventory.setItem(config.getInt("worth.navigation.search-button.slot"), createSearchItem());
    }

    private ItemStack getControlItem(String key) {
        return controlItemCache.computeIfAbsent(key, this::createControlItem);
    }

    private ItemStack createControlItem(String key) {
        String path = "worth.navigation." + key;
        Material material = Material.valueOf(config.getString(path + ".material"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ColorUtil.colorize(config.getString(path + ".displayname")));
        List<String> configLore = config.getStringList(path + ".lore");
        List<String> lore = new ArrayList<>(configLore.size());
        for (String line : configLore) lore.add(ColorUtil.colorize(line));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSearchItem() {
        ItemStack item = createControlItem("search-button");
        ItemMeta meta = item.getItemMeta();
        List<String> configLore = config.getStringList("worth.navigation.search-button.lore");
        List<String> lore = new ArrayList<>(configLore.size());
        String currentFilter = filter == null ? "None" : filter;
        for (String line : configLore) lore.add(ColorUtil.colorize(line.replace("{filter}", currentFilter)));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSortItem() {
        ItemStack item = createControlItem("sort-button");
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();
        lore.add(ColorUtil.colorize(config.getString("worth.navigation.sort-button.switch")));
        List<String> activeLore = config.getStringList("worth.navigation.sort-button.lore-active");
        List<String> inactiveLore = config.getStringList("worth.navigation.sort-button.lore-inactive");
        SortType[] values = SortType.values();
        for (int i = 0; i < values.length; i++) {
            if (i == sortType.ordinal()) lore.add(ColorUtil.colorize(activeLore.get(i + 1)));
            else lore.add(ColorUtil.colorize(inactiveLore.get(i + 1)));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFiller() {
        Material material = Material.valueOf(config.getString("worth.filler.material", "GRAY_STAINED_GLASS_PANE"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ColorUtil.colorize(config.getString("worth.filler.displayname", " ")));
        item.setItemMeta(meta);
        return item;
    }

    public void setFilter(String filter) {
        this.filter = filter;
        this.page = 0;
        update();
    }

    public void clearFilter() {
        this.filter = null;
        this.page = 0;
        update();
    }

    public void nextPage() {
        int totalItems = (int) module.getPriceManager().getPrices().entrySet().stream()
                .filter(entry -> filter == null || entry.getKey().name().toLowerCase().contains(filter.toLowerCase()))
                .count();
        if ((page + 1) * 45 < totalItems) {
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
        update();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
