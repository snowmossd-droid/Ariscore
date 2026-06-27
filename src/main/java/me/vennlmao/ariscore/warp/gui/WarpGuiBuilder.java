package me.vennlmao.ariscore.warp.gui;

import me.vennlmao.ariscore.warp.WarpModule;
import me.vennlmao.ariscore.warp.utils.ColorUtil;
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

public class WarpGuiBuilder {

    public static Inventory buildWarpsGui(WarpModule module, Player player) {
        String title = module.getConfig().getString("gui.title", "&8ᴡᴀʀᴘs");
        int size     = module.getConfig().getInt("gui.size", 27);
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.parse(title));

        Map<String, Location> warps = module.getWarpManager().getAllWarps();
        int total = warps.size();

        String warpMat  = module.getConfig().getString("gui.warp-info.warp.material",
                module.getConfig().getString("default-material", "STONE"));
        String warpName = module.getConfig().getString("gui.warp-info.warp.displayName", "&bᴡᴀʀᴘ &f%name%");
        List<String> warpLore = module.getConfig().getStringList("gui.warp-info.warp.lore");

        int randomSlot      = module.getConfig().getInt("gui.warp-info.random-warp.slot", 22);
        String randomMat    = module.getConfig().getString("gui.warp-info.random-warp.material",
                module.getConfig().getString("default-material", "STONE"));
        String randomName   = module.getConfig().getString("gui.warp-info.random-warp.displayName", "&aᴡᴀʀᴘ ʀᴀɴᴅᴏᴍ");
        List<String> randomLore = module.getConfig().getStringList("gui.warp-info.random-warp.lore");

        int slot = 0, index = 0;
        for (Map.Entry<String, Location> entry : warps.entrySet()) {
            if (slot >= size) break;
            if (slot == randomSlot) slot++;
            if (slot >= size) break;

            String name = entry.getKey();
            String displayName = warpName
                    .replace("%name%", name)
                    .replace("%current%", String.valueOf(index + 1))
                    .replace("%max%", String.valueOf(total));

            List<String> lore = new ArrayList<>();
            for (String l : warpLore) lore.add(l
                    .replace("%name%", name)
                    .replace("%current%", String.valueOf(index + 1))
                    .replace("%max%", String.valueOf(total)));

            inv.setItem(slot, buildItem(module, warpMat, displayName, lore));
            slot++; index++;
        }

        if (!warps.isEmpty()) inv.setItem(randomSlot, buildItem(module, randomMat, randomName, randomLore));

        return inv;
    }

    public static ItemStack buildItem(WarpModule module, String materialName, String displayName, List<String> lore) {
        Material mat = Material.matchMaterial(materialName);
        if (mat == null) {
            mat = Material.matchMaterial(module.getConfig().getString("default-material", "STONE"));
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
