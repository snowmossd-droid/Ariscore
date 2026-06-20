package me.vennlmao.ariscore.sell.managers;

import me.vennlmao.ariscore.sell.SellModule;
import me.vennlmao.ariscore.sell.utils.ColorUtil;
import me.vennlmao.ariscore.sell.utils.FormatUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SellWandManager {

    public enum WandType { USES, TIME }

    private final SellModule module;
    private final NamespacedKey idKey;
    private final NamespacedKey typeKey;
    private final NamespacedKey usesKey;
    private final NamespacedKey expiryKey;
    private final NamespacedKey itemsSoldKey;
    private final NamespacedKey moneyMadeKey;
    private final NamespacedKey lastUsedByKey;

    public SellWandManager(SellModule module) {
        this.module = module;
        Plugin plugin = module.getPlugin();
        this.idKey = new NamespacedKey(plugin, "sell_wand_id");
        this.typeKey = new NamespacedKey(plugin, "sell_wand_type");
        this.usesKey = new NamespacedKey(plugin, "sell_wand_uses");
        this.expiryKey = new NamespacedKey(plugin, "sell_wand_expiry");
        this.itemsSoldKey = new NamespacedKey(plugin, "sell_wand_items_sold");
        this.moneyMadeKey = new NamespacedKey(plugin, "sell_wand_money_made");
        this.lastUsedByKey = new NamespacedKey(plugin, "sell_wand_last_used_by");
    }

    private FileConfiguration cfg() {
        return module.getGuiManager().getGuiConfig("sellwand");
    }

    public ItemStack createWand(WandType type, long value) {
        FileConfiguration config = cfg();
        String configKey = type == WandType.USES ? "sellstick" : "sellstick-timebased";
        Material material = Material.valueOf(config.getString(configKey + ".material"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        pdc.set(idKey, PersistentDataType.STRING, UUID.randomUUID().toString().substring(0, 8));
        pdc.set(typeKey, PersistentDataType.STRING, type.name());
        pdc.set(itemsSoldKey, PersistentDataType.INTEGER, 0);
        pdc.set(moneyMadeKey, PersistentDataType.DOUBLE, 0.0);
        pdc.set(lastUsedByKey, PersistentDataType.STRING, config.getString(configKey + (type == WandType.USES ? ".not-used-yet-uses" : ".not-used-yet-timebased")));

        if (type == WandType.USES) {
            pdc.set(usesKey, PersistentDataType.LONG, value);
        } else {
            pdc.set(expiryKey, PersistentDataType.LONG, System.currentTimeMillis() + value);
        }

        item.setItemMeta(meta);
        updateLore(item);
        return item;
    }

    public void updateLore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (updateLore(meta)) item.setItemMeta(meta);
    }

    public boolean updateLore(ItemMeta meta) {
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String typeRaw = pdc.get(typeKey, PersistentDataType.STRING);
        if (typeRaw == null) return false;

        WandType type;
        try {
            type = WandType.valueOf(typeRaw);
        } catch (IllegalArgumentException e) {
            return false;
        }

        FileConfiguration config = cfg();
        String configKey = type == WandType.USES ? "sellstick" : "sellstick-timebased";
        meta.setDisplayName(ColorUtil.colorize(config.getString(configKey + ".displayname")));

        String id = pdc.get(idKey, PersistentDataType.STRING);
        int itemsSold = pdc.getOrDefault(itemsSoldKey, PersistentDataType.INTEGER, 0);
        double moneyMade = pdc.getOrDefault(moneyMadeKey, PersistentDataType.DOUBLE, 0.0);
        String lastUsedBy = pdc.get(lastUsedByKey, PersistentDataType.STRING);

        String usesStr;
        if (type == WandType.USES) {
            long uses = pdc.getOrDefault(usesKey, PersistentDataType.LONG, 0L);
            usesStr = uses == -1L ? config.getString(configKey + ".unlimited-uses-text") : String.valueOf(uses);
        } else {
            long expiry = pdc.getOrDefault(expiryKey, PersistentDataType.LONG, 0L);
            usesStr = expiry <= System.currentTimeMillis() ? "Expired" : formatTimeLeft(expiry);
        }

        List<String> rawLore = config.getStringList(configKey + ".lore");
        List<String> coloredLore = new ArrayList<>(rawLore.size());
        String itemsSoldStr = String.valueOf(itemsSold);
        String moneyMadeStr = FormatUtils.formatPrice(moneyMade);
        for (String line : rawLore) {
            coloredLore.add(ColorUtil.colorize(line
                    .replace("{uses}", usesStr)
                    .replace("{lastusedby}", lastUsedBy != null ? lastUsedBy : "")
                    .replace("{items_sold}", itemsSoldStr)
                    .replace("{money_made}", moneyMadeStr)
                    .replace("{id}", id != null ? id : "")));
        }
        meta.setLore(coloredLore);
        return true;
    }

    public void updateStats(ItemStack item, String playerName, int itemsSold, double moneyMade) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        pdc.set(itemsSoldKey, PersistentDataType.INTEGER, pdc.getOrDefault(itemsSoldKey, PersistentDataType.INTEGER, 0) + itemsSold);
        pdc.set(moneyMadeKey, PersistentDataType.DOUBLE, pdc.getOrDefault(moneyMadeKey, PersistentDataType.DOUBLE, 0.0) + moneyMade);
        pdc.set(lastUsedByKey, PersistentDataType.STRING, playerName);

        String typeStr = pdc.get(typeKey, PersistentDataType.STRING);
        if (typeStr != null && typeStr.equals(WandType.USES.name())) {
            long uses = pdc.getOrDefault(usesKey, PersistentDataType.LONG, 0L);
            if (uses > 0L) pdc.set(usesKey, PersistentDataType.LONG, uses - 1L);
        }

        updateLore(meta);
        item.setItemMeta(meta);
    }

    public boolean isWand(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(typeKey, PersistentDataType.STRING);
    }

    public boolean isValid(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return isValid(item.getItemMeta());
    }

    public boolean isValid(ItemMeta meta) {
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String typeRaw = pdc.get(typeKey, PersistentDataType.STRING);
        if (typeRaw == null) return false;

        WandType type;
        try {
            type = WandType.valueOf(typeRaw);
        } catch (IllegalArgumentException e) {
            return false;
        }

        if (type == WandType.USES) {
            long uses = pdc.getOrDefault(usesKey, PersistentDataType.LONG, 0L);
            return uses == -1L || uses > 0L;
        }

        long expiry = pdc.getOrDefault(expiryKey, PersistentDataType.LONG, 0L);
        return expiry > System.currentTimeMillis();
    }

    public NamespacedKey getTypeKey() {
        return typeKey;
    }

    private String formatTimeLeft(long expiry) {
        long diff = expiry - System.currentTimeMillis();
        if (diff <= 0L) return "Expired";
        long days = diff / 86400000L;
        long hours = diff / 3600000L % 24L;
        long minutes = diff / 60000L % 60L;
        if (days > 0L) return days + "d " + hours + "h";
        if (hours > 0L) return hours + "h " + minutes + "m";
        return minutes + "m";
    }

    public long parseTime(String timeStr) {
        try {
            long time = Long.parseLong(timeStr.substring(0, timeStr.length() - 1));
            char unit = timeStr.charAt(timeStr.length() - 1);
            switch (unit) {
                case 'd': return time * 1000L * 60L * 60L * 24L;
                case 'h': return time * 1000L * 60L * 60L;
                case 'm': return time * 1000L * 60L;
                case 's': return time * 1000L;
                default: return 0L;
            }
        } catch (Exception e) {
            return 0L;
        }
    }
}
