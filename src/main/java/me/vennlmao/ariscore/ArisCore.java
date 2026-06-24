package me.vennlmao.ariscore;

import me.vennlmao.ariscore.auction.AuctionModule;
import me.vennlmao.ariscore.afk.AfkModule;
import me.vennlmao.ariscore.warp.WarpModule;
import me.vennlmao.ariscore.spawn.SpawnModule;
import me.vennlmao.ariscore.sell.SellModule;
import me.vennlmao.ariscore.order.OrderModule;
import me.vennlmao.ariscore.tab.TabModule;
import me.vennlmao.ariscore.commands.ArisCoreReloadCommand;
import me.vennlmao.ariscore.home.HomeModule;
import me.vennlmao.ariscore.shards.ShardsModule;
import me.vennlmao.ariscore.shop.ShopModule;
import me.vennlmao.ariscore.team.TeamModule;
import me.vennlmao.ariscore.tpa.TpaModule;
import org.bukkit.plugin.java.JavaPlugin;

public class ArisCore extends JavaPlugin {

    private static ArisCore instance;
    private TeamModule teamModule;
    private TpaModule tpaModule;
    private HomeModule homeModule;
    private ShopModule shopModule;
    private ShardsModule shardsModule;
    private AuctionModule auctionModule;
    private SellModule sellModule;
    private TabModule tabModule;
    private OrderModule orderModule;
    private SpawnModule spawnModule;
    private AfkModule afkModule;
    private WarpModule warpModule;
    private WarpModule warpModule;

    @Override
    public void onEnable() {
        instance = this;

        shardsModule = new ShardsModule(this);
        shardsModule.enable();

        teamModule = new TeamModule(this);
        teamModule.enable();

        tpaModule = new TpaModule(this);
        tpaModule.enable();

        homeModule = new HomeModule(this);
        homeModule.enable();

        shopModule = new ShopModule(this);
        shopModule.enable();

        auctionModule = new AuctionModule(this);
        auctionModule.enable();

        sellModule = new SellModule(this);
        sellModule.enable();

        tabModule = new TabModule(this);
        tabModule.enable();

        orderModule = new OrderModule(this);
        orderModule.enable();

        spawnModule = new SpawnModule(this);
        spawnModule.enable();

        afkModule = new AfkModule(this);
        afkModule.enable();

        warpModule = new WarpModule(this);
        warpModule.enable();

        ArisCoreReloadCommand reloadCmd = new ArisCoreReloadCommand(this);
        getCommand("ariscore").setExecutor(reloadCmd);
        getCommand("ariscore").setTabCompleter(reloadCmd);

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new ArisCoreExpansion(this).register();
            getLogger().info("[ArisCore] PlaceholderAPI expansion registered: %ariscore_team%, %ariscore_shards%");
        }

        getLogger().info("ArisCore enabled! Modules: TPA, Home, Shop, Shards, Team, Auction, Sell, Tab, Order, Spawn, AFK, Warp");
    }

    @Override
    public void onDisable() {
        if (teamModule != null) teamModule.disable();
        if (tpaModule != null) tpaModule.disable();
        if (homeModule != null) homeModule.disable();
        if (shopModule != null) shopModule.disable();
        if (shardsModule != null) shardsModule.disable();
        if (auctionModule != null) auctionModule.disable();
        if (sellModule != null) sellModule.disable();
        if (tabModule != null) tabModule.disable();
        if (orderModule != null) orderModule.disable();
        if (spawnModule != null) spawnModule.disable();
        if (afkModule != null) afkModule.disable();
        if (warpModule != null) warpModule.disable();
        if (warpModule != null) warpModule.disable();
    }

    public static ArisCore getInstance() { return instance; }
    public TpaModule getTpaModule() { return tpaModule; }
    public HomeModule getHomeModule() { return homeModule; }
    public ShopModule getShopModule() { return shopModule; }
    public ShardsModule getShardsModule() { return shardsModule; }
    public TeamModule getTeamModule() { return teamModule; }
    public AuctionModule getAuctionModule() { return auctionModule; }
    public SellModule getSellModule() { return sellModule; }
    public TabModule getTabModule() { return tabModule; }
    public OrderModule getOrderModule() { return orderModule; }
    public SpawnModule getSpawnModule() { return spawnModule; }
    public AfkModule getAfkModule() { return afkModule; }
    public WarpModule getWarpModule() { return warpModule; }
    public WarpModule getWarpModule() { return warpModule; }
}
