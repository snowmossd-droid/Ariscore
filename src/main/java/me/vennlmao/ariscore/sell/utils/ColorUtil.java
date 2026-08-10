package me.vennlmao.ariscore.sell.utils;

import net.md_5.bungee.api.ChatColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&?#([0-9A-Fa-f]{6})");

    public static String colorize(String message) {
        if (message == null) return "";
        try {
            Matcher matcher = HEX_PATTERN.matcher(message);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                try {
                    ChatColor color = ChatColor.of("#" + matcher.group(1));
                    matcher.appendReplacement(buffer, color.toString());
                } catch (IllegalArgumentException e) {
                    matcher.appendReplacement(buffer, matcher.group(0));
                }
            }
            matcher.appendTail(buffer);
            return ChatColor.translateAlternateColorCodes('&', buffer.toString());
        } catch (Exception e) {
            return ChatColor.translateAlternateColorCodes('&', message);
        }
    }

    public static Component component(String message) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(colorize(message));
    }
}
