package me.vennlmao.ariscore.sell.utils;

import java.text.DecimalFormat;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class FormatUtils {

    private static final String[] SUFFIXES = {"", "k", "m", "b", "t"};
    private static final Map<String, String> ITEM_NAME_CACHE = new ConcurrentHashMap<>();

    public static String formatPrice(double value) {
        if (value < 1000.0) return String.format("%.2f", value);
        int index = 0;
        double displayValue = value;
        while (displayValue >= 1000.0 && index < SUFFIXES.length - 1) {
            displayValue /= 1000.0;
            index++;
        }
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(displayValue) + SUFFIXES[index];
    }

    public static String formatItemName(String rawName) {
        if (rawName == null || rawName.isEmpty()) return "";
        return ITEM_NAME_CACHE.computeIfAbsent(rawName, FormatUtils::formatItemNameInternal);
    }

    private static String formatItemNameInternal(String rawName) {
        String[] parts = rawName.toLowerCase(Locale.ROOT).split("[_\\-\\s]+");
        StringBuilder sb = new StringBuilder(rawName.length() + 4);
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) sb.append(part, 1, part.length());
            }
        }
        return sb.toString();
    }
}
