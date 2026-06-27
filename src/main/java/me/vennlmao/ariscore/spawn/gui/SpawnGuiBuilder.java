package me.vennlmao.ariscore.spawn.gui;

import me.vennlmao.ariscore.spawn.SpawnModule;
import me.vennlmao.ariscore.spawn.utils.ColorUtil;
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

public class SpawnGuiBuilder {

    public static Inventory buildSpawnsGui(SpawnModule module, Player player) {
        String title = module.getConfig().getString("gui.title", "&8ꜱᴘᴀᴡɴs");
        int size = module.getConfig().getInt("gui.size", 27);
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.parse(title));

        Map<String, Location> spawns = module.getSpawnManager().getAllSpawns();
        int total = spawns.size();

        String spawnMat  = module.getConfig().getString("gui.spawn-info.spawn.material", module.getConfig().getString("default-material", "STONE"));
        String spawnName = module.getConfig().getString("gui.spawn-info.spawn.displayName", "&bꜱᴘᴀᴡɴ %name%");
        List<String> spawnLore = module.getConfig().getStringList("gui.spawn-info.spawn.lore");

        int randomSlot       = module.getConfig().getInt("gui.spawn-info.random-spawn.slot", 22);
        String randomMat     = module.getConfig().getString("gui.spawn-info.random-spawn.material", module.getConfig().getString("default-material", "STONE"));
        String randomName    = module.getConfig().getString("gui.spawn-info.random-spawn.displayName", "&aꜱᴘᴀᴡɴ");
        List<String> randomLore = module.getConfig().getStringList("gui.spawn-info.random-spawn.lore");

        int slot = 0;
        int index = 0;
        for (Map.Entry<String, Location> entry : spawns.entrySet()) {
            if (slot >= size) break;
            if (slot == randomSlot) slot++;
            if (slot >= size) break;

            String name = entry.getKey();
            String displayName = spawnName
                    .replace("%name%", name)
                    .replace("%current%", String.valueOf(index + 1))
                    .replace("%max%", String.valueOf(total));

            List<String> loreReplaced = new ArrayList<>();
            for (String line : spawnLore) {
                loreReplaced.add(line
                        .replace("%name%", name)
                        .replace("%current%", String.valueOf(index + 1))
                        .replace("%max%", String.valueOf(total)));
            }

            inv.setItem(slot, buildItem(module, spawnMat, displayName, loreReplaced));
            slot++;
            index++;
        }

        if (!spawns.isEmpty()) {
            inv.setItem(randomSlot, buildItem(module, randomMat, randomName, randomLore));
        }

        return inv;
    }

    public static ItemStack buildItem(SpawnModule module, String materialName, String displayName, List<String> lore) {
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
