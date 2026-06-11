package me.vennlmao.ariscore.auction;

import me.vennlmao.ariscore.auction.commands.AuctionCommand;
import me.vennlmao.ariscore.auction.listeners.AuctionChatListener;
import me.vennlmao.ariscore.auction.listeners.AuctionSignListener;
import me.vennlmao.ariscore.auction.utils.SignEditorUtil;
import me.vennlmao.ariscore.auction.listeners.AuctionGuiListener;
import me.vennlmao.ariscore.auction.managers.AuctionManager;
import me.vennlmao.ariscore.auction.utils.MessageUtil;
import me.vennlmao.ariscore.auction.utils.SoundUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuctionModule {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private Economy economy;
    private AuctionManager auctionManager;
    private AuctionGuiListener guiListener;
    private final Map<UUID, String> searching = new HashMap<>();
    private final Map<UUID, java.util.function.Consumer<String[]>> pendingSign = new HashMap<>();

    public AuctionModule(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        loadConfig();
        setupEconomy();
        auctionManager = new AuctionManager(this);
        guiListener = new AuctionGuiListener(this);
        MessageUtil.init(this);
        SoundUtil.init(this);
        SignEditorUtil.init(this);
        plugin.getServer().getPluginManager().registerEvents(guiListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(new AuctionChatListener(this), plugin);
        com.github.retrooper.packetevents.PacketEvents.getAPI().getEventManager().registerListener(new AuctionSignListener(this));
        AuctionCommand cmd = new AuctionCommand(this);
        plugin.getCommand("ah").setExecutor(cmd);
        plugin.getCommand("auction").setExecutor(cmd);
    }

    public void disable() {}

    public void reload() { loadConfig(); }

    private void loadConfig() {
        File folder = new File(plugin.getDataFolder(), "auction");
        if (!folder.exists()) folder.mkdirs();
        File file = new File(folder, "config.yml");
        if (!file.exists()) plugin.saveResource("auction/config.yml", false);
        config = YamlConfiguration.loadConfiguration(file);
    }

    private void setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) return;
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) economy = rsp.getProvider();
    }

    public FileConfiguration getConfig() { return config; }
    public JavaPlugin getPlugin() { return plugin; }
    public Economy getEconomy() { return economy; }
    public AuctionManager getAuctionManager() { return auctionManager; }
    public AuctionGuiListener getGuiListener() { return guiListener; }
    public Map<UUID, String> getSearching() { return searching; }
    public Map<UUID, java.util.function.Consumer<String[]>> getPendingSign() { return pendingSign; }
}
