package me.vennlmao.ariscore.order.managers;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.order.utils.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class OrderConfigManager {

    private final ArisCore plugin;
    private FileConfiguration config;
    private FileConfiguration lang;
    private FileConfiguration items;
    private final Map<String, FileConfiguration> guiConfigs = new HashMap<>();

    private static final String[] GUI_FILES = {
        "new-order", "your-orders", "order-view", "edit-order",
        "collect-items", "confirm-delivery", "confirm-cancel", "list-materials"
    };

    public OrderConfigManager(ArisCore plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File folder = new File(plugin.getDataFolder(), "order");
        folder.mkdirs();
        new File(folder, "gui").mkdirs();

        saveDefault("order/config.yml", new File(folder, "config.yml"));
        saveDefault("order/lang.yml", new File(folder, "lang.yml"));
        saveDefault("order/items.yml", new File(folder, "items.yml"));
        for (String guiName : GUI_FILES) {
            saveDefault("order/gui/" + guiName + ".yml", new File(folder, "gui/" + guiName + ".yml"));
        }

        config = loadYml(new File(folder, "config.yml"), "order/config.yml");
        lang = loadYml(new File(folder, "lang.yml"), "order/lang.yml");
        items = loadYml(new File(folder, "items.yml"), "order/items.yml");
        guiConfigs.clear();
        for (String guiName : GUI_FILES) {
            guiConfigs.put(guiName, loadYml(new File(folder, "gui/" + guiName + ".yml"), "order/gui/" + guiName + ".yml"));
        }
    }

    private void saveDefault(String resource, File target) {
        if (target.exists()) return;
        try (InputStream in = plugin.getResource(resource)) {
            if (in != null) Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private FileConfiguration loadYml(File file, String resource) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        try (InputStream in = plugin.getResource(resource)) {
            if (in != null) cfg.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
        } catch (Exception ignored) {}
        return cfg;
    }

    public FileConfiguration getConfig() { return config; }
    public FileConfiguration getLang() { return lang; }
    public FileConfiguration getItems() { return items; }
    public FileConfiguration getGuiConfig(String name) { return guiConfigs.getOrDefault(name, config); }

    public String msg(String path) {
        String value = lang.getString(path);
        if (value == null) value = config.getString(path, "&c[Missing: " + path + "]");
        return ColorUtil.color(value);
    }

    public String msg(String path, String... replacements) {
        String message = msg(path);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        return message;
    }
}
