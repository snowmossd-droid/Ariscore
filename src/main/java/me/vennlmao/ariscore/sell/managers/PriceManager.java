package me.vennlmao.ariscore.sell.managers;

import me.vennlmao.ariscore.sell.SellModule;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PriceManager {

    private final SellModule module;
    private final Map<Material, Double> prices = new HashMap<>();
    private final Map<Material, String> categories = new HashMap<>();
    private File file;

    public PriceManager(SellModule module) {
        this.module = module;
        load();
    }

    public void load() {
        File folder = new File(module.getPlugin().getDataFolder(), "sell");
        if (!folder.exists()) folder.mkdirs();

        file = new File(folder, "price.yml");
        if (!file.exists()) module.getPlugin().saveResource("sell/price.yml", false);

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        prices.clear();
        categories.clear();

        for (String sectionName : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(sectionName);
            if (section == null) continue;
            for (String key : section.getKeys(false)) {
                try {
                    Material material = Material.valueOf(key.toUpperCase());
                    if (!material.isItem()) continue;
                    double price = section.getDouble(key);
                    prices.put(material, price);
                    categories.put(material, sectionName.toLowerCase());
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public double getPrice(Material material) {
        return prices.getOrDefault(material, 0.0);
    }

    public String getCategory(Material material) {
        return categories.get(material);
    }

    public Map<Material, Double> getPrices() {
        return prices;
    }
}
