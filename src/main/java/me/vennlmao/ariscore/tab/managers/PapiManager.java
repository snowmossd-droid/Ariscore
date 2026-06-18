package me.vennlmao.ariscore.tab.managers;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PapiManager {

    private static final Pattern HEX_GRADIENT = Pattern.compile("<#([A-Fa-f0-9]{6})>(.+?)</#([A-Fa-f0-9]{6})>", Pattern.DOTALL);
    private static final Pattern HEX_AMP      = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern HEX_ANGLE    = Pattern.compile("<#([A-Fa-f0-9]{6})>");
    private final boolean hasPapi;

    public PapiManager() {
        this.hasPapi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public String parse(Player player, String text) {
        if (text == null) return "";
        if (hasPapi) text = PlaceholderAPI.setPlaceholders(player, text);
        return colorize(text);
    }

    public static String colorize(String text) {
        if (text == null) return "";
        text = applyGradients(text);
        Matcher m = HEX_AMP.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            try { m.appendReplacement(sb, ChatColor.of("#" + m.group(1)).toString()); }
            catch (Exception e) { m.appendReplacement(sb, m.group(0)); }
        }
        m.appendTail(sb);
        text = sb.toString();
        Matcher m2 = HEX_ANGLE.matcher(text);
        StringBuffer sb2 = new StringBuffer();
        while (m2.find()) {
            try { m2.appendReplacement(sb2, ChatColor.of("#" + m2.group(1)).toString()); }
            catch (Exception e) { m2.appendReplacement(sb2, m2.group(0)); }
        }
        m2.appendTail(sb2);
        return ChatColor.translateAlternateColorCodes('&', sb2.toString());
    }

    private static String applyGradients(String text) {
        Matcher m = HEX_GRADIENT.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String start   = m.group(1);
            String content = m.group(2);
            String end     = m.group(3);
            m.appendReplacement(sb, Matcher.quoteReplacement(gradient(content, start, end)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String gradient(String text, String startHex, String endHex) {
        int[] s = hexToRgb(startHex);
        int[] e = hexToRgb(endHex);
        int len = text.length();
        if (len == 0) return "";
        StringBuilder sb = new StringBuilder(len * 14);
        for (int i = 0; i < len; i++) {
            float ratio = len > 1 ? (float) i / (len - 1) : 0f;
            int r = Math.round(s[0] + (e[0] - s[0]) * ratio);
            int g = Math.round(s[1] + (e[1] - s[1]) * ratio);
            int b = Math.round(s[2] + (e[2] - s[2]) * ratio);
            try { sb.append(ChatColor.of(String.format("#%02X%02X%02X", r, g, b))); }
            catch (Exception ignored) {}
            sb.append(text.charAt(i));
        }
        return sb.toString();
    }

    private static int[] hexToRgb(String hex) {
        return new int[]{
            Integer.parseInt(hex.substring(0, 2), 16),
            Integer.parseInt(hex.substring(2, 4), 16),
            Integer.parseInt(hex.substring(4, 6), 16)
        };
    }
}
