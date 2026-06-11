package me.vennlmao.ariscore.auction.data;
import org.bukkit.inventory.ItemStack;
import java.util.UUID;
public class Transaction {
    public enum Type { SOLD, BOUGHT }
    private final UUID playerUuid;
    private final String otherName;
    private final ItemStack item;
    private final double amount;
    private final Type type;
    private final long time;
    public Transaction(UUID playerUuid, String otherName, ItemStack item, double amount, Type type, long time) {
        this.playerUuid = playerUuid; this.otherName = otherName;
        this.item = item.clone(); this.amount = amount; this.type = type; this.time = time;
    }
    public UUID getPlayerUuid() { return playerUuid; }
    public String getOtherName() { return otherName; }
    public ItemStack getItem() { return item.clone(); }
    public double getAmount() { return amount; }
    public Type getType() { return type; }
    public long getTime() { return time; }
    public String getTimeAgo() {
        long diff = System.currentTimeMillis() - time;
        long s = diff/1000; long m = s/60; long h = m/60; long d = h/24;
        if (d > 0) return d + "d ago";
        if (h > 0) return h + "h ago";
        if (m > 0) return m + "m ago";
        return s + "s ago";
    }
}
