package me.vennlmao.ariscore.auction.utils;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class AuctionItem {

    private final UUID auctionId;
    private final UUID sellerUUID;
    private final String sellerName;
    private final ItemStack itemStack;
    private final double price;
    private final long createTime;
    private final long expireTime;

    public AuctionItem(UUID auctionId, UUID sellerUUID, String sellerName, ItemStack itemStack,
                       double price, long createTime, long expireTime) {
        this.auctionId = auctionId;
        this.sellerUUID = sellerUUID;
        this.sellerName = sellerName;
        this.itemStack = itemStack;
        this.price = price;
        this.createTime = createTime;
        this.expireTime = expireTime;
    }

    public UUID getAuctionId() { return auctionId; }
    public UUID getSellerUUID() { return sellerUUID; }
    public String getSellerName() { return sellerName; }
    public ItemStack getItemStack() { return itemStack; }
    public double getPrice() { return price; }
    public long getCreateTime() { return createTime; }
    public long getExpireTime() { return expireTime; }

    public boolean isExpired() { return System.currentTimeMillis() > expireTime; }

    public String getTimeLeft() {
        long ms = expireTime - System.currentTimeMillis();
        if (ms <= 0) return "expired";
        long s = ms / 1000, m = s / 60, h = m / 60, d = h / 24;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("d ");
        if (h % 24 > 0) sb.append(h % 24).append("h ");
        if (m % 60 > 0) sb.append(m % 60).append("m ");
        sb.append(s % 60).append("s");
        return sb.toString().trim();
    }
}
