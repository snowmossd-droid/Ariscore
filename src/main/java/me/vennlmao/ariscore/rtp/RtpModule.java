package me.vennlmao.ariscore.rtp;

import me.vennlmao.ariscore.rtp.commands.RtpCommand;
import me.vennlmao.ariscore.rtp.listeners.DamageListener;
import me.vennlmao.ariscore.rtp.listeners.GuiListener;
import me.vennlmao.ariscore.rtp.listeners.MoveListener;
import me.vennlmao.ariscore.rtp.managers.CooldownManager;
import me.vennlmao.ariscore.rtp.managers.WarmupManager;
import me.vennlmao.ariscore.rtp.utils.MessageUtil;
import me.vennlmao.ariscore.rtp.utils.SoundUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import me.vennlmao.ariscore.LicenseManager;

import java.io.File;

public class RtpModule {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private WarmupManager warmupManager;
    private CooldownManager cooldownManager;

    public RtpModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        if (!new LicenseManager(plugin).validate()) {
            plugin.getLogger().severe("[ArisCore] RtpModule disabled: invalid license.");
            return;
        }
        loadConfig();

        warmupManager = new WarmupManager(this);
        cooldownManager = new CooldownManager(this);

        MessageUtil.init(this);
        SoundUtil.init(this);

        RtpCommand rtpCmd = new RtpCommand(this);
        plugin.getCommand("rtp").setExecutor(rtpCmd);
        plugin.getCommand("randomtp").setExecutor(rtpCmd);

        plugin.getServer().getPluginManager().registerEvents(new GuiListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new MoveListener(this), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DamageListener(this), plugin);
    }

    public void disable() {
        warmupManager.cancelAll();
        cooldownManager.clear();
    }

    public void reload() {
        loadConfig();
    }

    private void loadConfig() {
        File folder = new File(plugin.getDataFolder(), "rtp");
        if (!folder.exists()) folder.mkdirs();
        File file = new File(folder, "config.yml");
        if (!file.exists()) plugin.saveResource("rtp/config.yml", false);
        config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() { return config; }
    public JavaPlugin getPlugin() { return plugin; }
    public WarmupManager getWarmupManager() { return warmupManager; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
}
