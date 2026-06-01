package me.vennlmao.ariscore.home.utils;

import me.vennlmao.ariscore.home.HomeModule;
import org.bukkit.entity.Player;

import java.util.function.UnaryOperator;

public class MessageUtil {

    private static HomeModule plugin;

    public static void init(HomeModule pl) {
        plugin = pl;
    }

    public static void sendChat(Player player, String key) {
        sendChat(player, key, s -> s);
    }

    public static void sendChat(Player player, String key, UnaryOperator<String> replacer) {
        String msg = plugin.getConfig().getString("messages." + key, "");
        if (!msg.isEmpty()) player.sendMessage(ColorUtil.parse(replacer.apply(msg)));
    }

    public static void sendActionbar(Player player, String key) {
        sendActionbar(player, key, s -> s);
    }

    public static void sendActionbar(Player player, String key, UnaryOperator<String> replacer) {
        String msg = plugin.getConfig().getString("messages." + key, "");
        if (!msg.isEmpty()) player.sendActionBar(ColorUtil.parse(replacer.apply(msg)));
    }
}
