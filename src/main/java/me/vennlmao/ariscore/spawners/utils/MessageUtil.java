package me.vennlmao.ariscore.spawners.utils;

import me.vennlmao.ariscore.spawners.SpawnersModule;
import org.bukkit.entity.Player;

import java.util.function.UnaryOperator;

public class MessageUtil {

    private static SpawnersModule module;

    public static void init(SpawnersModule pl) {
        module = pl;
    }

    public static void sendChat(Player player, String key) {
        sendChat(player, key, s -> s);
    }

    public static void sendChat(Player player, String key, UnaryOperator<String> replacer) {
        String msg = module.getConfig().getString("messages." + key, "");
        if (!msg.isEmpty()) player.sendMessage(ColorUtil.parse(replacer.apply(msg)));
    }

    public static void sendActionbar(Player player, String key) {
        sendActionbar(player, key, s -> s);
    }

    public static void sendActionbar(Player player, String key, UnaryOperator<String> replacer) {
        String msg = module.getConfig().getString("messages." + key, "");
        if (!msg.isEmpty()) player.sendActionBar(ColorUtil.parse(replacer.apply(msg)));
    }

    public static String raw(String key) {
        return module.getConfig().getString("messages." + key, "");
    }
}
