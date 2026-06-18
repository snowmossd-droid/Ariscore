package me.vennlmao.ariscore.shards.utils;

import me.vennlmao.ariscore.shards.ShardsModule;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundUtil {

    private static ShardsModule module;

    public static void init(ShardsModule m) { module = m; }

    public static void play(Player player, String key) {
        String soundName = module.getConfig().getString("sounds." + key + ".sound", "");
        float volume = (float) module.getConfig().getDouble("sounds." + key + ".volume", 1.0);
        float pitch = (float) module.getConfig().getDouble("sounds." + key + ".pitch", 1.0);
        if (soundName.isEmpty()) return;
        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName), volume, pitch);
        } catch (IllegalArgumentException ignored) {}
    }
}
