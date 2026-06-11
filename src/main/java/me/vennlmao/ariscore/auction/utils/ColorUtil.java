package me.vennlmao.ariscore.auction.utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
public class ColorUtil {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    public static Component parse(String raw) {
        if (raw == null) return Component.empty();
        String s = raw
            .replaceAll("#([0-9A-Fa-f]{6})", "<color:#$1>")
            .replace("&a","<green>").replace("&b","<aqua>").replace("&c","<red>")
            .replace("&d","<light_purple>").replace("&e","<yellow>").replace("&f","<white>")
            .replace("&7","<gray>").replace("&6","<gold>").replace("&4","<dark_red>")
            .replace("&2","<dark_green>").replace("&l","<bold>").replace("&o","<italic>")
            .replace("&n","<underlined>").replace("&m","<strikethrough>")
            .replace("&k","<obfuscated>").replace("&r","<reset>")
            .replace("&8","<dark_gray>");
        return MM.deserialize("<italic:false>" + s);
    }
    public static String strip(String s) {
        if (s == null) return "";
        return s.replaceAll("&[0-9a-fk-or]","").replaceAll("&#[0-9A-Fa-f]{6}","").replaceAll("#[0-9A-Fa-f]{6}","").replaceAll("<[^>]+>","").trim();
    }
}
