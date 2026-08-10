package me.vennlmao.ariscore.tab;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.tab.commands.TabCommand;
import me.vennlmao.ariscore.tab.managers.BelownameManager;
import me.vennlmao.ariscore.tab.managers.ConditionEvaluator;
import me.vennlmao.ariscore.tab.managers.NameTagManager;
import me.vennlmao.ariscore.tab.managers.PapiManager;
import me.vennlmao.ariscore.tab.managers.ScoreboardManager;
import me.vennlmao.ariscore.tab.managers.TabConfigManager;
import me.vennlmao.ariscore.tab.managers.TabListManager;
import org.bukkit.scoreboard.Scoreboard;
import me.vennlmao.ariscore.LicenseManager;

public class TabModule {

    private final ArisCore plugin;
    private TabConfigManager configManager;
    private PapiManager papiManager;
    private ConditionEvaluator conditionEvaluator;
    private TabListManager tabListManager;
    private ScoreboardManager scoreboardManager;
    private NameTagManager nameTagManager;
    private BelownameManager belownameManager;
    private Scoreboard sharedScoreboard;

    public TabModule(ArisCore plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        if (!new LicenseManager(plugin).validate()) {
            plugin.getLogger().severe("[ArisCore] TabModule disabled: invalid license.");
            return;
        }
        sharedScoreboard   = null;
        configManager      = new TabConfigManager(plugin);
        configManager.load();

        papiManager        = new PapiManager();
        conditionEvaluator = new ConditionEvaluator(papiManager);
        tabListManager     = new TabListManager(plugin, papiManager, conditionEvaluator, configManager);
        scoreboardManager  = new ScoreboardManager(plugin, papiManager, conditionEvaluator, configManager);
        nameTagManager     = new NameTagManager(plugin, papiManager, configManager, sharedScoreboard);
        belownameManager   = new BelownameManager(plugin, papiManager, configManager);

        plugin.getServer().getPluginManager().registerEvents(tabListManager,    plugin);
        plugin.getServer().getPluginManager().registerEvents(scoreboardManager, plugin);
        plugin.getServer().getPluginManager().registerEvents(nameTagManager,    plugin);
        plugin.getServer().getPluginManager().registerEvents(belownameManager,  plugin);

        tabListManager.start();
        scoreboardManager.start();
        nameTagManager.start();
        belownameManager.start();

        TabCommand cmd = new TabCommand(this);
        if (plugin.getCommand("tab") != null) {
            plugin.getCommand("tab").setExecutor(cmd);
            plugin.getCommand("tab").setTabCompleter(cmd);
        }

        plugin.getLogger().info("[Tab] Module enabled.");
    }

    public void disable() {
        if (tabListManager    != null) tabListManager.stop();
        if (scoreboardManager != null) scoreboardManager.stop();
        if (nameTagManager    != null) nameTagManager.stop();
        if (belownameManager  != null) belownameManager.stop();
        plugin.getLogger().info("[Tab] Module disabled.");
    }

    public void reload() {
        configManager.load();
        tabListManager.reload();
        scoreboardManager.reload();
        nameTagManager.reload();
        belownameManager.reload();
    }

    public TabConfigManager  getConfigManager()      { return configManager; }
    public PapiManager       getPapiManager()        { return papiManager; }
    public TabListManager    getTabListManager()     { return tabListManager; }
    public ScoreboardManager getScoreboardManager()  { return scoreboardManager; }
    public NameTagManager    getNameTagManager()     { return nameTagManager; }
    public BelownameManager  getBelownameManager()   { return belownameManager; }
    public Scoreboard        getSharedScoreboard()   { return sharedScoreboard; }
}
