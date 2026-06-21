package me.vennlmao.ariscore.order.utils;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("(&?#[0-9a-fA-F]{6})");

    public static String color(String text) {
        if (text == null) return null;
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            if (hex.startsWith("&")) hex = hex.substring(1);
            try {
                ChatColor color = ChatColor.of(hex);
                matcher.appendReplacement(buffer, color.toString());
            } catch (Exception e) {
                matcher.appendReplacement(buffer, matcher.group());
            }
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public static String stripColors(String text) {
        if (text == null) return "";
        text = text.replaceAll("(&?#[0-9a-fA-F]{6})", "");
        text = text.replaceAll("(&[0-9a-fk-or])", "");
        return ChatColor.stripColor(text);
    }
}
