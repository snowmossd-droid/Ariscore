package me.vennlmao.ariscore.afk;

import me.vennlmao.ariscore.afk.commands.AfkCommand;
import me.vennlmao.ariscore.afk.commands.AfksCommand;
import me.vennlmao.ariscore.afk.commands.SetAfkCommand;
import me.vennlmao.ariscore.afk.listeners.AfkDamageListener;
import me.vennlmao.ariscore.afk.listeners.AfkGuiListener;
import me.vennlmao.ariscore.afk.listeners.AfkMoveListener;
import me.vennlmao.ariscore.afk.managers.AfkDatabaseManager;
import me.vennlmao.ariscore.afk.managers.AfkManager;
import me.vennlmao.ariscore.afk.managers.AfkWarmupManager;
import me.vennlmao.ariscore.afk.utils.MessageUtil;
import me.vennlmao.ariscore.afk.utils.SoundUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class AfkModule {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private AfkDatabaseManager databaseManager;
    private AfkManager afkManager;
    private AfkWarmupManager warmupManager;
    private AfkGuiListener guiListener;

    public AfkModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        loadConfig();

        databaseManager = new AfkDatabaseManager(this);
        databaseManager.init();

        afkManager = new AfkManager(this, databaseManager);
        warmupManager = new AfkWarmupManager(this);
        guiListener = new AfkGuiListener(this);

        SoundUtil.init(this);
        MessageUtil.init(this);

        plugin.getServer().getPluginManager().registerEvents(guiListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(new AfkMoveListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new AfkDamageListener(this), plugin);

        plugin.getCommand("setafk").setExecutor(new SetAfkCommand(this));

        AfkCommand afkCmd = new AfkCommand(this);
        plugin.getCommand("afk").setExecutor(afkCmd);
        plugin.getCommand("afk").setTabCompleter(afkCmd);

        plugin.getCommand("afks").setExecutor(new AfksCommand(this));
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
        File folder = new File(plugin.getDataFolder(), "afk");
        if (!folder.exists()) folder.mkdirs();
        File file = new File(folder, "config.yml");
        if (!file.exists()) plugin.saveResource("afk/config.yml", false);
        config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() { return config; }
    public JavaPlugin getPlugin() { return plugin; }
    public AfkDatabaseManager getDatabaseManager() { return databaseManager; }
    public AfkManager getAfkManager() { return afkManager; }
    public AfkWarmupManager getWarmupManager() { return warmupManager; }
    public AfkGuiListener getGuiListener() { return guiListener; }
}
