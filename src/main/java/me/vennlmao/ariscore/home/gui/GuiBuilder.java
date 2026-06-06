package me.vennlmao.ariscore.home.gui;

import me.vennlmao.ariscore.home.HomeModule;
import me.vennlmao.ariscore.home.utils.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GuiBuilder {

    public static Inventory buildHomesGui(HomeModule plugin, Player player) {
        String title = plugin.getConfig().getString("gui.title", "&8ʜᴏᴍᴇꜱ");
        int size = plugin.getConfig().getInt("gui.size", 36);
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.parse(title));

        List<Integer> bedSlots = parseSlots(plugin.getConfig().getString("gui.slots.beds", "11,12,13,14,15"));
        List<Integer> dyeSlots = parseSlots(plugin.getConfig().getString("gui.slots.dyes", "20,21,22,23,24"));

        List<String> playerHomes = plugin.getHomeManager().getHomes(player);
        int maxHomes = plugin.getHomeManager().getMaxHomes(player);
        int totalSlots = Math.min(bedSlots.size(), dyeSlots.size());

        for (int i = 0; i < totalSlots; i++) {
            int bedSlot = bedSlots.get(i);
            int dyeSlot = dyeSlots.get(i);

            boolean hasPermission = i < maxHomes;
            boolean homeSet = i < playerHomes.size();
            String homeName = homeSet ? playerHomes.get(i) : "home" + (i + 1);

            if (!hasPermission) {
                inv.setItem(bedSlot, buildConfigItem(plugin, "gui.no-permission.bed", homeName));
                inv.setItem(dyeSlot, buildConfigItem(plugin, "gui.no-permission.dye", homeName));
            } else if (homeSet) {
                inv.setItem(bedSlot, buildConfigItem(plugin, "gui.home-set.bed", homeName));
                inv.setItem(dyeSlot, buildConfigItem(plugin, "gui.home-set.dye", homeName));
            } else {
                inv.setItem(bedSlot, buildConfigItem(plugin, "gui.home-not-set.bed", homeName));
                inv.setItem(dyeSlot, buildConfigItem(plugin, "gui.home-not-set.dye", homeName));
            }
        }

        return inv;
    }

    public static Inventory buildConfirmDelete(HomeModule plugin, String homeName) {
        String title = plugin.getConfig().getString("gui-confirm-delete.title", "&8Confirm Delete");
        int size = plugin.getConfig().getInt("gui-confirm-delete.size", 27);
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.parse(title));

        ConfigurationSection items = plugin.getConfig().getConfigurationSection("gui-confirm-delete.items");
        if (items == null) return inv;

        for (String key : items.getKeys(false)) {
            String path = "gui-confirm-delete.items." + key;
            int slot = plugin.getConfig().getInt(path + ".slot", 0);
            inv.setItem(slot, buildConfigItem(plugin, path, homeName));
        }

        return inv;
    }

    public static ItemStack buildConfigItem(HomeModule plugin, String path, String homeName) {
        String matName = plugin.getConfig().getString(path + ".material", "STONE");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.STONE;

        String name = plugin.getConfig().getString(path + ".name", "").replace("%home%", homeName);
        List<String> lore = plugin.getConfig().getStringList(path + ".lore");

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(ColorUtil.parse(name));

        List<Component> loreComp = new ArrayList<>();
        for (String l : lore) loreComp.add(ColorUtil.parse(l.replace("%home%", homeName)));
        meta.lore(loreComp);
        item.setItemMeta(meta);
        return item;
    }

    public static List<Integer> parseSlots(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }
}
