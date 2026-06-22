package me.vennlmao.ariscore.tab.managers;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PapiManager {

    private static final Pattern HEX_AMP         = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern HEX_ANGLE        = Pattern.compile("<#([A-Fa-f0-9]{6})>");
    private static final Pattern HEX_ANGLE_CLOSE  = Pattern.compile("</#([A-Fa-f0-9]{6})>");
    private static final Pattern HEX_GRADIENT     = Pattern.compile(
            "<#([A-Fa-f0-9]{6})>(.*?)</#([A-Fa-f0-9]{6})>", Pattern.DOTALL);
    private static final Pattern LEGACY_SECTION   = Pattern.compile("§x(§[0-9A-Fa-f]){6}");

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

        // Bước 1: Xử lý gradient trước (bao gồm cả &l bên trong)
        text = applyGradients(text);

        // Bước 2: Convert &#RRGGBB → §x§R§R§G§G§B§B
        Matcher m1 = HEX_AMP.matcher(text);
        StringBuffer sb1 = new StringBuffer();
        while (m1.find()) m1.appendReplacement(sb1, Matcher.quoteReplacement(hexToLegacy(m1.group(1))));
        m1.appendTail(sb1);
        text = sb1.toString();

        // Bước 3: Convert <#RRGGBB> → §x§R§R§G§G§B§B
        Matcher m2 = HEX_ANGLE.matcher(text);
        StringBuffer sb2 = new StringBuffer();
        while (m2.find()) m2.appendReplacement(sb2, Matcher.quoteReplacement(hexToLegacy(m2.group(1))));
        m2.appendTail(sb2);
        text = sb2.toString();

        // Bước 4: Xóa thẻ đóng </#RRGGBB> còn sót
        text = HEX_ANGLE_CLOSE.matcher(text).replaceAll("");

        // Bước 5: Tạm thay §x...§? (hex đã convert) bằng placeholder để tránh bị &→§ đè lên
        // Convert & codes — chỉ convert & không phải § (§ đã đúng rồi)
        StringBuilder result = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&' && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                if ((next >= '0' && next <= '9')
                        || (next >= 'a' && next <= 'f')
                        || (next >= 'A' && next <= 'F')
                        || "klmnorKLMNOR".indexOf(next) >= 0) {
                    result.append('§').append(Character.toLowerCase(next));
                    i++;
                    continue;
                }
            }
            result.append(c);
        }

        return result.toString();
    }

    private static String hexToLegacy(String hex) {
        StringBuilder sb = new StringBuilder("§x");
        for (char c : hex.toCharArray()) sb.append('§').append(c);
        return sb.toString();
    }

    private static String applyGradients(String text) {
        Matcher m = HEX_GRADIENT.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String startHex = m.group(1);
            String content  = m.group(2);
            String endHex   = m.group(3);
            // Strip & formatting codes từ content để đếm ký tự thật
            String stripped = content.replaceAll("&[0-9a-fk-orA-FK-OR]", "");
            m.appendReplacement(sb, Matcher.quoteReplacement(
                    gradient(content, stripped, startHex, endHex)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String gradient(String original, String stripped, String startHex, String endHex) {
        int[] s = hexToRgb(startHex);
        int[] e = hexToRgb(endHex);
        int len = stripped.length();
        if (len == 0) return original;

        StringBuilder sb = new StringBuilder();
        int colorIndex = 0;
        for (int i = 0; i < original.length(); i++) {
            char c = original.charAt(i);
            // Giữ nguyên & format codes (sẽ được convert sau)
            if (c == '&' && i + 1 < original.length()) {
                char next = original.charAt(i + 1);
                if ((next >= '0' && next <= '9') || (next >= 'a' && next <= 'f')
                        || (next >= 'A' && next <= 'F') || "klmnorKLMNOR".indexOf(next) >= 0) {
                    sb.append(c).append(next);
                    i++;
                    continue;
                }
            }
            float ratio = len > 1 ? (float) colorIndex / (len - 1) : 0f;
            int r = Math.round(s[0] + (e[0] - s[0]) * ratio);
            int g = Math.round(s[1] + (e[1] - s[1]) * ratio);
            int b = Math.round(s[2] + (e[2] - s[2]) * ratio);
            sb.append(hexToLegacy(String.format("%02X%02X%02X", r, g, b)));
            sb.append(c);
            colorIndex++;
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
