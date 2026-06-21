package me.vennlmao.ariscore.order.managers;

import me.vennlmao.ariscore.ArisCore;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundManager {

    private final ArisCore plugin;

    public SoundManager(ArisCore plugin) {
        this.plugin = plugin;
    }

    public void play(Player player, String key) {
        if (player == null || !player.isOnline()) return;
        String soundName = plugin.getOrderModule().getConfigManager().getConfig().getString("sounds." + key + ".sound");
        if (soundName == null || soundName.isEmpty()) return;
        if (!plugin.getOrderModule().getConfigManager().getConfig().getBoolean("sounds." + key + ".enabled", true)) return;
        float volume = (float) plugin.getOrderModule().getConfigManager().getConfig().getDouble("sounds." + key + ".volume", 1.0);
        float pitch  = (float) plugin.getOrderModule().getConfigManager().getConfig().getDouble("sounds." + key + ".pitch", 1.0);
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase().replace(".", "_").replace("-", "_"));
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException ignored) {}
    }
}
