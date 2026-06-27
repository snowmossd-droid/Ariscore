package me.vennlmao.ariscore.tab.managers;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.regex.Pattern;

public class ConditionEvaluator {

    private final PapiManager papi;
    private static final Pattern STRIP_COLOR = Pattern.compile(
            "(?i)(&|§)[0-9a-fk-or]|&#[0-9a-fA-F]{6}|§x(§[0-9a-fA-F]){6}|<#[0-9a-fA-F]{6}>|</#[0-9a-fA-F]{6}>");
    private static final boolean HAS_PAPI =
            Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;

    public ConditionEvaluator(PapiManager papi) {
        this.papi = papi;
    }

    public boolean evaluate(Player player, String condition) {
        if (condition == null || condition.trim().isEmpty()) return true;
        try {
            if (condition.contains(">="))         return compareNum(player, condition, ">=");
            if (condition.contains("<="))         return compareNum(player, condition, "<=");
            if (condition.contains("!="))         return compareStr(player, condition, "!=");
            if (condition.contains("=="))         return compareStr(player, condition, "==");
            if (condition.contains(">"))          return compareNum(player, condition, ">");
            if (condition.contains("<"))          return compareNum(player, condition, "<");
            if (condition.contains("startsWith")) return compareSpecial(player, condition, "startsWith");
            if (condition.contains("endsWith"))   return compareSpecial(player, condition, "endsWith");
            if (condition.contains("contains"))   return compareSpecial(player, condition, "contains");
        } catch (Exception ignored) {}
        return true;
    }

    private String resolve(Player player, String text) {
        String s = text.trim();
        if (HAS_PAPI) s = PlaceholderAPI.setPlaceholders(player, s);
        return STRIP_COLOR.matcher(s).replaceAll("").trim();
    }

    private boolean compareStr(Player player, String condition, String op) {
        String[] parts = condition.split(Pattern.quote(op), 2);
        if (parts.length < 2) return true;
        String left  = resolve(player, parts[0]);
        String right = parts[1].trim();
        return op.equals("==") ? left.equals(right) : !left.equals(right);
    }

    private boolean compareNum(Player player, String condition, String op) {
        String[] parts = condition.split(Pattern.quote(op), 2);
        if (parts.length < 2) return true;
        try {
            double a = Double.parseDouble(resolve(player, parts[0]).replaceAll("[^0-9.\\-]", ""));
            double b = Double.parseDouble(parts[1].trim().replaceAll("[^0-9.\\-]", ""));
            switch (op) {
                case ">":  return a > b;
                case "<":  return a < b;
                case ">=": return a >= b;
                case "<=": return a <= b;
            }
        } catch (NumberFormatException e) {
            return compareStr(player, condition, "==");
        }
        return false;
    }

    private boolean compareSpecial(Player player, String condition, String op) {
        String[] parts = condition.split(op, 2);
        if (parts.length < 2) return true;
        String left  = resolve(player, parts[0]);
        String right = parts[1].trim();
        switch (op) {
            case "contains":   return left.contains(right);
            case "startsWith": return left.startsWith(right);
            case "endsWith":   return left.endsWith(right);
        }
        return false;
    }
}
