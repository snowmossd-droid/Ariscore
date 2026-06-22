package me.vennlmao.ariscore.tab.managers;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PapiManager {

    private static final Pattern HEX_AMP         = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern HEX_ANGLE        = Pattern.compile("<#([A-Fa-f0-9]{6})>");
    private static final Pattern HEX_ANGLE_CLOSE  = Pattern.compile("</#([A-Fa-f0-9]{6})>");
    private static final Pattern HEX_GRADIENT     = Pattern.compile("<#([A-Fa-f0-9]{6})>(.+?)</#([A-Fa-f0-9]{6})>", Pattern.DOTALL);

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMP =
            LegacyComponentSerializer.builder().character('&').hexColors().build();

    private final boolean hasPapi;

    public PapiManager() {
        this.hasPapi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public String parse(Player player, String text) {
        if (text == null) return "";
        if (hasPapi) text = PlaceholderAPI.setPlaceholders(player, text);
        return colorize(text);
    }

    public String parseRaw(Player player, String text) {
        if (text == null) return "";
        if (hasPapi) text = PlaceholderAPI.setPlaceholders(player, text);
        return text;
    }

    public static String colorize(String text) {
        if (text == null) return "";
        text = applyGradients(text);
        text = convertAmpHex(text);
        text = convertAngleHex(text);
        text = HEX_ANGLE_CLOSE.matcher(text).replaceAll("");
        text = text.replace("&", "§").replace("§§", "&");
        return text;
    }

    private static String convertAmpHex(String text) {
        Matcher m = HEX_AMP.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(hexToLegacy(m.group(1))));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String convertAngleHex(String text) {
        Matcher m = HEX_ANGLE.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(hexToLegacy(m.group(1))));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String hexToLegacy(String hex) {
        StringBuilder sb = new StringBuilder("§x");
        for (char c : hex.toCharArray()) {
            sb.append('§').append(c);
        }
        return sb.toString();
    }

    private static String applyGradients(String text) {
        Matcher m = HEX_GRADIENT.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(gradient(m.group(2), m.group(1), m.group(3))));
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
            sb.append(hexToLegacy(String.format("%02X%02X%02X", r, g, b)));
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

    public static Component toComponent(String colorized) {
        return LegacyComponentSerializer.legacySection().deserialize(colorized);
    }
    }
