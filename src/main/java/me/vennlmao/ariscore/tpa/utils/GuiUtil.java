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
        String path = "gui.sender";
        int size = plugin.getConfig().getInt(path + ".size", 27);
        String title = plugin.getConfig().getString(path + ".title", "&8ᴄᴏɴғɪʀᴍ ʀᴇǫᴜᴇsᴛ");
        Inventory inv = Bukkit.createInventory(null, size, parseColor(title));

        Player target = request.getReceiver();
        String playerName = target.getName();
        String world = target.getWorld() != null ? target.getWorld().getName() : "unknown";
        String ping = String.valueOf(target.getPing());

        UnaryOperator<String> replacer = s -> s
                .replace("{player}", playerName)
                .replace("{world}", world)
                .replace("{ping}", ping);

        ConfigurationSection icons = plugin.getConfig().getConfigurationSection(path + ".icons");
        if (icons != null) {
            for (String key : icons.getKeys(false)) {
                if (key.equals("player")) {
                    placeHead(plugin, inv, icons.getConfigurationSection(key), target, replacer);
                } else {
                    placeItem(inv, icons.getConfigurationSection(key), replacer);
                }
            }
        }

        return inv;
    }

    public static Inventory buildAcceptGui(TpaModule plugin, TpaRequest request) {
        String path = "gui.accept";
        int size = plugin.getConfig().getInt(path + ".size", 27);
        String title = plugin.getConfig().getString(path + ".title", "&8ᴀᴄᴄᴇᴘᴛ ʀᴇǫᴜᴇsᴛ");
        Inventory inv = Bukkit.createInventory(null, size, parseColor(title));

        Player sender = request.getSender();
        String playerName = sender.getName();
        String world = sender.getWorld() != null ? sender.getWorld().getName() : "unknown";
        String ping = String.valueOf(sender.getPing());

        UnaryOperator<String> replacer = s -> s
                .replace("{player}", playerName)
                .replace("{world}", world)
                .replace("{ping}", ping);

        ConfigurationSection icons = plugin.getConfig().getConfigurationSection(path + ".icons");
        if (icons != null) {
            for (String key : icons.getKeys(false)) {
                if (key.equals("player")) {
                    placeHead(plugin, inv, icons.getConfigurationSection(key), sender, replacer);
                } else {
                    placeItem(inv, icons.getConfigurationSection(key), replacer);
                }
            }
        }

        return inv;
    }

    private static void placeItem(Inventory inv, ConfigurationSection sec, UnaryOperator<String> replacer) {
        if (sec == null) return;

        int slot = sec.getInt("slot", 0);
        if (slot < 0 || slot >= inv.getSize()) return;

        Material mat = Material.matchMaterial(sec.getString("material", "STONE"));
        if (mat == null) mat = Material.STONE;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String displayName = sec.getString("display-name", "");
        meta.displayName(parseColor(replacer.apply(displayName)));

        List<Component> lore = new ArrayList<>();
        for (String l : sec.getStringList("lore")) {
            lore.add(parseColor(replacer.apply(l)));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    private static void placeHead(TpaModule plugin, Inventory inv, ConfigurationSection sec,
                                  Player player, UnaryOperator<String> replacer) {
        if (sec == null) return;

        int slot = sec.getInt("slot", 13);
        if (slot < 0 || slot >= inv.getSize()) return;

        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return;

        meta.setOwningPlayer(player);

        String displayName = sec.getString("display-name", "&aᴘʟᴀʏᴇʀ");
        meta.displayName(parseColor(replacer.apply(displayName)));

        List<Component> lore = new ArrayList<>();
        for (String l : sec.getStringList("lore")) {
            lore.add(parseColor(replacer.apply(l)));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    public static Component parseColor(String raw) {
        if (raw == null) return Component.empty();
        String s = raw
                .replaceAll("&#([0-9A-Fa-f]{6})", "<color:#$1>")
                .replaceAll("#([0-9A-Fa-f]{6})", "<color:#$1>")
                .replace("&a", "<green>").replace("&b", "<aqua>").replace("&c", "<red>")
                .replace("&d", "<light_purple>").replace("&e", "<yellow>").replace("&f", "<white>")
                .replace("&7", "<gray>").replace("&6", "<gold>").replace("&4", "<dark_red>")
                .replace("&2", "<dark_green>").replace("&1", "<dark_blue>").replace("&9", "<blue>")
                .replace("&5", "<dark_purple>").replace("&3", "<dark_aqua>").replace("&0", "<black>")
                .replace("&8", "<dark_gray>").replace("&l", "<bold>").replace("&o", "<italic>")
                .replace("&n", "<underlined>").replace("&m", "<strikethrough>")
                .replace("&k", "<obfuscated>").replace("&r", "<reset>");
        return MM.deserialize("<!italic>" + s);
    }

    public static String stripColor(String s) {
        if (s == null) return "";
        return s.replaceAll("&[0-9a-fk-orA-FK-OR]", "")
                .replaceAll("&#[0-9A-Fa-f]{6}", "")
                .replaceAll("#[0-9A-Fa-f]{6}", "")
                .trim();
    }
}
