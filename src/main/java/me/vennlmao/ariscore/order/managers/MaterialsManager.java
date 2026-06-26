package me.vennlmao.ariscore.order.managers;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.order.utils.ColorUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MaterialsManager {

    private final ArisCore plugin;
    private final Map<String, ItemEntry> entries = new LinkedHashMap<>();

    public MaterialsManager(ArisCore plugin) {
        this.plugin = plugin;
    }

    public void load() {
        entries.clear();
        FileConfiguration items = plugin.getOrderModule().getConfigManager().getItems();
        ConfigurationSection categories = items.getConfigurationSection("categories");
        if (categories == null) return;
        for (String category : categories.getKeys(false)) {
            ConfigurationSection catSection = categories.getConfigurationSection(category);
            if (catSection == null) continue;
            ConfigurationSection itemSection = catSection.getConfigurationSection("items");
            if (itemSection == null) continue;
            for (String itemKey : itemSection.getKeys(false)) {
                ConfigurationSection itemCfg = itemSection.getConfigurationSection(itemKey);
                if (itemCfg == null) continue;
                String matName = itemCfg.getString("material", itemKey);
                Material material = Material.getMaterial(matName.toUpperCase());
                if (material == null) continue;
                String itemType = itemCfg.getString("itemType");
                String subType = itemCfg.getString("subType");
                double defaultPrice = itemCfg.getDouble("default-price", 0.0);
                double minPrice = itemCfg.getDouble("min-price", 0.0);
                double maxPrice = itemCfg.getDouble("max-price", Double.MAX_VALUE);
                String displayName = itemCfg.getString("name", ColorUtil.color("&f" + OrderItem.formatMaterialName(matName)));
                List<String> lore = itemCfg.getStringList("lore");
                entries.put(itemKey, new ItemEntry(itemKey, material, itemType, subType, displayName, lore, defaultPrice, minPrice, maxPrice, category));
            }
        }
    }

    public ItemEntry getEntry(String itemId) { return entries.get(itemId); }
    public Map<String, ItemEntry> getEntries() { return Collections.unmodifiableMap(entries); }

    public List<ItemEntry> getEntriesByCategory(String category) {
        List<ItemEntry> list = new ArrayList<>();
        for (ItemEntry e : entries.values()) if (category.equals(e.getCategory())) list.add(e);
        return list;
    }

    public List<String> getCategories() {
        List<String> cats = new ArrayList<>();
        for (ItemEntry e : entries.values()) if (!cats.contains(e.getCategory())) cats.add(e.getCategory());
        return cats;
    }

    public ItemStack buildItemStack(ItemEntry entry, OrderConfigManager configManager) {
        ItemStack item = new ItemStack(entry.getMaterial(), 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(ColorUtil.color(entry.getDisplayName()));
        List<String> coloredLore = new ArrayList<>();
        for (String line : entry.getLore()) coloredLore.add(ColorUtil.color(line));
        meta.setLore(coloredLore);
        item.setItemMeta(meta);
        return item;
    }

    public static class ItemEntry {
        private final String id;
        private final Material material;
        private final String itemType;
        private final String subType;
        private final String displayName;
        private final List<String> lore;
        private final double defaultPrice;
        private final double minPrice;
        private final double maxPrice;
        private final String category;

        public ItemEntry(String id, Material material, String itemType, String subType,
                         String displayName, List<String> lore,
                         double defaultPrice, double minPrice, double maxPrice, String category) {
            this.id = id; this.material = material; this.itemType = itemType; this.subType = subType;
            this.displayName = displayName; this.lore = lore;
            this.defaultPrice = defaultPrice; this.minPrice = minPrice; this.maxPrice = maxPrice;
            this.category = category;
        }

        public String getId() { return id; }
        public Material getMaterial() { return material; }
        public String getItemType() { return itemType; }
        public String getSubType() { return subType; }
        public String getDisplayName() { return displayName; }
        public List<String> getLore() { return lore; }
        public double getDefaultPrice() { return defaultPrice; }
        public double getMinPrice() { return minPrice; }
        public double getMaxPrice() { return maxPrice; }
        public String getCategory() { return category; }
    }
}
