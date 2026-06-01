package me.vennlmao.ariscore.shop.managers;

import me.vennlmao.ariscore.shop.ShopModule;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class ShopManager {

    private final ShopModule plugin;
    private final Map<String, List<ShopItem>> categoryItems = new HashMap<>();
    private final List<String> categories = new ArrayList<>();

    public ShopManager(ShopModule plugin) {
        this.plugin = plugin;
        loadAll();
    }

    private void loadAll() {
        File dir = new File(plugin.getPlugin().getDataFolder(), "categories");
        if (!dir.exists()) dir.mkdirs();

        String[] cats = {"food", "gear", "nether", "shards", "end"};
        for (String cat : cats) {
            File f = new File(dir, cat + ".yml");
            if (!f.exists()) {
                plugin.getPlugin().saveResource("categories/" + cat + ".yml", false);
            }
            loadCategory(cat, f);
            categories.add(cat);
        }
    }

    private void loadCategory(String category, File file) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        List<ShopItem> items = new ArrayList<>();

        if (!cfg.contains("items")) return;

        for (String key : cfg.getConfigurationSection("items").getKeys(false)) {
            String path = "items." + key;
            int slot = cfg.getInt(path + ".slot", 0);
            String matName = cfg.getString(path + ".material", "STONE");
            Material mat = Material.matchMaterial(matName);
            if (mat == null) mat = Material.STONE;
            String name = cfg.getString(path + ".name", key);
            List<String> lore = cfg.getStringList(path + ".lore");
            int stack = cfg.getInt(path + ".stack", 1);
            double price = cfg.getDouble(path + ".price", 0);
            boolean isShard = cfg.getBoolean(path + ".isShard", false);
            List<String> commands = cfg.getStringList(path + ".commands");

            items.add(new ShopItem(key, slot, mat, name, lore, stack, price, isShard, commands, category));
        }

        categoryItems.put(category, items);
    }

    public List<ShopItem> getItems(String category) {
        return categoryItems.getOrDefault(category, new ArrayList<>());
    }

    public List<String> getCategories() {
        return categories;
    }

    public ShopItem findItem(String category, int slot) {
        for (ShopItem item : getItems(category)) {
            if (item.getSlot() == slot) return item;
        }
        return null;
    }
}
