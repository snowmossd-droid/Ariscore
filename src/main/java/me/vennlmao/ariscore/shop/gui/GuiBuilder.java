package me.vennlmao.ariscore.shop.gui;

import me.vennlmao.ariscore.shop.ShopModule;
import me.vennlmao.ariscore.shop.managers.ShopItem;
import me.vennlmao.ariscore.shop.utils.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public class GuiBuilder {

    public static Inventory buildMainShop(ShopModule plugin) {
        int size = plugin.getConfig().getInt("gui.size", 27);
        String title = plugin.getConfig().getString("gui.title", "Shop");
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.parse(title));

        ConfigurationSection cats = plugin.getConfig().getConfigurationSection("gui.categories");
        if (cats == null) return inv;

        for (String key : cats.getKeys(false)) {
            String path = "gui.categories." + key;
            int slot = plugin.getConfig().getInt(path + ".slot", 0);
            Material mat = Material.matchMaterial(plugin.getConfig().getString(path + ".material", "STONE"));
            if (mat == null) mat = Material.STONE;
            String name = plugin.getConfig().getString(path + ".name", key);
            List<String> lore = plugin.getConfig().getStringList(path + ".lore");

            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;
            meta.displayName(ColorUtil.parse(name));
            List<Component> loreComp = new ArrayList<>();
            for (String l : lore) loreComp.add(ColorUtil.parse(l));
            meta.lore(loreComp);
            item.setItemMeta(meta);
            inv.setItem(slot, item);
        }

        return inv;
    }

    public static Inventory buildCategory(ShopModule plugin, String category) {
        java.io.File f = new java.io.File(plugin.getPlugin().getDataFolder(), "categories/" + category + ".yml");
        org.bukkit.configuration.file.YamlConfiguration cfg =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);

        int size = cfg.getInt("size", 27);
        String title = cfg.getString("title", category);
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.parse(title));

        for (ShopItem item : plugin.getShopManager().getItems(category)) {
            ItemStack stack = new ItemStack(item.getMaterial());
            ItemMeta meta = stack.getItemMeta();
            if (meta == null) continue;
            meta.displayName(ColorUtil.parse(item.getName()));
            List<Component> lore = new ArrayList<>();
            for (String l : item.getLore()) {
                String replaced = l.replace("{price}", String.valueOf((int) item.getPrice()))
                        .replace("$", "");
                lore.add(ColorUtil.parse(replaced));
            }
            meta.lore(lore);
            stack.setItemMeta(meta);
            inv.setItem(item.getSlot(), stack);
        }

        placeButton(plugin, inv, "gui-back.back", s -> s);
        return inv;
    }

    public static Inventory buildPurchase(ShopModule plugin, ShopItem item, int amount) {
        String rawTitle = plugin.getConfig().getString("gui-purchase.title", "Mua {currentItem}");
        String title = rawTitle.replace("{currentItem}", ColorUtil.strip(item.getName()));
        int size = plugin.getConfig().getInt("gui-purchase.size", 27);
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.parse(title));

        int maxStack = item.getStack();

        placeButton(plugin, inv, "gui-purchase.confirm", s -> s);
        placeButton(plugin, inv, "gui-purchase.cancel", s -> s);

        if (maxStack == 1) {
            placeDisplay(plugin, inv, item, amount);
            return inv;
        }

        boolean atMin = amount <= 1;
        boolean atMax = amount >= maxStack;
        boolean canRemoveTen = amount > 10;

        if (!atMin) {
            placeButton(plugin, inv, "gui-purchase.remove.one", s -> s);
        }
        if (canRemoveTen) {
            placeButton(plugin, inv, "gui-purchase.remove.ten", s -> s);
        }
        if (atMax) {
            placeButton(plugin, inv, "gui-purchase.remove.max",
                    s -> s.replace("{maxAmount}", String.valueOf(maxStack)));
        }

        if (!atMax) {
            placeButton(plugin, inv, "gui-purchase.add.one", s -> s);
            if (amount + 10 <= maxStack) {
                placeButton(plugin, inv, "gui-purchase.add.ten", s -> s);
            }
            placeButton(plugin, inv, "gui-purchase.add.max",
                    s -> s.replace("{maxAmount}", String.valueOf(maxStack)));
        }

        placeDisplay(plugin, inv, item, amount);
        return inv;
    }

    private static void placeDisplay(ShopModule plugin, Inventory inv, ShopItem item, int amount) {
        String displayPath = item.isShard() ? "gui-purchase.display_shards" : "gui-purchase.display_money";
        ConfigurationSection display = plugin.getConfig().getConfigurationSection(displayPath);
        if (display == null) return;

        int slot = display.getInt("slot", 13);
        int displayAmount = Math.min(amount, item.getMaterial().getMaxStackSize());
        ItemStack dispItem = new ItemStack(item.getMaterial(), displayAmount);
        ItemMeta meta = dispItem.getItemMeta();
        if (meta == null) return;

        meta.displayName(ColorUtil.parse(item.getName()));
        double total = item.getPrice() * amount;
        List<Component> lore = new ArrayList<>();
        for (String l : display.getStringList("lore")) {
            String replaced = l
                    .replace("{amount}", String.valueOf(amount))
                    .replace("{moneyprice}", String.valueOf((int) item.getPrice()))
                    .replace("{totalmoney}", String.valueOf((int) total))
                    .replace("{shardsprice}", String.valueOf((int) item.getPrice()))
                    .replace("{totalShards}", String.valueOf((int) total));
            lore.add(ColorUtil.parse(replaced));
        }
        meta.lore(lore);
        dispItem.setItemMeta(meta);
        inv.setItem(slot, dispItem);
    }

    private static void placeButton(ShopModule plugin, Inventory inv, String path,
                                    UnaryOperator<String> replacer) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection(path);
        if (sec == null) return;
        int slot = sec.getInt("slot", 0);
        Material mat = Material.matchMaterial(sec.getString("material", "STONE"));
        if (mat == null) mat = Material.STONE;
        String name = replacer.apply(sec.getString("name", ""));
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.displayName(ColorUtil.parse(name));
        List<Component> lore = new ArrayList<>();
        for (String l : sec.getStringList("lore")) lore.add(ColorUtil.parse(replacer.apply(l)));
        meta.lore(lore);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }
}
