package me.vennlmao.ariscore.crates;

import me.vennlmao.ariscore.crates.commands.CrateCommand;
import me.vennlmao.ariscore.crates.listeners.CrateListener;
import me.vennlmao.ariscore.crates.listeners.PlayerConnectionListener;
import me.vennlmao.ariscore.crates.managers.CrateConfigManager;
import me.vennlmao.ariscore.crates.managers.CrateRegistry;
import me.vennlmao.ariscore.crates.managers.GamerDataManager;
import me.vennlmao.ariscore.crates.managers.KeyAllManager;
import me.vennlmao.ariscore.crates.managers.PlayerStorageManager;
import me.vennlmao.ariscore.crates.models.ConfirmGuiConfig;
import me.vennlmao.ariscore.crates.utils.MessageUtil;
import me.vennlmao.ariscore.crates.views.ConfirmRewardView;
import me.vennlmao.ariscore.crates.views.CrateEditView;
import me.vennlmao.ariscore.crates.views.CrateRewardsView;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import me.vennlmao.ariscore.LicenseManager;

import java.io.File;

public class CratesModule {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private MessageUtil messageUtil;

    private CrateConfigManager crateConfigManager;
    private CrateRegistry crateRegistry;
    private GamerDataManager gamerDataManager;
    private PlayerStorageManager playerStorageManager;
    private KeyAllManager keyAllManager;
    private ConfirmGuiConfig globalConfirmGui;

    public CratesModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        if (!new LicenseManager(plugin).validate()) {
            plugin.getLogger().severe("[ArisCore] CratesModule disabled: invalid license.");
            return;
        }
        loadConfig();
        saveDefaultCrateResources();

        messageUtil = new MessageUtil(this);
        crateRegistry = new CrateRegistry();
        gamerDataManager = new GamerDataManager();
        playerStorageManager = new PlayerStorageManager(this);
        crateConfigManager = new CrateConfigManager(this);
        globalConfirmGui = crateConfigManager.buildGlobalConfirmGui();
        crateConfigManager.loadAll();

        keyAllManager = new KeyAllManager(this);
        keyAllManager.start();

        plugin.getServer().getPluginManager().registerEvents(new CrateListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new CrateRewardsView.ClickListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new ConfirmRewardView.ClickListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new CrateEditView.ClickListener(this), plugin);

        CrateCommand cmd = new CrateCommand(this);
        plugin.getCommand("crates").setExecutor(cmd);
        plugin.getCommand("crates").setTabCompleter(cmd);

        if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new CratesExpansion(this).register();
        }
    }

    public void disable() {
        if (keyAllManager != null) keyAllManager.stop();
        if (playerStorageManager != null) playerStorageManager.saveAll(gamerDataManager);
    }

    public void reload() {
        loadConfig();
        messageUtil = new MessageUtil(this);
        crateRegistry.clear();
        globalConfirmGui = crateConfigManager.buildGlobalConfirmGui();
        crateConfigManager.loadAll();
        if (keyAllManager != null) {
            keyAllManager.stop();
            keyAllManager.start();
        }
    }

    private void loadConfig() {
        File folder = new File(plugin.getDataFolder(), "crates");
        if (!folder.exists()) folder.mkdirs();
        File file = new File(folder, "config.yml");
        if (!file.exists()) plugin.saveResource("crates/config.yml", false);
        config = YamlConfiguration.loadConfiguration(file);
    }

    private void saveDefaultCrateResources() {
        File cratesFolder = new File(plugin.getDataFolder(), "crates/crates");
        if (!cratesFolder.exists()) {
            cratesFolder.mkdirs();
            plugin.saveResource("crates/crates/example-crate.yml", false);
        }
        File locationsFile = new File(plugin.getDataFolder(), "crates/locations.yml");
        if (!locationsFile.exists()) plugin.saveResource("crates/locations.yml", false);
    }

    public JavaPlugin getPlugin() { return plugin; }
    public FileConfiguration getConfig() { return config; }
    public MessageUtil getMessageUtil() { return messageUtil; }
    public CrateRegistry getCrateRegistry() { return crateRegistry; }
    public GamerDataManager getGamerDataManager() { return gamerDataManager; }
    public PlayerStorageManager getPlayerStorageManager() { return playerStorageManager; }
    public CrateConfigManager getCrateConfigManager() { return crateConfigManager; }
    public KeyAllManager getKeyAllManager() { return keyAllManager; }
    public ConfirmGuiConfig getGlobalConfirmGui() { return globalConfirmGui; }
}
