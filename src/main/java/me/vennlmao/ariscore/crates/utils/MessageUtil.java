package me.vennlmao.ariscore.crates.utils;

import me.vennlmao.ariscore.crates.CratesModule;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

public class MessageUtil {

    private final CratesModule module;

    public MessageUtil(CratesModule module) {
        this.module = module;
    }

    public void send(CommandSender sender, String key) {
        sender.sendMessage(get(key));
    }

    public void send(CommandSender sender, String key, String... replacements) {
        sender.sendMessage(get(key, replacements));
    }

    public void sendActionBar(Player player, String key, String... replacements) {
        String text = get(key, replacements);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(text));
    }

    public void sendTitle(Player player, String titleKey, String subtitleKey, String... replacements) {
        FileConfiguration cfg = module.getConfig();
        String title = get(titleKey, replacements);
        String subtitle = get(subtitleKey, replacements);
        int fadeIn = cfg.getInt("title-defaults.fade-in", 10);
        int stay = cfg.getInt("title-defaults.stay", 40);
        int fadeOut = cfg.getInt("title-defaults.fade-out", 10);
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    public void playSound(Player player, String key) {
        FileConfiguration cfg = module.getConfig();
        String soundName = cfg.getString("sounds." + key + ".sound", "");
        if (soundName.isEmpty()) return;
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            float volume = (float) cfg.getDouble("sounds." + key + ".volume", 1.0);
            float pitch = (float) cfg.getDouble("sounds." + key + ".pitch", 1.0);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            module.getPlugin().getLogger().warning("[Crates] Unknown sound in config 'sounds." + key + "': " + soundName);
        }
    }

    public String get(String key, String... replacements) {
        String raw = module.getConfig().getString("messages." + key, "");
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace(replacements[i], replacements[i + 1]);
        }
        return ColorUtil.translate(raw);
    }

    public List<String> getList(String key) {
        return ColorUtil.translate(module.getConfig().getStringList("messages." + key));
    }
}
