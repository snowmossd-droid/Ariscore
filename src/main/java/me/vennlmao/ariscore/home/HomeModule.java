package me.vennlmao.ariscore.home;

import me.vennlmao.ariscore.home.commands.*;
import me.vennlmao.ariscore.home.listeners.HomeDamageListener;
import me.vennlmao.ariscore.home.listeners.HomeMoveListener;
import me.vennlmao.ariscore.home.listeners.HomesListener;
import me.vennlmao.ariscore.home.managers.DatabaseManager;
import me.vennlmao.ariscore.home.managers.HomeManager;
import me.vennlmao.ariscore.home.managers.HomeWarmupManager;
import me.vennlmao.ariscore.home.utils.MessageUtil;
import me.vennlmao.ariscore.home.utils.SoundUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class HomeModule {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private DatabaseManager databaseManager;
    private HomeManager homeManager;
    private HomeWarmupManager warmupManager;
    private HomesListener homesListener;

    public HomeModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        loadConfig();

        databaseManager = new DatabaseManager(this);
        databaseManager.init();

        homeManager = new HomeManager(this, databaseManager);
        warmupManager = new HomeWarmupManager(this);
        homesListener = new HomesListener(this);

        SoundUtil.init(this);
        MessageUtil.init(this);

        plugin.getServer().getPluginManager().registerEvents(homesListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(new HomeMoveListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new HomeDamageListener(this), plugin);

        plugin.getCommand("homes").setExecutor(new HomesCommand(this));

        HomeCommand homeCmd = new HomeCommand(this);
        plugin.getCommand("home").setExecutor(homeCmd);
        plugin.getCommand("home").setTabCompleter(homeCmd);

        plugin.getCommand("sethome").setExecutor(new SetHomeCommand(this));

        DelHomeCommand delHomeCmd = new DelHomeCommand(this);
        plugin.getCommand("delhome").setExecutor(delHomeCmd);
        plugin.getCommand("delhome").setTabCompleter(delHomeCmd);
    }

    public void disable() {
        if (warmupManager != null) warmupManager.cancelAll();
        if (databaseManager != null) databaseManager.close();
    }

    public void reload() {
        loadConfig();
    }

    private void loadConfig() {
        File folder = new File(plugin.getDataFolder(), "home");
        if (!folder.exists()) folder.mkdirs();
        File file = new File(folder, "config.yml");
        if (!file.exists()) plugin.saveResource("home/config.yml", false);
        config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() { return config; }
    public JavaPlugin getPlugin() { return plugin; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public HomeManager getHomeManager() { return homeManager; }
    public HomeWarmupManager getWarmupManager() { return warmupManager; }
    public HomesListener getHomesListener() { return homesListener; }
}
