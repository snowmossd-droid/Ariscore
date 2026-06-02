package me.vennlmao.ariscore.team;

import me.vennlmao.ariscore.team.commands.TeamCommand;
import me.vennlmao.ariscore.team.gui.TeamGuiBuilder;
import me.vennlmao.ariscore.team.listeners.TeamChatListener;
import me.vennlmao.ariscore.team.listeners.TeamDamageListener;
import me.vennlmao.ariscore.team.listeners.TeamGuiListener;
import me.vennlmao.ariscore.team.listeners.TeamMoveListener;
import me.vennlmao.ariscore.team.managers.TeamDatabaseManager;
import me.vennlmao.ariscore.team.managers.TeamData;
import me.vennlmao.ariscore.team.managers.TeamManager;
import me.vennlmao.ariscore.team.managers.TeamPlaceholder;
import me.vennlmao.ariscore.team.managers.TeamWarmupManager;
import me.vennlmao.ariscore.team.utils.MessageUtil;
import me.vennlmao.ariscore.team.utils.SoundUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class TeamModule {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private TeamDatabaseManager databaseManager;
    private TeamManager teamManager;
    private TeamWarmupManager warmupManager;
    private TeamGuiBuilder guiBuilder;
    private TeamChatListener chatListener;
    private TeamGuiListener guiListener;
    private TeamData.SortType defaultSort = TeamData.SortType.JOIN_DATE;

    public TeamModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        loadConfig();

        databaseManager = new TeamDatabaseManager(this);
        databaseManager.init();

        teamManager = new TeamManager(this, databaseManager);
        warmupManager = new TeamWarmupManager(this);
        guiBuilder = new TeamGuiBuilder(this);
        chatListener = new TeamChatListener(this);
        guiListener = new TeamGuiListener(this);

        SoundUtil.init(this);
        MessageUtil.init(this);

        plugin.getServer().getPluginManager().registerEvents(chatListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(guiListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(new TeamMoveListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new TeamDamageListener(this), plugin);

        TeamCommand teamCmd = new TeamCommand(this);
        plugin.getCommand("team").setExecutor(teamCmd);
        plugin.getCommand("team").setTabCompleter(teamCmd);

        if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new TeamPlaceholder(this).register();
            plugin.getLogger().info("[Team] PlaceholderAPI registered: %ariscore_team%");
        }
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
        File folder = new File(plugin.getDataFolder(), "team");
        if (!folder.exists()) folder.mkdirs();
        File file = new File(folder, "config.yml");
        if (!file.exists()) plugin.saveResource("team/config.yml", false);
        config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() { return config; }
    public JavaPlugin getPlugin() { return plugin; }
    public TeamDatabaseManager getDatabaseManager() { return databaseManager; }
    public TeamManager getTeamManager() { return teamManager; }
    public TeamWarmupManager getWarmupManager() { return warmupManager; }
    public TeamGuiBuilder getGuiBuilder() { return guiBuilder; }
    public TeamChatListener getChatListener() { return chatListener; }
    public TeamGuiListener getGuiListener() { return guiListener; }
}
