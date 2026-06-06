package me.vennlmao.ariscore.team.utils;

import me.vennlmao.ariscore.team.TeamModule;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundUtil {

    private static TeamModule module;

    public static void init(TeamModule m) { module = m; }

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
