package me.vennlmao.ariscore.order.utils;

import me.vennlmao.ariscore.order.managers.OrderConfigManager;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class EcoUtil {

    private static final String[] DEFAULT_SUFFIXES = {"K", "M", "B", "T"};

    private EcoUtil() {}

    public static Double parsePrice(String input, OrderConfigManager cfg) {
        if (input == null) return null;
        String raw = input.trim().toLowerCase(Locale.ROOT).replace(" ", "").replace(",", "");
        if (raw.isEmpty()) return null;
        List<String> formats = getFormats(cfg);
        try {
            for (int i = formats.size() - 1; i >= 0; i--) {
                String suffix = formats.get(i).toLowerCase(Locale.ROOT);
                if (!suffix.isEmpty() && raw.endsWith(suffix)) {
                    String num = raw.substring(0, raw.length() - suffix.length());
                    if (num.isEmpty()) return null;
                    return Double.parseDouble(num) * Math.pow(1000.0, i + 1);
                }
            }
            return Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static String format(double amount) {
        return format(amount, null);
    }

    public static String format(double amount, OrderConfigManager cfg) {
        List<String> formats = getFormats(cfg);
        double abs = Math.abs(amount);
        for (int i = formats.size() - 1; i >= 0; i--) {
            double divisor = Math.pow(1000.0, i + 1);
            if (abs >= divisor) {
                return buildFormat().format(amount / divisor) + formats.get(i);
            }
        }
        return buildFormat().format(amount);
    }

    private static List<String> getFormats(OrderConfigManager cfg) {
        if (cfg == null) return List.of(DEFAULT_SUFFIXES);
        if (!cfg.getConfig().getBoolean("economy.abbreviations.enabled", true)) return List.of();
        List<String> formats = cfg.getConfig().getStringList("economy.abbreviations.formats");
        return formats.isEmpty() ? List.of(DEFAULT_SUFFIXES) : formats;
    }

    private static DecimalFormat buildFormat() {
        DecimalFormatSymbols sym = DecimalFormatSymbols.getInstance(Locale.ROOT);
        DecimalFormat df = new DecimalFormat("#,##0.##", sym);
        df.setMaximumFractionDigits(2);
        return df;
    }
}
