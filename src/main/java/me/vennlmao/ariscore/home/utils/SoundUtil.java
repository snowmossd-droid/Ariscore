package me.vennlmao.ariscore.home.utils;

import me.vennlmao.ariscore.home.HomeModule;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundUtil {

    private static HomeModule plugin;

    public static void init(HomeModule pl) {
        plugin = pl;
    }

    public static void play(Player player, String key) {
        if (player == null || !player.isOnline()) return;
        String soundName = plugin.getConfig().getString("sounds." + key + ".sound", "");
        float volume = (float) plugin.getConfig().getDouble("sounds." + key + ".volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble("sounds." + key + ".pitch", 1.0);
        if (soundName == null || soundName.isEmpty()) return;
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase().replace(".", "_").replace("-", "_"));
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            try {
                player.playSound(player.getLocation(), soundName.toLowerCase().replace("_", "."), volume, pitch);
            } catch (Exception ignored) {}
        }
    }
}
