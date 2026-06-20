package me.vennlmao.ariscore.order.gui;

import me.vennlmao.ariscore.order.utils.ColorUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GuiUtil {

    private GuiUtil() {}

    public static ItemStack buildItem(ConfigurationSection section, Map<String, String> placeholders) {
        if (section == null) return new ItemStack(Material.STONE);
        String matName = section.getString("material", "STONE");
        Material material;
        try { material = Material.valueOf(matName.toUpperCase()); }
        catch (IllegalArgumentException e) { material = Material.STONE; }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        String name = section.getString("name", section.getString("displayname", " "));
        meta.setDisplayName(ColorUtil.color(applyPlaceholders(name, placeholders)));
        List<String> loreRaw = section.getStringList("lore");
        List<String> lore = new ArrayList<>();
        for (String line : loreRaw) {
            String applied = applyPlaceholders(line, placeholders);
            if (applied.contains("\n")) {
                for (String subLine : applied.split("\n")) lore.add(ColorUtil.color(subLine));
            } else {
                lore.add(ColorUtil.color(applied));
            }
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack buildItem(ConfigurationSection section) {
        return buildItem(section, null);
    }

    public static ItemStack buildFiller(ConfigurationSection section) {
        if (section == null) {
            ItemStack f = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta m = f.getItemMeta();
            if (m != null) { m.setDisplayName(" "); f.setItemMeta(m); }
            return f;
        }
        return buildItem(section);
    }

    public static String applyPlaceholders(String text, Map<String, String> placeholders) {
        if (text == null) return "";
        if (placeholders == null) return text;
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            if (e.getValue() != null) text = text.replace(e.getKey(), e.getValue());
        }
        return text;
    }

    public static List<Integer> parseSlots(String raw) {
        List<Integer> slots = new ArrayList<>();
        if (raw == null || raw.isEmpty()) return slots;
        for (String part : raw.split(",")) {
            part = part.trim();
            if (part.contains("-")) {
                String[] range = part.split("-");
                try {
                    int from = Integer.parseInt(range[0].trim());
                    int to   = Integer.parseInt(range[1].trim());
                    for (int i = from; i <= to; i++) slots.add(i);
                } catch (Exception ignored) {}
            } else {
                try { slots.add(Integer.parseInt(part)); } catch (Exception ignored) {}
            }
        }
        return slots;
    }
}
