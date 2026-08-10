package me.vennlmao.ariscore.spawners;

import me.vennlmao.ariscore.LicenseManager;
import me.vennlmao.ariscore.spawners.commands.SpawnerCommand;
import me.vennlmao.ariscore.spawners.listeners.SpawnerBlockListener;
import me.vennlmao.ariscore.spawners.listeners.SpawnerGuiListener;
import me.vennlmao.ariscore.spawners.listeners.SpawnerNoSpawnListener;
import me.vennlmao.ariscore.spawners.managers.SpawnerDatabaseManager;
import me.vennlmao.ariscore.spawners.managers.SpawnerDefinitionManager;
import me.vennlmao.ariscore.spawners.managers.SpawnerManager;
import me.vennlmao.ariscore.spawners.managers.SpawnerProductionTask;
import me.vennlmao.ariscore.spawners.utils.MessageUtil;
import me.vennlmao.ariscore.spawners.utils.SoundUtil;
import me.vennlmao.ariscore.spawners.utils.SpawnerItemUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class SpawnersModule {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration guiConfig;

    private SpawnerDatabaseManager databaseManager;
    private SpawnerDefinitionManager definitionManager;
    private SpawnerManager spawnerManager;
    private SpawnerProductionTask productionTask;
    private SpawnerGuiListener guiListener;

    public SpawnersModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        if (!new LicenseManager(plugin).validate()) {
            plugin.getLogger().severe("[ArisCore] SpawnersModule disabled: invalid license.");
            return;
        }
        loadConfig();

        definitionManager = new SpawnerDefinitionManager(this);
        definitionManager.load();

        databaseManager = new SpawnerDatabaseManager(this);
        databaseManager.init();

        spawnerManager = new SpawnerManager(this, databaseManager);
        spawnerManager.loadAll();

        MessageUtil.init(this);
        SoundUtil.init(this);
        SpawnerItemUtil.init(this);

        guiListener = new SpawnerGuiListener(this);

        plugin.getServer().getPluginManager().registerEvents(new SpawnerBlockListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new SpawnerNoSpawnListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(guiListener, plugin);

        SpawnerCommand cmd = new SpawnerCommand(this);
        plugin.getCommand("spawner").setExecutor(cmd);
        plugin.getCommand("spawner").setTabCompleter(cmd);

        productionTask = new SpawnerProductionTask(this);
        productionTask.start();
    }

    public void disable() {
        if (productionTask != null) productionTask.stop();
        if (spawnerManager != null) spawnerManager.saveAllSync();
        if (databaseManager != null) databaseManager.close();
    }

    public void reload() {
        loadConfig();
        if (definitionManager != null) definitionManager.load();
    }

    private void loadConfig() {
        File folder = new File(plugin.getDataFolder(), "spawners");
        if (!folder.exists()) folder.mkdirs();

        File file = new File(folder, "config.yml");
        if (!file.exists()) plugin.saveResource("spawners/config.yml", false);
        config = YamlConfiguration.loadConfiguration(file);

        File guiFile = new File(folder, "gui.yml");
        if (!guiFile.exists()) plugin.saveResource("spawners/gui.yml", false);
        guiConfig = YamlConfiguration.loadConfiguration(guiFile);
    }

    public FileConfiguration getConfig() { return config; }
    public FileConfiguration getGuiConfig() { return guiConfig; }
    public JavaPlugin getPlugin() { return plugin; }
    public SpawnerDatabaseManager getDatabaseManager() { return databaseManager; }
    public SpawnerDefinitionManager getSpawnerDefinitionManager() { return definitionManager; }
    public SpawnerManager getSpawnerManager() { return spawnerManager; }
    public SpawnerProductionTask getProductionTask() { return productionTask; }
    public SpawnerGuiListener getGuiListener() { return guiListener; }
}
