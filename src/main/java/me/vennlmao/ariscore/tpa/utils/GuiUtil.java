package me.vennlmao.ariscore.tpa.utils;

import me.vennlmao.ariscore.tpa.TpaModule;
import me.vennlmao.ariscore.tpa.managers.TpaRequest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public class GuiUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static Inventory buildSenderGui(TpaModule plugin, TpaRequest request) {
        int size = plugin.getConfig().getInt("gui.size", 27);
        String title = plugin.getConfig().getString("gui.title", "Confirm Request");
        Inventory inv = Bukkit.createInventory(null, size, MM.deserialize(title));

        Player target = request.getReceiver();

        placeItem(plugin, inv, "gui.items.confirm_tpa",
                s -> s.replace("{player}", target.getName()));
        placeItem(plugin, inv, "gui.items.cancel", s -> s);
        placeItem(plugin, inv, "gui.items.location",
                s -> s.replace("{world}", target.getWorld().getName()));
        placeItem(plugin, inv, "gui.items.region",
                s -> s.replace("{playerPing}", String.valueOf(target.getPing())));
        placeHead(plugin, inv, target);

        return inv;
    }

    public static Inventory buildAcceptGui(TpaModule plugin, TpaRequest request) {
        int size = plugin.getConfig().getInt("gui.size", 27);
        String title = plugin.getConfig().getString("gui.title", "Confirm Request");
        Inventory inv = Bukkit.createInventory(null, size, MM.deserialize(title));

        Player sender = request.getSender();
        String confirmKey = request.getType() == TpaRequest.Type.TPA ? "confirm_tpa" : "confirm_tpahere";

        placeItem(plugin, inv, "gui.items." + confirmKey,
                s -> s.replace("{player}", sender.getName()));
        placeItem(plugin, inv, "gui.items.cancel", s -> s);
        placeItem(plugin, inv, "gui.items.location",
                s -> s.replace("{world}", sender.getWorld().getName()));
        placeItem(plugin, inv, "gui.items.region",
                s -> s.replace("{playerPing}", String.valueOf(sender.getPing())));
        placeHead(plugin, inv, sender);

        return inv;
    }

    private static void placeItem(TpaModule plugin, Inventory inv, String path, UnaryOperator<String> replacer) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection(path);
        if (sec == null) return;

        int slot = sec.getInt("slot", 0);
        Material mat = Material.matchMaterial(sec.getString("material", "STONE"));
        if (mat == null) mat = Material.STONE;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.displayName(parseColor(replacer.apply(sec.getString("name", ""))));

        List<Component> lore = new ArrayList<>();
        for (String l : sec.getStringList("lore")) {
            lore.add(parseColor(replacer.apply(l)));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    private static void placeHead(TpaModule plugin, Inventory inv, Player player) {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("gui.items.playerhead");
        if (sec == null) return;

        int slot = sec.getInt("slot", 13);
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return;

        meta.setOwningPlayer(player);
        meta.displayName(parseColor(sec.getString("name", "").replace("{playerName}", player.getName())));

        List<Component> lore = new ArrayList<>();
        for (String l : sec.getStringList("lore")) {
            lore.add(parseColor(l.replace("{playerName}", player.getName())));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    public static Component parseColor(String raw) {
        String s = raw
                .replaceAll("#([0-9A-Fa-f]{6})", "<color:#$1>")
                .replace("&a", "<green>").replace("&b", "<aqua>").replace("&c", "<red>")
                .replace("&d", "<light_purple>").replace("&e", "<yellow>").replace("&f", "<white>")
                .replace("&7", "<gray>").replace("&6", "<gold>").replace("&4", "<dark_red>")
                .replace("&2", "<dark_green>").replace("&l", "<bold>").replace("&o", "<italic>")
                .replace("&n", "<underlined>").replace("&m", "<strikethrough>")
                .replace("&k", "<obfuscated>").replace("&r", "<reset>");
        return MM.deserialize("<italic:false>" + s);
    }
                  }
        
