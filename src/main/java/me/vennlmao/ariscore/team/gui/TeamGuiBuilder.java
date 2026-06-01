package me.vennlmao.ariscore.team.gui;

import me.vennlmao.ariscore.team.TeamModule;
import me.vennlmao.ariscore.team.managers.TeamData;
import me.vennlmao.ariscore.team.utils.ColorUtil;
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

import java.util.*;

public class TeamGuiBuilder {

    private final TeamModule module;

    public TeamGuiBuilder(TeamModule module) {
        this.module = module;
    }

    public Inventory buildMain(Player viewer, TeamData team) {
        String title = module.getConfig().getString("gui.main.title", "&8Team {team}")
                .replace("{team}", team.getName());
        int size = module.getConfig().getInt("gui.main.size", 54);
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.parse(title));

        List<UUID> members = new ArrayList<>(team.getMembers().keySet());
        for (int i = 0; i < members.size() && i < 45; i++) {
            UUID uuid = members.get(i);
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(op);
                meta.displayName(ColorUtil.parse("&f" + op.getName()));
                List<Component> lore = new ArrayList<>();
                lore.add(ColorUtil.parse("&7" + team.getRoleString(uuid)));
                if (uuid.equals(viewer.getUniqueId()) || team.isOwnerOrCoOwner(viewer.getUniqueId())) {
                    lore.add(ColorUtil.parse("&eClick to manage"));
                }
                meta.lore(lore);
                head.setItemMeta(meta);
            }
            inv.setItem(i, head);
        }

        placeItem(inv, "gui.main.items.team_home", s -> s.replace("{team}", team.getName()));
        placeItem(inv, "gui.main.items.search", s -> s);
        if (team.isOwnerOrCoOwner(viewer.getUniqueId())) {
            placeItem(inv, "gui.main.items.back", s -> s);
        }

        return inv;
    }

    public Inventory buildEdit(TeamData team) {
        String title = module.getConfig().getString("gui.edit.title", "&8Edit {team}")
                .replace("{team}", team.getName());
        int size = module.getConfig().getInt("gui.edit.size", 27);
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.parse(title));

        placeItem(inv, "gui.edit.items.invite", s -> s);
        placeItem(inv, "gui.edit.items.kick", s -> s);
        placeItem(inv, "gui.edit.items.sethome", s -> s);
        placeItem(inv, "gui.edit.items.delhome", s -> s);
        placeItem(inv, "gui.edit.items.disband", s -> s);

        return inv;
    }

    public Inventory buildConfirmDisband(TeamData team) {
        String title = module.getConfig().getString("gui.confirm_disband.title", "&8Confirm Disband");
        int size = module.getConfig().getInt("gui.confirm_disband.size", 27);
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.parse(title));
        placeItem(inv, "gui.confirm_disband.items.confirm", s -> s.replace("{team}", team.getName()));
        placeItem(inv, "gui.confirm_disband.items.cancel", s -> s);
        return inv;
    }

    public Inventory buildConfirmKick(OfflinePlayer target, String role) {
        String title = module.getConfig().getString("gui.confirm_kick.title", "&8Confirm Kick");
        int size = module.getConfig().getInt("gui.confirm_kick.size", 27);
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.parse(title));
        placeItem(inv, "gui.confirm_kick.items.confirm",
                s -> s.replace("{player}", target.getName() != null ? target.getName() : "?"));
        placeItem(inv, "gui.confirm_kick.items.cancel", s -> s);

        ConfigurationSection headSec = module.getConfig().getConfigurationSection("gui.confirm_kick.items.player_head");
        if (headSec != null) {
            int slot = headSec.getInt("slot", 13);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(target);
                String name = headSec.getString("name", "&f{player}")
                        .replace("{player}", target.getName() != null ? target.getName() : "?");
                meta.displayName(ColorUtil.parse(name));
                List<Component> lore = new ArrayList<>();
                for (String l : headSec.getStringList("lore")) {
                    lore.add(ColorUtil.parse(l.replace("{role}", role)));
                }
                meta.lore(lore);
                head.setItemMeta(meta);
            }
            inv.setItem(slot, head);
        }

        return inv;
    }

    private void placeItem(Inventory inv, String path, java.util.function.UnaryOperator<String> replacer) {
        ConfigurationSection sec = module.getConfig().getConfigurationSection(path);
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
