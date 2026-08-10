package me.vennlmao.ariscore.duel.utils;

import me.vennlmao.ariscore.duel.DuelModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.function.UnaryOperator;

public class MessageUtil {

    private static DuelModule module;

    public static void init(DuelModule m) { module = m; }

    public static void sendChat(CommandSender sender, String key) {
        sendChat(sender, key, s -> s);
    }

    public static void sendChat(CommandSender sender, String key, UnaryOperator<String> replacer) {
        String msg = module.getConfig().getString("messages." + key, "");
        if (!msg.isEmpty()) sender.sendMessage(ColorUtil.parse(replacer.apply(msg)));
    }

    public static void sendActionbar(Player player, String key) {
        sendActionbar(player, key, s -> s);
    }

    public static void sendActionbar(Player player, String key, UnaryOperator<String> replacer) {
        String msg = module.getConfig().getString("messages." + key, "");
        if (!msg.isEmpty()) player.sendActionBar(ColorUtil.parse(replacer.apply(msg)));
    }

    public static void sendBoth(CommandSender sender, String key) {
        sendBoth(sender, key, s -> s);
    }

    public static void sendBoth(CommandSender sender, String key, UnaryOperator<String> replacer) {
        sendChat(sender, key, replacer);
        if (sender instanceof Player player) {
            sendActionbar(player, key + "_ab", replacer);
        }
    }

    public static void sendTitle(Player player, String titleKey, String subtitleKey) {
        sendTitle(player, titleKey, subtitleKey, s -> s);
    }

    public static void sendTitle(Player player, String titleKey, String subtitleKey, UnaryOperator<String> replacer) {
        String titleRaw = module.getConfig().getString("titles." + titleKey, "");
        String subtitleRaw = module.getConfig().getString("titles." + subtitleKey, "");
        if (titleRaw.isEmpty() && subtitleRaw.isEmpty()) return;
        Component title = ColorUtil.parse(replacer.apply(titleRaw));
        Component subtitle = ColorUtil.parse(replacer.apply(subtitleRaw));
        int fadeIn = module.getConfig().getInt("title-defaults.fade-in", 5);
        int stay = module.getConfig().getInt("title-defaults.stay", 40);
        int fadeOut = module.getConfig().getInt("title-defaults.fade-out", 10);
        Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeIn * 50L),
                Duration.ofMillis(stay * 50L),
                Duration.ofMillis(fadeOut * 50L));
        player.showTitle(Title.title(title, subtitle, times));
    }
}
