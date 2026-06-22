package me.vennlmao.ariscore.auction.utils;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

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
}
