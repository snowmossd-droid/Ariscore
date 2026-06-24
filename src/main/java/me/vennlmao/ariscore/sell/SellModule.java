package me.vennlmao.ariscore.sell;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.sell.commands.SellAdminCommand;
import me.vennlmao.ariscore.sell.commands.SellCommand;
import me.vennlmao.ariscore.sell.commands.SellHistoryCommand;
import me.vennlmao.ariscore.sell.commands.SellMultiCommand;
import me.vennlmao.ariscore.sell.commands.SellWandCommand;
import me.vennlmao.ariscore.sell.commands.WorthCommand;
import me.vennlmao.ariscore.sell.listeners.HistoryListener;
import me.vennlmao.ariscore.sell.listeners.JoinListener;
import me.vennlmao.ariscore.sell.listeners.MultiListener;
import me.vennlmao.ariscore.sell.listeners.QuitListener;
import me.vennlmao.ariscore.sell.listeners.SearchListener;
import me.vennlmao.ariscore.sell.listeners.SellListener;
import me.vennlmao.ariscore.sell.listeners.SellWandListener;
import me.vennlmao.ariscore.sell.listeners.WorthListener;
import me.vennlmao.ariscore.sell.managers.ChatSignManager;
import me.vennlmao.ariscore.sell.managers.GuiManager;
import me.vennlmao.ariscore.sell.managers.PriceManager;
import me.vennlmao.ariscore.sell.managers.SearchManager;
import me.vennlmao.ariscore.sell.managers.SellDataManager;
import me.vennlmao.ariscore.sell.managers.SellDatabaseManager;
import me.vennlmao.ariscore.sell.managers.SellWandManager;
import me.vennlmao.ariscore.sell.utils.SoundUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.File;

public class SellModule {

    private final ArisCore plugin;
    private FileConfiguration config;
    private File configFile;
    private Economy economy;

    private PriceManager priceManager;
    private GuiManager guiManager;
    private SellDatabaseManager databaseManager;
    private SellDataManager dataManager;
    private SellWandManager wandManager;
    private SearchManager searchManager;
    private ChatSignManager chatSignManager;

    public SellModule(ArisCore plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        loadConfig();

        if (!setupEconomy()) {
            plugin.getLogger().severe("[Sell] Vault not found! Sell module disabled.");
            return;
        }

        guiManager = new GuiManager(this);
        priceManager = new PriceManager(this);
        databaseManager = new SellDatabaseManager(this);
        databaseManager.init();
        dataManager = new SellDataManager(this, databaseManager);
        wandManager = new SellWandManager(this);
        searchManager = new SearchManager();
        chatSignManager = new ChatSignManager(this);
        chatSignManager.startCleanupTask();

        SoundUtil.init(this);

        plugin.getCommand("sell").setExecutor(new SellCommand(this));
        plugin.getCommand("worth").setExecutor(new WorthCommand(this));
        plugin.getCommand("sellhistory").setExecutor(new SellHistoryCommand(this));
        plugin.getCommand("sellmulti").setExecutor(new SellMultiCommand(this));

        SellWandCommand sellWandCommand = new SellWandCommand(this);
        plugin.getCommand("sellwand").setExecutor(sellWandCommand);
        plugin.getCommand("sellwand").setTabCompleter(sellWandCommand);

        SellAdminCommand sellAdminCommand = new SellAdminCommand(this);
        plugin.getCommand("selladmin").setExecutor(sellAdminCommand);
        plugin.getCommand("selladmin").setTabCompleter(sellAdminCommand);

        plugin.getServer().getPluginManager().registerEvents(new SellListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new WorthListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new HistoryListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new MultiListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new SearchListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(chatSignManager, plugin);
        plugin.getServer().getPluginManager().registerEvents(new SellWandListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new JoinListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new QuitListener(this), plugin);

        plugin.getLogger().info("[Sell] Module enabled.");
    }

    public void disable() {
        if (databaseManager != null) databaseManager.close();
        plugin.getLogger().info("[Sell] Module disabled.");
    }

    public void reload() {
        loadConfig();
        guiManager.load();
        priceManager.load();
    }

    private void loadConfig() {
        File folder = new File(plugin.getDataFolder(), "sell");
        if (!folder.exists()) folder.mkdirs();

        configFile = new File(folder, "config.yml");
        if (!configFile.exists()) plugin.saveResource("sell/config.yml", false);

        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public ArisCore getPlugin() { return plugin; }
    public FileConfiguration getConfig() { return config; }
    public Economy getEconomy() { return economy; }
    public PriceManager getPriceManager() { return priceManager; }
    public GuiManager getGuiManager() { return guiManager; }
    public SellDatabaseManager getDatabaseManager() { return databaseManager; }
    public SellDataManager getDataManager() { return dataManager; }
    public SellWandManager getWandManager() { return wandManager; }
    public SearchManager getSearchManager() { return searchManager; }
    public ChatSignManager getChatSignManager() { return chatSignManager; }
}
