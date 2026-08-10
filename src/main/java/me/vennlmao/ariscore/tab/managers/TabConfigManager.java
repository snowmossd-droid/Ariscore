package me.vennlmao.ariscore.tab.managers;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.tab.models.ScoreboardProfile;
import me.vennlmao.ariscore.tab.models.TabProfile;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class TabConfigManager {

    private final ArisCore plugin;

    private boolean tabEnabled;
    private long tabUpdateTicks;
    private List<TabProfile> tabProfiles = new ArrayList<>();

    private boolean scoreboardEnabled;
    private long scoreboardUpdateTicks;
    private List<ScoreboardProfile> scoreboardProfiles = new ArrayList<>();

    private boolean nametagEnabled;
    private String nametagTag;
    private long nametagUpdateTicks;

    private boolean belownameEnabled;
    private String belownameText;
    private String belownameValuePlaceholder;
    private long belownameUpdateTicks;

    public TabConfigManager(ArisCore plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File folder = new File(plugin.getDataFolder(), "tab");
        folder.mkdirs();

        saveDefault("tab/config.yml",      new File(folder, "config.yml"));
        saveDefault("tab/tab.yml",         new File(folder, "tab.yml"));
        saveDefault("tab/scoreboard.yml",  new File(folder, "scoreboard.yml"));

        FileConfiguration mainCfg  = loadYml(new File(folder, "config.yml"),     "tab/config.yml");
        FileConfiguration tabCfg   = loadYml(new File(folder, "tab.yml"),        "tab/tab.yml");
        FileConfiguration sbCfg    = loadYml(new File(folder, "scoreboard.yml"), "tab/scoreboard.yml");

        tabUpdateTicks        = mainCfg.getLong("tab.update-interval-ticks", 20L);
        scoreboardUpdateTicks = mainCfg.getLong("scoreboard.update-interval-ticks", 20L);

        tabEnabled    = tabCfg.getBoolean("enabled", true);
        tabProfiles   = parseTabProfiles(tabCfg);

        nametagEnabled      = tabCfg.getBoolean("nametag.enabled", true);
        nametagTag          = tabCfg.getString("nametag.tag", "%luckperms_prefix%");
        nametagUpdateTicks  = tabCfg.getLong("nametag.update-interval-ticks", 40L);

        belownameEnabled            = tabCfg.getBoolean("belowname-objective.enabled", false);
        belownameText               = tabCfg.getString("belowname-objective.text", "");
        belownameValuePlaceholder   = tabCfg.getString("belowname-objective.value-placeholder", "");
        belownameUpdateTicks        = tabCfg.getLong("belowname-objective.update-interval-ticks", 20L);

        scoreboardEnabled  = sbCfg.getBoolean("enabled", true);
        scoreboardProfiles = parseScoreboardProfiles(sbCfg);
    }

    private void saveDefault(String resource, File target) {
        if (target.exists()) return;
        try (InputStream in = plugin.getResource(resource)) {
            if (in != null) Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private FileConfiguration loadYml(File file, String resource) {
        YamlConfiguration cfg = new YamlConfiguration();
        if (file.exists()) {
            try {
                List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                StringBuilder sb = new StringBuilder(lines.size() * 40);
                for (String line : lines) {
                    if (!line.stripLeading().startsWith("```")) sb.append(line).append('\n');
                }
                cfg.loadFromString(sb.toString());
            } catch (Exception e) {
                plugin.getLogger().warning("[Tab] Failed to parse " + file.getName() + ": " + e.getMessage());
            }
        }
        try (InputStream in = plugin.getResource(resource)) {
            if (in != null) cfg.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
        } catch (Exception ignored) {}
        return cfg;
    }

    private List<TabProfile> parseTabProfiles(FileConfiguration cfg) {
        List<TabProfile> list = new ArrayList<>();
        ConfigurationSection section = cfg.getConfigurationSection("tabs");
        if (section == null) return list;
        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) continue;
            list.add(new TabProfile(key,
                s.getString("display-condition"),
                s.getString("world"),
                s.getStringList("header"),
                s.getStringList("footer"),
                s.getString("tablist-name-format")));
        }
        list.sort((a, b) -> Boolean.compare(isEmpty(a.getWorld()), isEmpty(b.getWorld())));
        return list;
    }

    private List<ScoreboardProfile> parseScoreboardProfiles(FileConfiguration cfg) {
        List<ScoreboardProfile> list = new ArrayList<>();
        ConfigurationSection section = cfg.getConfigurationSection("scoreboards");
        if (section == null) return list;
        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) continue;
            list.add(new ScoreboardProfile(key,
                s.getString("display-condition"),
                s.getString("world"),
                s.getString("title", ""),
                s.getStringList("lines")));
        }
        list.sort((a, b) -> Boolean.compare(isEmpty(a.getWorld()), isEmpty(b.getWorld())));
        return list;
    }

    private static boolean isEmpty(String s) { return s == null || s.trim().isEmpty(); }

    public boolean isTabEnabled()              { return tabEnabled; }
    public long getTabUpdateTicks()            { return tabUpdateTicks; }
    public List<TabProfile> getTabProfiles()   { return tabProfiles; }

    public boolean isScoreboardEnabled()               { return scoreboardEnabled; }
    public long getScoreboardUpdateTicks()             { return scoreboardUpdateTicks; }
    public List<ScoreboardProfile> getScoreboardProfiles() { return scoreboardProfiles; }

    public boolean isNametagEnabled()   { return nametagEnabled; }
    public String getNametagTag()       { return nametagTag; }
    public long getNametagUpdateTicks() { return nametagUpdateTicks; }

    public boolean isBelownameEnabled()          { return belownameEnabled; }
    public String getBelownameText()             { return belownameText; }
    public String getBelownameValuePlaceholder() { return belownameValuePlaceholder; }
    public long getBelownameUpdateTicks()        { return belownameUpdateTicks; }
}