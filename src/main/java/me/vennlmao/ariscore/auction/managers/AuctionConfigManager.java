package me.vennlmao.ariscore.auction.managers;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.auction.utils.ColorUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class AuctionConfigManager {

    private final ArisCore plugin;
    private FileConfiguration config;
    private File configFile;

    public AuctionConfigManager(ArisCore plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        File folder = new File(plugin.getDataFolder(), "auction");
        if (!folder.exists()) folder.mkdirs();

        configFile = new File(folder, "config.yml");
        if (!configFile.exists()) saveDefault("auction/config.yml", configFile);

        config = YamlConfiguration.loadConfiguration(configFile);

        InputStream defStream = plugin.getResource("auction/config.yml");
        if (defStream != null) {
            FileConfiguration def = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream));
            config.setDefaults(def);
        }
    }

    private void saveDefault(String resourcePath, File target) {
        try {
            InputStream in = plugin.getResource(resourcePath);
            if (in != null) Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getConfig() { return config; }

    public int getAuctionDurationHours() { return config.getInt("auction.duration-hours"); }
    public double getFeePercentage() { return config.getDouble("auction.fee-percentage"); }
    public double getMinimumPrice() { return config.getDouble("auction.minimum-price"); }
    public double getMaximumPrice() { return config.getDouble("auction.maximum-price"); }
    public int getExpireCheckInterval() { return config.getInt("auction.expire-check-interval"); }
    public String getCurrencyFormat() { return config.getString("economy.currency-format"); }
    public boolean isAbbreviationsEnabled() { return config.getBoolean("economy.abbreviations.enabled"); }
    public List<String> getAbbreviationFormats() { return config.getStringList("economy.abbreviations.formats"); }
    public List<String> getBlacklistItems() { return config.getStringList("blacklist-items"); }
    public boolean useSign() { return config.getBoolean("chat-input.use-sign"); }
    public boolean useChat() { return config.getBoolean("chat-input.use-chat"); }
    public List<String> getSignLines() { return config.getStringList("sign-lines"); }

    public boolean isBlacklisted(Material material) {
        if (material == null) return false;
        for (String s : getBlacklistItems()) {
            if (s.equalsIgnoreCase(material.name())) return true;
        }
        return false;
    }

    public void playSound(Player player, String key) {
        if (player == null || !player.isOnline()) return;
        String soundStr = config.getString("sounds." + key, "");
        if (soundStr == null || soundStr.isEmpty()) return;
        try {
            Sound sound = Sound.valueOf(soundStr.toUpperCase().replace(".", "_").replace("-", "_"));
            player.playSound(player.getLocation(), sound, 1f, 1f);
        } catch (IllegalArgumentException ignored) {}
    }
}
