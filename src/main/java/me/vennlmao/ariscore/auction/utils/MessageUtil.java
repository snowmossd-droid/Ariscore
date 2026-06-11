package me.vennlmao.ariscore.auction.utils;
import me.vennlmao.ariscore.auction.AuctionModule;
import org.bukkit.entity.Player;
import java.util.function.UnaryOperator;
public class MessageUtil {
    private static AuctionModule module;
    public static void init(AuctionModule m) { module = m; }
    public static void sendChat(Player p, String key) { sendChat(p, key, s -> s); }
    public static void sendChat(Player p, String key, UnaryOperator<String> r) {
        String msg = module.getConfig().getString("messages." + key, "");
        if (!msg.isEmpty()) p.sendMessage(ColorUtil.parse(r.apply(msg)));
    }
    public static void sendActionbar(Player p, String key) { sendActionbar(p, key, s -> s); }
    public static void sendActionbar(Player p, String key, UnaryOperator<String> r) {
        String msg = module.getConfig().getString("messages." + key + "_ab", "");
        if (!msg.isEmpty()) p.sendActionBar(ColorUtil.parse(r.apply(msg)));
    }
}
