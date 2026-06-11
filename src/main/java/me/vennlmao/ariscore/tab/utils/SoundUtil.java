package me.vennlmao.ariscore.tab.utils;

import me.vennlmao.ariscore.tab.TabModule;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundUtil {

    private static TabModule module;

    public static void init(TabModule m) {
        module = m;
    }

    public static void play(Player player, String key) {
        String soundName = module.getConfig().getString("sounds." + key + ".sound", "");
        float volume = (float) module.getConfig().getDouble("sounds." + key + ".volume", 1.0);
        float pitch = (float) module.getConfig().getDouble("sounds." + key + ".pitch", 1.0);
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
