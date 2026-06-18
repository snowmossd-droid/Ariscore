package me.vennlmao.ariscore.tab.managers;

import org.bukkit.entity.Player;

public class ConditionEvaluator {

    private final PapiManager papi;

    public ConditionEvaluator(PapiManager papi) {
        this.papi = papi;
    }

    public boolean evaluate(Player player, String condition) {
        if (condition == null || condition.isEmpty()) return true;
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

    private boolean compareStr(Player player, String condition, String op) {
        String[] parts = condition.split(op, 2);
        if (parts.length < 2) return true;
        String left  = papi.parse(player, parts[0].trim());
        String right = parts[1].trim();
        return op.equals("==") ? left.equals(right) : !left.equals(right);
    }

    private boolean compareNum(Player player, String condition, String op) {
        String[] parts = condition.split(op, 2);
        if (parts.length < 2) return true;
        String leftRaw = papi.parse(player, parts[0].trim());
        String rightRaw = parts[1].trim();
        try {
            double a = Double.parseDouble(leftRaw.replaceAll("[^0-9.\\-]", ""));
            double b = Double.parseDouble(rightRaw.replaceAll("[^0-9.\\-]", ""));
            switch (op) {
                case ">":  return a > b;
                case "<":  return a < b;
                case ">=": return a >= b;
                case "<=": return a <= b;
                default:   return false;
            }
        } catch (NumberFormatException e) {
            return compareStr(player, condition, op.equals(">=") || op.equals(">") ? "==" : "!=");
        }
    }

    private boolean compareSpecial(Player player, String condition, String op) {
        String[] parts = condition.split(op, 2);
        if (parts.length < 2) return true;
        String left  = papi.parse(player, parts[0].trim());
        String right = parts[1].trim();
        switch (op) {
            case "contains":   return left.contains(right);
            case "startsWith": return left.startsWith(right);
            case "endsWith":   return left.endsWith(right);
            default:           return false;
        }
    }
}
