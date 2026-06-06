package me.vennlmao.ariscore.tab;

import me.vennlmao.ariscore.tab.commands.ScoreboardToggleCommand;
import me.vennlmao.ariscore.tab.listeners.TabListener;
import me.vennlmao.ariscore.tab.managers.ScoreboardManager;
import me.vennlmao.ariscore.tab.managers.TabManager;
import me.vennlmao.ariscore.tab.utils.SoundUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class TabModule {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private ScoreboardManager scoreboardManager;
    private TabManager tabManager;

    public TabModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        loadConfig();
        scoreboardManager = new ScoreboardManager(this);
        tabManager = new TabManager(this);
        plugin.getServer().getPluginManager().registerEvents(new TabListener(this), plugin);
        SoundUtil.init(this);
        ScoreboardToggleCommand cmd = new ScoreboardToggleCommand(this);
        plugin.getCommand("scoreboardtoggle").setExecutor(cmd);
        plugin.getCommand("scoreboardtoggle").setTabCompleter(cmd);
        plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, t ->
                plugin.getServer().getOnlinePlayers().forEach(p -> {
                    scoreboardManager.startFor(p);
                    tabManager.setupFor(p);
                }), 2L);
    }

    public void disable() {
        if (scoreboardManager != null) scoreboardManager.removeAll();
        if (tabManager != null) tabManager.removeAll();
    }

    public void reload() {
        loadConfig();
        if (scoreboardManager != null) scoreboardManager.reloadAll();
        if (tabManager != null) tabManager.reloadAll();
    }

    private void loadConfig() {
        File folder = new File(plugin.getDataFolder(), "tab");
        if (!folder.exists()) folder.mkdirs();
        File file = new File(folder, "config.yml");
        if (!file.exists()) plugin.saveResource("tab/config.yml", false);
        config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() { return config; }
    public JavaPlugin getPlugin() { return plugin; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public TabManager getTabManager() { return tabManager; }
}
