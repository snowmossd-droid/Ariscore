package me.vennlmao.ariscore.spawners.utils;

import me.vennlmao.ariscore.spawners.SpawnersModule;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundUtil {

    private static SpawnersModule module;

    public static void init(SpawnersModule pl) {
        module = pl;
    }

    public static void play(Player player, String key) {
        if (player == null || !player.isOnline()) return;
        String soundName = module.getConfig().getString("sounds." + key + ".sound", "");
        float volume = (float) module.getConfig().getDouble("sounds." + key + ".volume", 1.0);
        float pitch = (float) module.getConfig().getDouble("sounds." + key + ".pitch", 1.0);
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
