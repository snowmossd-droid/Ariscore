package me.vennlmao.ariscore.shards;

import me.vennlmao.ariscore.shards.commands.ShardsCommand;
import me.vennlmao.ariscore.shards.listeners.ShardsJoinListener;
import me.vennlmao.ariscore.shards.managers.AutoShardsManager;
import me.vennlmao.ariscore.shards.managers.ShardsDatabaseManager;
import me.vennlmao.ariscore.shards.managers.ShardsManager;
import me.vennlmao.ariscore.shards.managers.ShardsPlaceholder;
import me.vennlmao.ariscore.shards.utils.SoundUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ShardsModule {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private ShardsDatabaseManager databaseManager;
    private ShardsManager shardsManager;
    private AutoShardsManager autoShardsManager;

    public ShardsModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        loadConfig();

        databaseManager = new ShardsDatabaseManager(this);
        databaseManager.init();

        shardsManager = new ShardsManager(this, databaseManager);
        autoShardsManager = new AutoShardsManager(this);

        SoundUtil.init(this);

        autoShardsManager.start();

        plugin.getServer().getPluginManager().registerEvents(new ShardsJoinListener(this), plugin);

        ShardsCommand cmd = new ShardsCommand(this);
        plugin.getCommand("shards").setExecutor(cmd);
        plugin.getCommand("shards").setTabCompleter(cmd);
        plugin.getCommand("shard").setExecutor(cmd);
        plugin.getCommand("shard").setTabCompleter(cmd);

        if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new ShardsPlaceholder(this).register();
            plugin.getLogger().info("[Shards] PlaceholderAPI registered: %ariscore_shards%");
        }
    }

    public void disable() {
        if (autoShardsManager != null) autoShardsManager.stop();
        if (databaseManager != null) databaseManager.close();
    }

    public void reload() {
        loadConfig();
        autoShardsManager.stop();
        autoShardsManager.start();
    }

    private void loadConfig() {
        File folder = new File(plugin.getDataFolder(), "shards");
        if (!folder.exists()) folder.mkdirs();
        File file = new File(folder, "config.yml");
        if (!file.exists()) plugin.saveResource("shards/config.yml", false);
        config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() { return config; }
    public JavaPlugin getPlugin() { return plugin; }
    public ShardsDatabaseManager getDatabaseManager() { return databaseManager; }
    public ShardsManager getShardsManager() { return shardsManager; }
    public AutoShardsManager getAutoShardsManager() { return autoShardsManager; }
}
