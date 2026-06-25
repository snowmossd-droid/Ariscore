package me.vennlmao.ariscore.rtp.utils;

import me.vennlmao.ariscore.rtp.RtpModule;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GuiUtil {

    public static Inventory buildMainGui(RtpModule plugin) {
        String title = plugin.getConfig().getString("gui.main.title", "");
        int size = plugin.getConfig().getInt("gui.main.size", 27);
        Inventory inv = Bukkit.createInventory(null, size, MessageUtil.parse(title));

        ConfigurationSection worlds = plugin.getConfig().getConfigurationSection("worlds");
        if (worlds == null) return inv;

        for (String key : worlds.getKeys(false)) {
            ConfigurationSection sec = worlds.getConfigurationSection(key);
            if (sec == null) continue;
            int slot = sec.getInt("slot", 0);
            if (slot < 0 || slot >= size) continue;
            inv.setItem(slot, buildItem(sec));
        }

        return inv;
    }

    public static Inventory buildSubWorldGui(RtpModule plugin, String worldKey) {
        String title = plugin.getConfig().getString("gui.world_select.title", "");
        int size = plugin.getConfig().getInt("gui.world_select.size", 27);
        Inventory inv = Bukkit.createInventory(null, size, MessageUtil.parse(title));

        ConfigurationSection subWorlds = plugin.getConfig()
                .getConfigurationSection("worlds." + worldKey + ".sub_worlds");
        if (subWorlds == null) return inv;

        for (String key : subWorlds.getKeys(false)) {
            ConfigurationSection sec = subWorlds.getConfigurationSection(key);
            if (sec == null) continue;
            int slot = sec.getInt("slot", 0);
            if (slot < 0 || slot >= size) continue;
            inv.setItem(slot, buildItem(sec));
        }

        return inv;
    }

    private static ItemStack buildItem(ConfigurationSection sec) {
        String matName = sec.getString("material", "STONE");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.STONE;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String displayName = sec.getString("display-name", "");
        meta.displayName(MessageUtil.parse(displayName));

        List<Component> lore = new ArrayList<>();
        for (String line : sec.getStringList("lore")) {
            lore.add(MessageUtil.parse(line));
        }
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    public static String stripColor(String s) {
        if (s == null) return "";
        return s.replaceAll("&[0-9a-fk-orA-FK-OR]", "")
                .replaceAll("&#[0-9A-Fa-f]{6}", "")
                .trim();
    }
}
