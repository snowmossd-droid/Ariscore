package me.vennlmao.ariscore;

import me.vennlmao.ariscore.auction.AuctionModule;
import me.vennlmao.ariscore.amethyst.AmethystModule;
import me.vennlmao.ariscore.crates.CratesModule;
import me.vennlmao.ariscore.duel.DuelModule;
import me.vennlmao.ariscore.afk.AfkModule;
import me.vennlmao.ariscore.warp.WarpModule;
import me.vennlmao.ariscore.spawn.SpawnModule;
import me.vennlmao.ariscore.spawners.SpawnersModule;
import me.vennlmao.ariscore.sell.SellModule;
import me.vennlmao.ariscore.order.OrderModule;
import me.vennlmao.ariscore.tab.TabModule;
import me.vennlmao.ariscore.commands.ArisCoreReloadCommand;
import me.vennlmao.ariscore.home.HomeModule;
import me.vennlmao.ariscore.shards.ShardsModule;
import me.vennlmao.ariscore.shop.ShopModule;
import me.vennlmao.ariscore.team.TeamModule;
import me.vennlmao.ariscore.rtp.RtpModule;
import me.vennlmao.ariscore.tpa.TpaModule;
import org.bukkit.plugin.java.JavaPlugin;

public class ArisCore extends JavaPlugin {

    private static ArisCore instance;
    private TeamModule teamModule;
    private TpaModule tpaModule;
    private RtpModule rtpModule;
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
    private CratesModule cratesModule;
    private AmethystModule amethystModule;
    private DuelModule duelModule;
    private SpawnersModule spawnersModule;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        if (!new LicenseManager(this).validate()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        shardsModule = new ShardsModule(this);
        if (isModuleEnabled("shards")) shardsModule.enable();

        teamModule = new TeamModule(this);
        if (isModuleEnabled("team")) teamModule.enable();

        tpaModule = new TpaModule(this);
        if (isModuleEnabled("tpa")) tpaModule.enable();

        rtpModule = new RtpModule(this);
        if (isModuleEnabled("rtp")) rtpModule.enable();

        homeModule = new HomeModule(this);
        if (isModuleEnabled("home")) homeModule.enable();

        shopModule = new ShopModule(this);
        if (isModuleEnabled("shop")) shopModule.enable();

        auctionModule = new AuctionModule(this);
        if (isModuleEnabled("auction")) auctionModule.enable();

        sellModule = new SellModule(this);
        if (isModuleEnabled("sell")) sellModule.enable();

        spawnersModule = new SpawnersModule(this);
        if (isModuleEnabled("spawners")) spawnersModule.enable();

        tabModule = new TabModule(this);
        if (isModuleEnabled("tab")) tabModule.enable();

        orderModule = new OrderModule(this);
        if (isModuleEnabled("order")) orderModule.enable();

        spawnModule = new SpawnModule(this);
        if (isModuleEnabled("spawn")) spawnModule.enable();

        afkModule = new AfkModule(this);
        if (isModuleEnabled("afk")) afkModule.enable();

        warpModule = new WarpModule(this);
        if (isModuleEnabled("warp")) warpModule.enable();

        cratesModule = new CratesModule(this);
        if (isModuleEnabled("crates")) cratesModule.enable();

        amethystModule = new AmethystModule(this);
        if (isModuleEnabled("amethyst")) amethystModule.enable();

        duelModule = new DuelModule(this);
        if (isModuleEnabled("duel")) duelModule.enable();

        ArisCoreReloadCommand reloadCmd = new ArisCoreReloadCommand(this);
        getCommand("ariscore").setExecutor(reloadCmd);
        getCommand("ariscore").setTabCompleter(reloadCmd);

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new ArisCoreExpansion(this).register();
            getLogger().info("[ArisCore] PlaceholderAPI expansion registered: %ariscore_team%, %ariscore_shards%");
        }

        getLogger().info("ArisCore enabled! Modules: TPA, Home, Shop, Shards, Team, Auction, Sell, Tab, Order, Spawn, AFK, Warp, Crates, Amethyst, Duel, Spawners");
    }

    @Override
    public void onDisable() {
        if (teamModule != null) teamModule.disable();
        if (tpaModule != null) tpaModule.disable();
        if (rtpModule != null) rtpModule.disable();
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
        if (cratesModule != null) cratesModule.disable();
        if (amethystModule != null) amethystModule.disable();
        if (duelModule != null) duelModule.disable();
        if (spawnersModule != null) spawnersModule.disable();
    }

    public boolean isModuleEnabled(String name) {
        return getConfig().getBoolean("modules." + name, true);
    }

    public static ArisCore getInstance() { return instance; }
    public TpaModule getTpaModule() { return tpaModule; }
    public RtpModule getRtpModule() { return rtpModule; }
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
    public CratesModule getCratesModule() { return cratesModule; }
    public AmethystModule getAmethystModule() { return amethystModule; }
    public DuelModule getDuelModule() { return duelModule; }
    public SpawnersModule getSpawnersModule() { return spawnersModule; }
}
