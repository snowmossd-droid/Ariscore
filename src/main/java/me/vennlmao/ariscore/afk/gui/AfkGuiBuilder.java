package me.vennlmao.ariscore.afk.gui;

import me.vennlmao.ariscore.afk.AfkModule;
import me.vennlmao.ariscore.afk.utils.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AfkGuiBuilder {

    public static Inventory buildAfksGui(AfkModule module, Player player) {
        String title = module.getConfig().getString("gui.title", "&8ᴀꜰᴋ ᴢᴏɴᴇꜱ");
        int size = module.getConfig().getInt("gui.size", 27);
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.parse(title));

        Map<String, Location> zones = module.getAfkManager().getAllZones();
        int total = zones.size();

        String zoneMat   = module.getConfig().getString("gui.afk-info.afk.material", module.getConfig().getString("default-material", "STONE"));
        String zoneName  = module.getConfig().getString("gui.afk-info.afk.displayName", "&bᴀꜰᴋ &f%name%");
        List<String> zoneLore = module.getConfig().getStringList("gui.afk-info.afk.lore");

        int randomSlot       = module.getConfig().getInt("gui.afk-info.random-afk.slot", 22);
        String randomMat     = module.getConfig().getString("gui.afk-info.random-afk.material", module.getConfig().getString("default-material", "STONE"));
        String randomName    = module.getConfig().getString("gui.afk-info.random-afk.displayName", "&aᴀꜰᴋ ʀᴀɴᴅᴏᴍ");
        List<String> randomLore = module.getConfig().getStringList("gui.afk-info.random-afk.lore");

        int slot = 0;
        int index = 0;
        for (Map.Entry<String, Location> entry : zones.entrySet()) {
            if (slot >= size) break;
            if (slot == randomSlot) slot++;
            if (slot >= size) break;

            String name = entry.getKey();
            String displayName = zoneName
                    .replace("%name%", name)
                    .replace("%current%", String.valueOf(index + 1))
                    .replace("%max%", String.valueOf(total));

            List<String> loreReplaced = new ArrayList<>();
            for (String line : zoneLore) {
                loreReplaced.add(line
                        .replace("%name%", name)
                        .replace("%current%", String.valueOf(index + 1))
                        .replace("%max%", String.valueOf(total)));
            }

            inv.setItem(slot, buildItem(module, zoneMat, displayName, loreReplaced));
            slot++;
            index++;
        }

        if (!zones.isEmpty()) {
            inv.setItem(randomSlot, buildItem(module, randomMat, randomName, randomLore));
        }

        return inv;
    }

    public static ItemStack buildItem(AfkModule module, String materialName, String displayName, List<String> lore) {
        Material mat = Material.matchMaterial(materialName);
        if (mat == null) {
            String fallback = module.getConfig().getString("default-material", "STONE");
            mat = Material.matchMaterial(fallback);
            if (mat == null) mat = Material.STONE;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(ColorUtil.parse(displayName));

        List<Component> loreComp = new ArrayList<>();
        for (String l : lore) loreComp.add(ColorUtil.parse(l));
        meta.lore(loreComp);

        item.setItemMeta(meta);
        return item;
    }
}
