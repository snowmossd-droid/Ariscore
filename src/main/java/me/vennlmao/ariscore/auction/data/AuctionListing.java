package me.vennlmao.ariscore.auction.data;
import org.bukkit.inventory.ItemStack;
import java.util.UUID;
public class AuctionListing {
    private final UUID id;
    private final UUID sellerUuid;
    private final String sellerName;
    private final ItemStack item;
    private final double price;
    private final long listedAt;
    private final long expiresAt;
    private boolean markedExpired;
    public AuctionListing(UUID id, UUID sellerUuid, String sellerName, ItemStack item, double price, long listedAt, long expiresAt) {
        this.id = id; this.sellerUuid = sellerUuid; this.sellerName = sellerName;
        this.item = item.clone(); this.price = price; this.listedAt = listedAt; this.expiresAt = expiresAt;
    }
    public UUID getId() { return id; }
    public UUID getSellerUuid() { return sellerUuid; }
    public String getSellerName() { return sellerName; }
    public ItemStack getItem() { return item.clone(); }
    public double getPrice() { return price; }
    public long getListedAt() { return listedAt; }
    public long getExpiresAt() { return expiresAt; }
    public boolean isExpired() { return markedExpired || System.currentTimeMillis() > expiresAt; }
    public void markExpired() { this.markedExpired = true; }
    public String getTimeLeftFormatted() {
        long ms = Math.max(0, expiresAt - System.currentTimeMillis());
        long s = ms/1000; long m = s/60; long h = m/60; long d = h/24;
        if (d > 0) return d + "d " + (h%24) + "h";
        if (h > 0) return h + "h " + (m%60) + "m";
        if (m > 0) return m + "m " + (s%60) + "s";
        return s + "s";
    }
}
