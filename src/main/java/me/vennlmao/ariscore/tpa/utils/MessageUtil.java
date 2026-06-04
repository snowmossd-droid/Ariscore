package me.vennlmao.ariscore.tpa.utils;

import me.vennlmao.ariscore.tpa.TpaModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.UnaryOperator;

public class MessageUtil {

    private static TpaModule plugin;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static void init(TpaModule pl) {
        plugin = pl;
    }

    private static Component parseRaw(String raw) {
        String s = raw;
        s = s.replaceAll("&#([0-9A-Fa-f]{6})", "<color:#$1>");
        s = s.replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>")
                .replace("&7", "<gray>")
                .replace("&6", "<gold>")
                .replace("&4", "<dark_red>")
                .replace("&2", "<dark_green>")
                .replace("&1", "<dark_blue>")
                .replace("&9", "<blue>")
                .replace("&5", "<dark_purple>")
                .replace("&3", "<dark_aqua>")
                .replace("&0", "<black>")
                .replace("&8", "<dark_gray>")
                .replace("&l", "<bold>")
                .replace("&o", "<italic>")
                .replace("&n", "<underlined>")
                .replace("&m", "<strikethrough>")
                .replace("&k", "<obfuscated>")
                .replace("&r", "<reset>");
        return MM.deserialize(s);
    }

    public static void sendChatList(Player player, String key) {
        sendChatList(player, key, s -> s);
    }

    public static void sendChatList(Player player, String key, UnaryOperator<String> replacer) {
        List<String> lines = plugin.getConfig().getStringList("messages." + key);
        for (String line : lines) {
            player.sendMessage(parseRaw(replacer.apply(line)));
        }
    }

    public static void sendActionbar(Player player, String key) {
        sendActionbar(player, key, s -> s);
    }

    public static void sendActionbar(Player player, String key, UnaryOperator<String> replacer) {
        String raw = plugin.getConfig().getString("messages." + key, "");
        player.sendActionBar(parseRaw(replacer.apply(raw)));
    }

    public static String getString(String key) {
        return plugin.getConfig().getString("messages." + key, "");
    }

    public static List<String> getList(String key) {
        return plugin.getConfig().getStringList("messages." + key);
    }
                     }
    
