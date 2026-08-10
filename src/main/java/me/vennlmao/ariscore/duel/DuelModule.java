package me.vennlmao.ariscore.duel;

import me.vennlmao.ariscore.LicenseManager;
import me.vennlmao.ariscore.duel.commands.*;
import me.vennlmao.ariscore.duel.gui.DuelGuiBuilder;
import me.vennlmao.ariscore.duel.listeners.DuelCommandBlockListener;
import me.vennlmao.ariscore.duel.listeners.DuelDamageListener;
import me.vennlmao.ariscore.duel.listeners.DuelGuiListener;
import me.vennlmao.ariscore.duel.listeners.DuelQuitListener;
import me.vennlmao.ariscore.duel.managers.DuelArenaDatabaseManager;
import me.vennlmao.ariscore.duel.managers.DuelArenaManager;
import me.vennlmao.ariscore.duel.managers.DuelSessionManager;
import me.vennlmao.ariscore.duel.managers.DuelStatsDatabaseManager;
import me.vennlmao.ariscore.duel.managers.DuelStatsManager;
import me.vennlmao.ariscore.duel.utils.MessageUtil;
import me.vennlmao.ariscore.duel.utils.SoundUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class DuelModule {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    private DuelArenaDatabaseManager arenaDatabaseManager;
    private DuelArenaManager arenaManager;
    private DuelStatsDatabaseManager statsDatabaseManager;
    private DuelStatsManager statsManager;
    private DuelSessionManager sessionManager;
    private DuelGuiBuilder guiBuilder;
    private DuelGuiListener guiListener;

    public DuelModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        if (!new LicenseManager(plugin).validate()) {
            plugin.getLogger().severe("[ArisCore] DuelModule disabled: invalid license.");
            return;
        }
        loadConfig();

        arenaDatabaseManager = new DuelArenaDatabaseManager(this);
        arenaDatabaseManager.init();
        arenaManager = new DuelArenaManager(arenaDatabaseManager);

        statsDatabaseManager = new DuelStatsDatabaseManager(this);
        statsDatabaseManager.init();
        statsManager = new DuelStatsManager(statsDatabaseManager);

        sessionManager = new DuelSessionManager(this);
        guiBuilder = new DuelGuiBuilder(this);
        guiListener = new DuelGuiListener(this);

        SoundUtil.init(this);
        MessageUtil.init(this);

        plugin.getServer().getPluginManager().registerEvents(guiListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(new DuelDamageListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DuelQuitListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DuelCommandBlockListener(this), plugin);

        plugin.getCommand("queue").setExecutor(new QueueCommand(this));

        plugin.getCommand("leave").setExecutor(new LeaveCommand(this));

        plugin.getCommand("draw").setExecutor(new DrawCommand(this));

        DuelCommand duelCmd = new DuelCommand(this);
        plugin.getCommand("duel").setExecutor(duelCmd);
        plugin.getCommand("duel").setTabCompleter(duelCmd);

        DuelAdminCommand adminCmd = new DuelAdminCommand(this);
        plugin.getCommand("arisduel").setExecutor(adminCmd);
        plugin.getCommand("arisduel").setTabCompleter(adminCmd);
    }

    public void disable() {
        if (sessionManager != null) sessionManager.cancelAll();
        if (arenaDatabaseManager != null) arenaDatabaseManager.close();
        if (statsDatabaseManager != null) statsDatabaseManager.close();
    }

    public void reload() {
        loadConfig();
        SoundUtil.init(this);
        MessageUtil.init(this);
    }

    private void loadConfig() {
        File folder = new File(plugin.getDataFolder(), "duel");
        if (!folder.exists()) folder.mkdirs();
        File file = new File(folder, "config.yml");
        if (!file.exists()) plugin.saveResource("duel/config.yml", false);
        config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() { return config; }
    public JavaPlugin getPlugin() { return plugin; }
    public DuelArenaManager getArenaManager() { return arenaManager; }
    public DuelStatsManager getStatsManager() { return statsManager; }
    public DuelSessionManager getSessionManager() { return sessionManager; }
    public DuelGuiBuilder getGuiBuilder() { return guiBuilder; }
    public DuelGuiListener getGuiListener() { return guiListener; }
}
