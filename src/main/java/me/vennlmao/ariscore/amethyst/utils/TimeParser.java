package me.vennlmao.ariscore.amethyst.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeParser {

    private static final Pattern PATTERN = Pattern.compile("(\\d+)([dhms])");

    public static long parseToMillis(String input) {
        if (input == null) return -1L;

        Matcher matcher = PATTERN.matcher(input.toLowerCase());
        long totalMillis = 0L;
        boolean matchedAny = false;

        while (matcher.find()) {
            matchedAny = true;
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);

            totalMillis += switch (unit) {
                case "d" -> value * 86400000L;
                case "h" -> value * 3600000L;
                case "m" -> value * 60000L;
                case "s" -> value * 1000L;
                default -> 0L;
            };
        }

        return matchedAny ? totalMillis : -1L;
    }

    public static String formatTimeLeft(long millisLeft) {
        if (millisLeft <= 0L) return "0s";

        long days = millisLeft / 86400000L;
        long hours = millisLeft / 3600000L % 24L;
        long minutes = millisLeft / 60000L % 60L;
        long seconds = millisLeft / 1000L % 60L;

        StringBuilder builder = new StringBuilder();
        if (days > 0L) builder.append(days).append("d ");
        if (days > 0L || hours > 0L) builder.append(hours).append("h ");
        if (days > 0L || hours > 0L || minutes > 0L) builder.append(minutes).append("m ");
        builder.append(seconds).append("s");

        return builder.toString();
    }
}
