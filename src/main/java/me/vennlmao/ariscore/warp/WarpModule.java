package me.vennlmao.ariscore.warp;

import me.vennlmao.ariscore.warp.commands.*;
import me.vennlmao.ariscore.warp.listeners.WarpDamageListener;
import me.vennlmao.ariscore.warp.listeners.WarpGuiListener;
import me.vennlmao.ariscore.warp.listeners.WarpMoveListener;
import me.vennlmao.ariscore.warp.managers.WarpDatabaseManager;
import me.vennlmao.ariscore.warp.managers.WarpManager;
import me.vennlmao.ariscore.warp.managers.WarpWarmupManager;
import me.vennlmao.ariscore.warp.utils.MessageUtil;
import me.vennlmao.ariscore.warp.utils.SoundUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import me.vennlmao.ariscore.LicenseManager;

import java.io.File;

public class WarpModule {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private WarpDatabaseManager databaseManager;
    private WarpManager warpManager;
    private WarpWarmupManager warmupManager;
    private WarpGuiListener guiListener;

    public WarpModule(JavaPlugin plugin) { this.plugin = plugin; }

    public void enable() {
        if (!new LicenseManager(plugin).validate()) {
            plugin.getLogger().severe("[ArisCore] WarpModule disabled: invalid license.");
            return;
        }
        loadConfig();

        databaseManager = new WarpDatabaseManager(this);
        databaseManager.init();

        warpManager  = new WarpManager(this, databaseManager);
        warmupManager = new WarpWarmupManager(this);
        guiListener  = new WarpGuiListener(this);

        SoundUtil.init(this);
        MessageUtil.init(this);

        plugin.getServer().getPluginManager().registerEvents(guiListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(new WarpMoveListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new WarpDamageListener(this), plugin);

        SetWarpCommand setCmd = new SetWarpCommand(this);
        plugin.getCommand("setwarp").setExecutor(setCmd);

        DelWarpCommand delCmd = new DelWarpCommand(this);
        plugin.getCommand("delwarp").setExecutor(delCmd);
        plugin.getCommand("delwarp").setTabCompleter(delCmd);

        WarpCommand warpCmd = new WarpCommand(this);
        plugin.getCommand("warp").setExecutor(warpCmd);
        plugin.getCommand("warp").setTabCompleter(warpCmd);

        plugin.getCommand("warps").setExecutor(new WarpsCommand(this));
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
        File folder = new File(plugin.getDataFolder(), "warp");
        if (!folder.exists()) folder.mkdirs();
        File file = new File(folder, "config.yml");
        if (!file.exists()) plugin.saveResource("warp/config.yml", false);
        config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig()         { return config; }
    public JavaPlugin getPlugin()                { return plugin; }
    public WarpDatabaseManager getDatabaseManager() { return databaseManager; }
    public WarpManager getWarpManager()          { return warpManager; }
    public WarpWarmupManager getWarmupManager()  { return warmupManager; }
    public WarpGuiListener getGuiListener()      { return guiListener; }
}
