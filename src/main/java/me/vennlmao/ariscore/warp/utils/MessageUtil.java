package me.vennlmao.ariscore.warp.utils;

import me.vennlmao.ariscore.warp.WarpModule;
import org.bukkit.entity.Player;

import java.util.function.UnaryOperator;

public class MessageUtil {

    private static WarpModule module;

    public static void init(WarpModule m) { module = m; }

    public static void sendChat(Player player, String key) { sendChat(player, key, s -> s); }

    public static void sendChat(Player player, String key, UnaryOperator<String> replacer) {
        String msg = module.getConfig().getString("messages." + key, "");
        if (!msg.isEmpty()) player.sendMessage(ColorUtil.parse(replacer.apply(msg)));
    }

    public static void sendActionbar(Player player, String key) { sendActionbar(player, key, s -> s); }

    public static void sendActionbar(Player player, String key, UnaryOperator<String> replacer) {
        String msg = module.getConfig().getString("messages." + key, "");
        if (!msg.isEmpty()) player.sendActionBar(ColorUtil.parse(replacer.apply(msg)));
    }
}
