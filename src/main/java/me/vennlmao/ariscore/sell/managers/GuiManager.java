package me.vennlmao.ariscore.sell.managers;

import me.vennlmao.ariscore.sell.SellModule;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class GuiManager {

    private final SellModule module;
    private final Map<String, FileConfiguration> guis = new HashMap<>();

    public GuiManager(SellModule module) {
        this.module = module;
        load();
    }

    public void load() {
        File guiFolder = new File(module.getPlugin().getDataFolder(), "sell/gui");
        if (!guiFolder.exists()) guiFolder.mkdirs();

        if (!new File(guiFolder, "sellhistory.yml").exists()) module.getPlugin().saveResource("sell/gui/sellhistory.yml", false);
        if (!new File(guiFolder, "worth.yml").exists()) module.getPlugin().saveResource("sell/gui/worth.yml", false);
        if (!new File(guiFolder, "sellmulti.yml").exists()) module.getPlugin().saveResource("sell/gui/sellmulti.yml", false);
        if (!new File(guiFolder, "sellwand.yml").exists()) module.getPlugin().saveResource("sell/gui/sellwand.yml", false);

        File[] files = guiFolder.listFiles();
        if (files == null) return;

        guis.clear();
        for (File file : files) {
            if (file.getName().endsWith(".yml")) {
                guis.put(file.getName().replace(".yml", ""), YamlConfiguration.loadConfiguration(file));
            }
        }
    }

    public FileConfiguration getGuiConfig(String name) {
        return guis.get(name);
    }
}
