package me.vennlmao.ariscore.spawn;

import me.vennlmao.ariscore.spawn.commands.DelSpawnCommand;
import me.vennlmao.ariscore.spawn.commands.SetSpawnCommand;
import me.vennlmao.ariscore.spawn.commands.SpawnCommand;
import me.vennlmao.ariscore.spawn.commands.SpawnsCommand;
import me.vennlmao.ariscore.spawn.listeners.SpawnDamageListener;
import me.vennlmao.ariscore.spawn.listeners.SpawnMoveListener;
import me.vennlmao.ariscore.spawn.listeners.SpawnsGuiListener;
import me.vennlmao.ariscore.spawn.managers.SpawnDatabaseManager;
import me.vennlmao.ariscore.spawn.managers.SpawnManager;
import me.vennlmao.ariscore.spawn.managers.SpawnWarmupManager;
import me.vennlmao.ariscore.spawn.utils.MessageUtil;
import me.vennlmao.ariscore.spawn.utils.SoundUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import me.vennlmao.ariscore.LicenseManager;

import java.io.File;

public class SpawnModule {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private SpawnDatabaseManager databaseManager;
    private SpawnManager spawnManager;
    private SpawnWarmupManager warmupManager;
    private SpawnsGuiListener guiListener;

    public SpawnModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        if (!new LicenseManager(plugin).validate()) {
            plugin.getLogger().severe("[ArisCore] SpawnModule disabled: invalid license.");
            return;
        }
        loadConfig();

        databaseManager = new SpawnDatabaseManager(this);
        databaseManager.init();

        spawnManager = new SpawnManager(this, databaseManager);
        warmupManager = new SpawnWarmupManager(this);
        guiListener = new SpawnsGuiListener(this);

        SoundUtil.init(this);
        MessageUtil.init(this);

        plugin.getServer().getPluginManager().registerEvents(guiListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(new SpawnMoveListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new SpawnDamageListener(this), plugin);

        plugin.getCommand("setspawn").setExecutor(new SetSpawnCommand(this));

        DelSpawnCommand delSpawnCmd = new DelSpawnCommand(this);
        plugin.getCommand("delspawn").setExecutor(delSpawnCmd);
        plugin.getCommand("delspawn").setTabCompleter(delSpawnCmd);

        SpawnCommand spawnCmd = new SpawnCommand(this);
        plugin.getCommand("spawn").setExecutor(spawnCmd);
        plugin.getCommand("spawn").setTabCompleter(spawnCmd);

        SpawnsCommand spawnsCmd = new SpawnsCommand(this);
        plugin.getCommand("spawns").setExecutor(spawnsCmd);
    }

    public void disable() {
        if (warmupManager != null) warmupManager.cancelAll();
        if (databaseManager != null) databaseManager.close();
    }

    public void reload() {
        loadConfig();
        SoundUtil.init(this);
        MessageUtil.init(this);
    }

    private void loadConfig() {
        File folder = new File(plugin.getDataFolder(), "spawn");
        if (!folder.exists()) folder.mkdirs();
        File file = new File(folder, "config.yml");
        if (!file.exists()) plugin.saveResource("spawn/config.yml", false);
        config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() { return config; }
    public JavaPlugin getPlugin() { return plugin; }
    public SpawnDatabaseManager getDatabaseManager() { return databaseManager; }
    public SpawnManager getSpawnManager() { return spawnManager; }
    public SpawnWarmupManager getWarmupManager() { return warmupManager; }
    public SpawnsGuiListener getGuiListener() { return guiListener; }
}
