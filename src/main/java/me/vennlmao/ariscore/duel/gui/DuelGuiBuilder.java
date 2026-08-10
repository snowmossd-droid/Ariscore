package me.vennlmao.ariscore.duel.gui;

import me.vennlmao.ariscore.duel.DuelModule;
import me.vennlmao.ariscore.duel.managers.DuelArena;
import me.vennlmao.ariscore.duel.managers.DuelStats;
import me.vennlmao.ariscore.duel.utils.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public class DuelGuiBuilder {

    private final DuelModule module;

    public DuelGuiBuilder(DuelModule module) { this.module = module; }

    public Inventory buildQueueConfirm(Player viewer, Player opponent) {
        String title = module.getConfig().getString("gui.queue-confirm.title", "");
        int size = module.getConfig().getInt("gui.queue-confirm.size", 27);
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.parse(title));

        DuelStats stats = module.getStatsManager().getStats(viewer.getUniqueId());

        placeHead(inv, "gui.queue-confirm.items.opponent-info", opponent, s -> s.replace("{player}", opponent.getName()));
        placeControl(inv, "gui.queue-confirm.items.stats", s -> s
                .replace("{wins}", String.valueOf(stats.getWins()))
                .replace("{losses}", String.valueOf(stats.getLosses()))
                .replace("{streak}", String.valueOf(stats.getStreak())));
        placeControl(inv, "gui.queue-confirm.items.confirm", s -> s);
        placeControl(inv, "gui.queue-confirm.items.cancel", s -> s);

        return inv;
    }

    public Inventory buildCreateDuel(Player viewer, OfflinePlayer target, DuelArena arena) {
        String title = module.getConfig().getString("gui.create-duel.title", "")
                .replace("{player}", target.getName() != null ? target.getName() : "?");
        int size = module.getConfig().getInt("gui.create-duel.size", 27);
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.parse(title));

        placeHead(inv, "gui.create-duel.items.opponent-info", target, s -> s.replace("{player}", target.getName() != null ? target.getName() : "?"));

        String arenaName = arena != null ? arena.getName() : module.getConfig().getString("gui.create-duel.random-label", "Random");
        placeControl(inv, "gui.create-duel.items.arena", s -> s.replace("{arena}", arenaName));

        placeControl(inv, "gui.create-duel.items.confirm", s -> s);
        placeControl(inv, "gui.create-duel.items.cancel", s -> s);

        return inv;
    }

    private void placeHead(Inventory inv, String path, OfflinePlayer target, UnaryOperator<String> replacer) {
        ConfigurationSection sec = module.getConfig().getConfigurationSection(path);
        if (sec == null) return;
        int slot = sec.getInt("slot");

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return;
        meta.setOwningPlayer(target);
        meta.displayName(ColorUtil.parse(replacer.apply(sec.getString("display-name", ""))));

        List<Component> lore = new ArrayList<>();
        for (String l : sec.getStringList("lore")) lore.add(ColorUtil.parse(replacer.apply(l)));
        meta.lore(lore);
        head.setItemMeta(meta);
        inv.setItem(slot, head);
    }

    private void placeControl(Inventory inv, String path, UnaryOperator<String> replacer) {
        ConfigurationSection sec = module.getConfig().getConfigurationSection(path);
        if (sec == null) return;
        int slot = sec.getInt("slot");
        Material mat = Material.matchMaterial(sec.getString("material", "STONE"));
        if (mat == null) mat = Material.STONE;
        String name = replacer.apply(sec.getString("display-name", ""));
        inv.setItem(slot, buildItem(mat, name, sec.getStringList("lore"), replacer));
    }

    private ItemStack buildItem(Material mat, String name, List<String> lore, UnaryOperator<String> replacer) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(ColorUtil.parse(name));
        List<Component> loreComp = new ArrayList<>();
        for (String l : lore) loreComp.add(ColorUtil.parse(replacer.apply(l)));
        meta.lore(loreComp);
        item.setItemMeta(meta);
        return item;
    }
}
