package me.vennlmao.ariscore.crates.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.UUID;

public class SkullUtil {

    public static ItemStack fromBase64(String base64) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) return skull;

        String url = extractUrl(base64);
        if (url != null) {
            PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
            PlayerTextures textures = profile.getTextures();
            try {
                textures.setSkin(new URL(url));
                profile.setTextures(textures);
                meta.setOwnerProfile(profile);
            } catch (MalformedURLException ignored) {
            }
        }

        skull.setItemMeta(meta);
        return skull;
    }

    private static String extractUrl(String base64) {
        try {
            String decoded = new String(Base64.getDecoder().decode(base64));
            int start = decoded.indexOf("\"url\":\"") + 7;
            if (start < 7) return null;
            int end = decoded.indexOf('"', start);
            if (end < 0) return null;
            return decoded.substring(start, end);
        } catch (Exception e) {
            return null;
        }
    }
}
