package me.vennlmao.ariscore.tpa;

import me.vennlmao.ariscore.tpa.commands.*;
import me.vennlmao.ariscore.tpa.listeners.DamageListener;
import me.vennlmao.ariscore.tpa.listeners.GuiListener;
import me.vennlmao.ariscore.tpa.listeners.MoveListener;
import me.vennlmao.ariscore.tpa.managers.RequestManager;
import me.vennlmao.ariscore.tpa.managers.TpautoActionbarManager;
import me.vennlmao.ariscore.tpa.managers.WarmupManager;
import me.vennlmao.ariscore.tpa.utils.MessageUtil;
import me.vennlmao.ariscore.tpa.utils.SoundUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class TpaModule {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private RequestManager requestManager;
    private WarmupManager warmupManager;
    private TpautoActionbarManager tpautoActionbarManager;

    public TpaModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        loadConfig();
        requestManager = new RequestManager(this);
        warmupManager = new WarmupManager(this);
        tpautoActionbarManager = new TpautoActionbarManager(this);

        MessageUtil.init(this);
        SoundUtil.init(this);

        TpaCommand tpaCmd = new TpaCommand(this);
        plugin.getCommand("tpa").setExecutor(tpaCmd);
        plugin.getCommand("tpa").setTabCompleter(tpaCmd);

        TpaHereCommand tpaHereCmd = new TpaHereCommand(this);
        plugin.getCommand("tpahere").setExecutor(tpaHereCmd);
        plugin.getCommand("tpahere").setTabCompleter(tpaHereCmd);

        plugin.getCommand("tpaccept").setExecutor(new TpAcceptCommand(this));
        plugin.getCommand("tpacancel").setExecutor(new TpaCancelCommand(this));
        plugin.getCommand("tpauto").setExecutor(new TpautoCommand(this));
        plugin.getCommand("tpatoggle").setExecutor(new TpaToggleCommand(this));
        plugin.getCommand("tpaheretoggle").setExecutor(new TpaHereToggleCommand(this));

        plugin.getServer().getPluginManager().registerEvents(new MoveListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DamageListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new GuiListener(this), plugin);
    }

    public void disable() {
        warmupManager.cancelAll();
        tpautoActionbarManager.cancelAll();
    }

    public void reload() { loadConfig(); }

    private void loadConfig() {
        File folder = new File(plugin.getDataFolder(), "tpa");
        if (!folder.exists()) folder.mkdirs();
        File file = new File(folder, "config.yml");
        if (!file.exists()) plugin.saveResource("tpa/config.yml", false);
        config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() { return config; }
    public JavaPlugin getPlugin() { return plugin; }
    public RequestManager getRequestManager() { return requestManager; }
    public WarmupManager getWarmupManager() { return warmupManager; }
    public TpautoActionbarManager getTpautoActionbarManager() { return tpautoActionbarManager; }
}
