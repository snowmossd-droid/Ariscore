package me.vennlmao.ariscore.auction.utils;
import me.vennlmao.ariscore.auction.AuctionModule;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
public class SoundUtil {
    private static AuctionModule module;
    public static void init(AuctionModule m) { module = m; }
    public static void play(Player p, String key) {
        String name = module.getConfig().getString("sounds." + key + ".sound","");
        float vol = (float) module.getConfig().getDouble("sounds." + key + ".volume", 1.0);
        float pitch = (float) module.getConfig().getDouble("sounds." + key + ".pitch", 1.0);
        if (name.isEmpty()) return;
        try { p.playSound(p.getLocation(), Sound.valueOf(name.toUpperCase()), vol, pitch); }
        catch (IllegalArgumentException e) { p.playSound(p.getLocation(), name, vol, pitch); }
    }
}
