package me.vennlmao.ariscore;

import me.vennlmao.ariscore.commands.ArisCoreReloadCommand;
import me.vennlmao.ariscore.home.HomeModule;
import me.vennlmao.ariscore.shards.ShardsModule;
import me.vennlmao.ariscore.shop.ShopModule;
import me.vennlmao.ariscore.auction.AuctionModule;
import me.vennlmao.ariscore.tab.TabModule;
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
    private TabModule tabModule;
    private AuctionModule auctionModule;

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

        tabModule = new TabModule(this);
        tabModule.enable();

        auctionModule = new AuctionModule(this);
        auctionModule.enable();

        ArisCoreReloadCommand reloadCmd = new ArisCoreReloadCommand(this);
        getCommand("ariscore").setExecutor(reloadCmd);
        getCommand("ariscore").setTabCompleter(reloadCmd);

        getLogger().info("ArisCore enabled! Modules: TPA, Home, Shop, Shards, Team, Tab, Auction");
    }

    @Override
    public void onDisable() {
        if (teamModule != null) teamModule.disable();
        if (tpaModule != null) tpaModule.disable();
        if (homeModule != null) homeModule.disable();
        if (shopModule != null) shopModule.disable();
        if (shardsModule != null) shardsModule.disable();
        if (tabModule != null) tabModule.disable();
        if (auctionModule != null) auctionModule.disable();
    }

    public static ArisCore getInstance() { return instance; }
    public TpaModule getTpaModule() { return tpaModule; }
    public HomeModule getHomeModule() { return homeModule; }
    public ShopModule getShopModule() { return shopModule; }
    public ShardsModule getShardsModule() { return shardsModule; }
    public TeamModule getTeamModule() { return teamModule; }
    public TabModule getTabModule() { return tabModule; }
    public AuctionModule getAuctionModule() { return auctionModule; }
}
