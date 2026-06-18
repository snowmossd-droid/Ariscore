package me.vennlmao.ariscore.sell.utils;

import me.vennlmao.ariscore.sell.SellModule;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundUtil {

    private static SellModule module;

    public static void init(SellModule m) { module = m; }

    public static void play(Player player, String key) {
        if (!module.getConfig().getBoolean(key + ".enabled", true)) return;
        String soundName = module.getConfig().getString(key + ".sound", "");
        float volume = (float) module.getConfig().getDouble(key + ".volume", 1.0);
        float pitch = (float) module.getConfig().getDouble(key + ".pitch", 1.0);
        if (soundName.isEmpty()) return;
        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName), volume, pitch);
        } catch (IllegalArgumentException ignored) {}
    }
}
