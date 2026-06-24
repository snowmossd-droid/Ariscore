package me.vennlmao.ariscore.auction;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.auction.commands.AuctionCommand;
import me.vennlmao.ariscore.auction.commands.FastBuyToggleCommand;
import me.vennlmao.ariscore.auction.commands.FastSellToggleCommand;
import me.vennlmao.ariscore.auction.gui.AuctionGUI;
import me.vennlmao.ariscore.auction.gui.ConfirmListingGUI;
import me.vennlmao.ariscore.auction.gui.ConfirmPurchaseGUI;
import me.vennlmao.ariscore.auction.gui.MyAuctionsGUI;
import me.vennlmao.ariscore.auction.gui.ShulkerViewGUI;
import me.vennlmao.ariscore.auction.gui.TransactionsGUI;
import me.vennlmao.ariscore.auction.listeners.AuctionJoinListener;
import me.vennlmao.ariscore.auction.managers.AuctionConfigManager;
import me.vennlmao.ariscore.auction.managers.AuctionManager;
import me.vennlmao.ariscore.auction.managers.ChatSignManager;
import me.vennlmao.ariscore.auction.managers.GUIManager;
import me.vennlmao.ariscore.auction.managers.LangManager;
import me.vennlmao.ariscore.auction.managers.PlayerDataManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;

public class AuctionModule {

    private final ArisCore plugin;
    private Economy economy;
    private AuctionConfigManager configManager;
    private GUIManager guiManager;
    private LangManager langManager;
    private AuctionManager auctionManager;
    private AuctionGUI auctionGUI;
    private MyAuctionsGUI myAuctionsGUI;
    private ConfirmPurchaseGUI confirmPurchaseGUI;
    private ConfirmListingGUI confirmListingGUI;
    private TransactionsGUI transactionsGUI;
    private ShulkerViewGUI shulkerViewGUI;
    private ChatSignManager chatSignManager;
    private PlayerDataManager playerDataManager;

    public AuctionModule(ArisCore plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        configManager = new AuctionConfigManager(plugin);
        configManager.setup();

        langManager = new LangManager(plugin);
        guiManager = new GUIManager(plugin);
        playerDataManager = new PlayerDataManager();
        chatSignManager = new ChatSignManager(plugin);
        chatSignManager.startCleanupTask();

        if (!setupEconomy()) {
            plugin.getLogger().severe("[Auction] Vault not found! Auction module disabled.");
            return;
        }

        auctionManager = new AuctionManager(plugin);
        auctionGUI = new AuctionGUI(plugin);
        myAuctionsGUI = new MyAuctionsGUI(plugin);
        confirmPurchaseGUI = new ConfirmPurchaseGUI(plugin);
        confirmListingGUI = new ConfirmListingGUI(plugin);
        transactionsGUI = new TransactionsGUI(plugin);
        shulkerViewGUI = new ShulkerViewGUI(plugin);

        AuctionCommand auctionCommand = new AuctionCommand(plugin);
        plugin.getCommand("ah").setExecutor(auctionCommand);
        plugin.getCommand("ah").setTabCompleter(auctionCommand);
        plugin.getCommand("auction").setExecutor(auctionCommand);
        plugin.getCommand("auction").setTabCompleter(auctionCommand);
        plugin.getCommand("ahfastbuytoggle").setExecutor(new FastBuyToggleCommand(plugin));
        plugin.getCommand("ahfastselltoggle").setExecutor(new FastSellToggleCommand(plugin));

        plugin.getServer().getPluginManager().registerEvents(auctionGUI, plugin);
        plugin.getServer().getPluginManager().registerEvents(myAuctionsGUI, plugin);
        plugin.getServer().getPluginManager().registerEvents(confirmPurchaseGUI, plugin);
        plugin.getServer().getPluginManager().registerEvents(confirmListingGUI, plugin);
        plugin.getServer().getPluginManager().registerEvents(transactionsGUI, plugin);
        plugin.getServer().getPluginManager().registerEvents(shulkerViewGUI, plugin);
        plugin.getServer().getPluginManager().registerEvents(chatSignManager, plugin);
        plugin.getServer().getPluginManager().registerEvents(new AuctionJoinListener(plugin), plugin);

        plugin.getLogger().info("[Auction] Module enabled.");
    }

    public void disable() {
        if (auctionManager != null) auctionManager.saveAllAuctions();
        if (auctionManager != null) auctionManager.getDataManager().closeConnection();
        if (playerDataManager != null) playerDataManager.closeConnection();
        plugin.getLogger().info("[Auction] Module disabled.");
    }

    public void reload() {
        configManager.setup();
        guiManager.reload();
        langManager.reload();
        if (auctionManager != null) auctionManager.saveAllAuctions();
    }

    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public ArisCore getPlugin() { return plugin; }
    public Economy getEconomy() { return economy; }
    public AuctionConfigManager getConfigManager() { return configManager; }
    public GUIManager getGuiManager() { return guiManager; }
    public LangManager getLangManager() { return langManager; }
    public AuctionManager getAuctionManager() { return auctionManager; }
    public AuctionGUI getAuctionGUI() { return auctionGUI; }
    public MyAuctionsGUI getMyAuctionsGUI() { return myAuctionsGUI; }
    public ConfirmPurchaseGUI getConfirmPurchaseGUI() { return confirmPurchaseGUI; }
    public ConfirmListingGUI getConfirmListingGUI() { return confirmListingGUI; }
    public TransactionsGUI getTransactionsGUI() { return transactionsGUI; }
    public ShulkerViewGUI getShulkerViewGUI() { return shulkerViewGUI; }
    public ChatSignManager getChatSignManager() { return chatSignManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
}
