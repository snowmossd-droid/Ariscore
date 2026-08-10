package me.vennlmao.ariscore.amethyst;

import me.vennlmao.ariscore.LicenseManager;
import me.vennlmao.ariscore.amethyst.commands.AmethystCommand;
import me.vennlmao.ariscore.amethyst.listeners.AmethystToolListener;
import me.vennlmao.ariscore.amethyst.managers.AmethystExpiryManager;
import me.vennlmao.ariscore.amethyst.managers.AmethystItemManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;

public class AmethystModule {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private AmethystItemManager itemManager;
    private AmethystExpiryManager expiryManager;

    public AmethystModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        if (!new LicenseManager(plugin).validate()) {
            plugin.getLogger().severe("[ArisCore] AmethystModule disabled: invalid license.");
            return;
        }
        loadConfig();

        itemManager = new AmethystItemManager(this);
        expiryManager = new AmethystExpiryManager(this);
        expiryManager.start();

        plugin.getServer().getPluginManager().registerEvents(new AmethystToolListener(this), plugin);

        AmethystCommand cmd = new AmethystCommand(this);
        plugin.getCommand("amethyst").setExecutor(cmd);
        plugin.getCommand("amethyst").setTabCompleter(cmd);
    }

    public void disable() {
        if (expiryManager != null) expiryManager.stop();
    }

    public void reload() {
        loadConfig();
    }

    private void loadConfig() {
        File folder = new File(plugin.getDataFolder(), "amethyst");
        if (!folder.exists()) folder.mkdirs();
        File file = new File(folder, "config.yml");
        if (!file.exists()) plugin.saveResource("amethyst/config.yml", false);
        config = YamlConfiguration.loadConfiguration(file);
    }

    public boolean isShardBoostActive(UUID uuid) {
        return itemManager.isShardBoostActive(uuid);
    }

    public double getShardBoostMultiplier() {
        return config.getDouble("tools.booster.boost-multiplier", 4.0);
    }

    public FileConfiguration getConfig() { return config; }
    public JavaPlugin getPlugin() { return plugin; }
    public AmethystItemManager getItemManager() { return itemManager; }
    public AmethystExpiryManager getExpiryManager() { return expiryManager; }
}
