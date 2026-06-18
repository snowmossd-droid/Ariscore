package me.vennlmao.ariscore.shop;

import me.vennlmao.ariscore.shop.commands.ShopCommand;
import me.vennlmao.ariscore.shop.listeners.ShopListener;
import me.vennlmao.ariscore.shop.managers.ShardsManager;
import me.vennlmao.ariscore.shop.managers.ShopManager;
import me.vennlmao.ariscore.shop.utils.SoundUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ShopModule {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private Economy economy;
    private ShopManager shopManager;
    private ShardsManager shardsManager;
    private ShopListener shopListener;

    public ShopModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        loadConfig();
        setupEconomy();

        shopManager = new ShopManager(this);
        shardsManager = new ShardsManager(this);
        shopListener = new ShopListener(this);

        SoundUtil.init(this);

        plugin.getServer().getPluginManager().registerEvents(shopListener, plugin);
        plugin.getCommand("shop").setExecutor(new ShopCommand(this));
    }

    public void disable() {}

    public void reload() { loadConfig(); }

    private void loadConfig() {
        File folder = new File(plugin.getDataFolder(), "shop");
        if (!folder.exists()) folder.mkdirs();
        File file = new File(folder, "config.yml");
        if (!file.exists()) plugin.saveResource("shop/config.yml", false);
        config = YamlConfiguration.loadConfiguration(file);

        String[] cats = {"food", "gear", "nether", "shards", "end"};
        File catFolder = new File(plugin.getDataFolder(), "shop/categories");
        if (!catFolder.exists()) catFolder.mkdirs();
        for (String cat : cats) {
            File f = new File(catFolder, cat + ".yml");
            if (!f.exists()) plugin.saveResource("shop/categories/" + cat + ".yml", false);
        }
    }

    private void setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) return;
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager()
                .getRegistration(Economy.class);
        if (rsp != null) economy = rsp.getProvider();
    }

    public FileConfiguration getConfig() { return config; }
    public JavaPlugin getPlugin() { return plugin; }
    public Economy getEconomy() { return economy; }
    public ShopManager getShopManager() { return shopManager; }
    public ShardsManager getShardsManager() { return shardsManager; }
    public ShopListener getShopListener() { return shopListener; }
}
