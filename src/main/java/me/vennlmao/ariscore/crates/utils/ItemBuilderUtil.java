package me.vennlmao.ariscore.crates.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.List;

public class ItemBuilderUtil {

    public static ItemStack fromSection(ConfigurationSection section) {
        String materialName = section.getString("material", "STONE").toUpperCase();
        Material material = Material.getMaterial(materialName);
        if (material == null) material = Material.STONE;

        ItemStack item;
        String base64 = section.getString("base64", null);
        if (material == Material.PLAYER_HEAD && base64 != null) {
            item = SkullUtil.fromBase64(base64);
        } else {
            item = new ItemStack(material);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String name = section.getString("name", null);
        if (name != null) meta.setDisplayName(ColorUtil.translate(name));

        List<String> lore = section.getStringList("lore");
        if (!lore.isEmpty()) meta.setLore(ColorUtil.translate(lore));

        int customModelData = section.getInt("custom-model-data", 0);
        if (customModelData > 0) meta.setCustomModelData(customModelData);

        int stack = section.getInt("stack", 1);
        if (stack > 1) item.setAmount(Math.min(stack, item.getMaxStackSize()));

        ConfigurationSection trimSection = section.getConfigurationSection("trim");
        if (trimSection != null && meta instanceof ArmorMeta armorMeta) {
            String trimMat = trimSection.getString("material", null);
            String trimPat = trimSection.getString("pattern", null);
            if (trimMat != null && trimPat != null) {
                TrimMaterial tm = Registry.TRIM_MATERIAL.get(NamespacedKey.minecraft(trimMat.toLowerCase()));
                TrimPattern tp = Registry.TRIM_PATTERN.get(NamespacedKey.minecraft(trimPat.toLowerCase()));
                if (tm != null && tp != null) armorMeta.setTrim(new ArmorTrim(tm, tp));
            }
        }

        ConfigurationSection enchSection = section.getConfigurationSection("enchantments");
        if (enchSection != null) {
            for (String enchName : enchSection.getKeys(false)) {
                Enchantment ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(enchName.toLowerCase()));
                if (ench != null) meta.addEnchant(ench, enchSection.getInt(enchName, 1), true);
            }
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DESTROYS);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack plainItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.translate(name));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }
}
