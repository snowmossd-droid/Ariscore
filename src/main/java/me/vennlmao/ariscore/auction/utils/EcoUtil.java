package me.vennlmao.ariscore.auction.utils;

import me.vennlmao.ariscore.auction.managers.AuctionConfigManager;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

public class EcoUtil {

    private EcoUtil() {}

    public static Double parsePrice(String input, AuctionConfigManager cfg) {
        if (input == null) return null;
        String raw = input.trim().toLowerCase(Locale.ROOT).replace(" ", "").replace(",", "");
        if (raw.isEmpty()) return null;
        List<String> formats = cfg.getAbbreviationFormats();
        try {
            if (cfg.isAbbreviationsEnabled() && formats != null && !formats.isEmpty()) {
                for (int i = formats.size() - 1; i >= 0; i--) {
                    String suffix = formats.get(i).toLowerCase(Locale.ROOT);
                    if (!suffix.isEmpty() && raw.endsWith(suffix)) {
                        String num = raw.substring(0, raw.length() - suffix.length());
                        if (num.isEmpty()) return null;
                        return Double.parseDouble(num) * Math.pow(1000.0, i + 1);
                    }
                }
            }
            return Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static String format(double amount, boolean abbreviate, AuctionConfigManager cfg) {
        List<String> formats = cfg.getAbbreviationFormats();
        if (abbreviate && cfg.isAbbreviationsEnabled() && formats != null && !formats.isEmpty()) {
            double abs = Math.abs(amount);
            for (int i = formats.size() - 1; i >= 0; i--) {
                double divisor = Math.pow(1000.0, i + 1);
                if (abs >= divisor) {
                    DecimalFormat df = buildFormat(cfg.getCurrencyFormat());
                    return df.format(amount / divisor) + formats.get(i);
                }
            }
        }
        return buildFormat(cfg.getCurrencyFormat()).format(amount);
    }

    private static DecimalFormat buildFormat(String pattern) {
        DecimalFormatSymbols sym = DecimalFormatSymbols.getInstance(Locale.ROOT);
        DecimalFormat df = new DecimalFormat(pattern, sym);
        df.setMaximumFractionDigits(2);
        return df;
    }
}
