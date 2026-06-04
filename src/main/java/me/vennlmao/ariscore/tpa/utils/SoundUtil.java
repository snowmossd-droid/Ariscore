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
        String soundName = plugin.getConfig().getString("sounds." + key + ".sound", "");
        float volume = (float) plugin.getConfig().getDouble("sounds." + key + ".volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble("sounds." + key + ".pitch", 1.0);
        if (soundName.isEmpty()) return;
        Sound sound = resolveSound(soundName);
        if (sound == null) return;
        player.playSound(player, sound, volume, pitch);
    }

    private static Sound resolveSound(String name) {
        try {
            return Sound.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ignored) {}
        try {
            String key = name.toLowerCase().replace(" ", "_");
            if (!key.contains(":")) key = "minecraft:" + key;
            org.bukkit.NamespacedKey nk = org.bukkit.NamespacedKey.fromString(key);
            if (nk != null) return org.bukkit.Registry.SOUNDS.get(nk);
        } catch (Exception ignored) {}
        return null;
    }
    }
