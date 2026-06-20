package me.vennlmao.ariscore.auction.managers;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.auction.utils.ColorUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class LangManager {

    private final ArisCore plugin;
    private FileConfiguration lang;
    private File langFile;

    public LangManager(ArisCore plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        File folder = new File(plugin.getDataFolder(), "auction");
        if (!folder.exists()) folder.mkdirs();

        langFile = new File(folder, "lang.yml");
        if (!langFile.exists()) saveDefault("auction/lang.yml", langFile);

        lang = YamlConfiguration.loadConfiguration(langFile);

        InputStream defStream = plugin.getResource("auction/lang.yml");
        if (defStream != null) {
            FileConfiguration def = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream));
            lang.setDefaults(def);
        }
    }

    public void reload() {
        lang = YamlConfiguration.loadConfiguration(langFile);
    }

    private void saveDefault(String resourcePath, File target) {
        try {
            InputStream in = plugin.getResource(resourcePath);
            if (in != null) Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getPrefix() {
        if (!lang.getBoolean("prefix-enable")) return "";
        return ColorUtil.colorize(lang.getString("prefix", ""));
    }

    public String get(String path) {
        return ColorUtil.colorize(lang.getString(path, "&c[Missing: " + path + "]"));
    }

    public String get(String path, String... replacements) {
        String msg = get(path);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }

    public List<String> getList(String path) {
        List<String> list = lang.getStringList(path);
        list.replaceAll(ColorUtil::colorize);
        return list;
    }

    public String formatItemName(ItemStack item) {
        if (item == null) return "Unknown";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) return item.getItemMeta().getDisplayName();
        return formatMaterial(item.getType().name());
    }

    public String formatMaterial(String name) {
        String[] parts = name.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    public String getChatBroadcast(String player, String amount, String item, String price) {
        if (!lang.getBoolean("chat-broadcast.enable")) return null;
        return ColorUtil.colorize(lang.getString("chat-broadcast.message", ""))
                .replace("%player%", player).replace("%amount%", amount)
                .replace("%item%", item).replace("%price%", price);
    }

    public String msg(String key) { return getPrefix() + get("messages." + key); }
    public String msg(String key, String... r) { return getPrefix() + get("messages." + key, r); }

    public String getAuctionCreated(String price) { return msg("auction-created", "%price%", price); }
    public String getItemBought(String amount, String item, String price) { return msg("item-bought", "%amount%", amount, "%item%", item, "%price%", price); }
    public String getItemSold(String player, String amount, String item, String price) { return msg("item-sold", "%player%", player, "%amount%", amount, "%item%", item, "%price%", price); }
    public String getConfigReloaded() { return msg("config-reloaded"); }
    public String getAuctionRemoved() { return msg("auction-removed"); }
    public String getItemReturned(String item) { return msg("item-returned", "%item%", item); }
    public String getAuctionAlreadySold() { return msg("auction-already-sold"); }
    public String getPriceTooLow(String amount) { return msg("price-too-low", "%amount%", amount); }
    public String getPriceTooHigh(String amount) { return msg("price-too-high", "%amount%", amount); }
    public String getInvalidPrice() { return msg("invalid-price"); }
    public String getNotEnoughMoney() { return msg("not-enough-money"); }
    public String getItemNotFound() { return msg("item-not-found"); }
    public String getInventoryFull() { return msg("inventory-full"); }
    public String getLimitReached(String current, String limit) { return msg("limit-reached", "%current%", current, "%limit%", limit); }
    public String getNoPermission() { return msg("no-permission"); }
    public String getPlayerOnly() { return msg("player-only"); }
    public String getNoItemInHand() { return msg("no-item-in-hand"); }
    public String getOwnAuction() { return msg("own-auction"); }
    public String getExpired(String amount, String item, String date) { return msg("expired", "%amount%", amount, "%item%", item, "%date%", date); }
    public String getNoTransactionsFound() { return msg("no-transactions-found"); }
    public String getSearchPrompt() { return get("messages.search"); }
    public String getSearchCancelled() { return get("messages.search-cancelled"); }
    public String getOfflineAuctionSold(String amount, String item, String price) { return msg("offline-auction-sold", "%amount%", amount, "%item%", item, "%price%", price); }
    public String getBlacklistedItem(String itemName) { return msg("blacklisted-item", "%item%", itemName); }
    public String getFastSellOn() { return msg("fast-sell-on"); }
    public String getFastSellOff() { return msg("fast-sell-off"); }
    public String getFastBuyOn() { return msg("fast-buy-on"); }
    public String getFastBuyOff() { return msg("fast-buy-off"); }
    public String getAdminRemoved(String staff, String amount, String item) { return msg("admin-removed", "%staff%", staff, "%amount%", amount, "%item%", item); }
    public String getAdminCancelled(String staff, String amount, String item) { return msg("admin-cancelled", "%staff%", staff, "%amount%", amount, "%item%", item); }
    public String getAdminRemovedInfo() { return msg("admin-removed-info"); }
    public String getAdminCancelledAllInfo() { return msg("admin-cancelled-all-info"); }
    public List<String> getHelp() { return getList("help"); }
    public List<String> getAdminHelp() { return getList("admin-help"); }
}
