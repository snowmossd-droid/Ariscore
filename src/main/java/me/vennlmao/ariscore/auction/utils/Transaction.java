package me.vennlmao.ariscore.auction.utils;

import java.util.UUID;

public class Transaction {

    public enum Type { PURCHASE, SALE }

    private final UUID transactionId;
    private final UUID buyerUUID;
    private final String buyerName;
    private final UUID sellerUUID;
    private final String sellerName;
    private final UUID auctionId;
    private final String itemBase64;
    private final String itemName;
    private final int itemAmount;
    private final double price;
    private final double fee;
    private final long time;
    private final Type type;

    public Transaction(UUID transactionId, UUID buyerUUID, String buyerName, UUID sellerUUID,
                       String sellerName, UUID auctionId, String itemBase64, String itemName,
                       int itemAmount, double price, double fee, long time, Type type) {
        this.transactionId = transactionId;
        this.buyerUUID = buyerUUID;
        this.buyerName = buyerName;
        this.sellerUUID = sellerUUID;
        this.sellerName = sellerName;
        this.auctionId = auctionId;
        this.itemBase64 = itemBase64;
        this.itemName = itemName;
        this.itemAmount = itemAmount;
        this.price = price;
        this.fee = fee;
        this.time = time;
        this.type = type;
    }

    public UUID getTransactionId() { return transactionId; }
    public UUID getBuyerUUID() { return buyerUUID; }
    public String getBuyerName() { return buyerName; }
    public UUID getSellerUUID() { return sellerUUID; }
    public String getSellerName() { return sellerName; }
    public UUID getAuctionId() { return auctionId; }
    public String getItemBase64() { return itemBase64; }
    public String getItemName() { return itemName; }
    public int getItemAmount() { return itemAmount; }
    public double getPrice() { return price; }
    public double getFee() { return fee; }
    public long getTime() { return time; }
    public Type getType() { return type; }

    public String getTimeAgo() {
        long diff = System.currentTimeMillis() - time;
        long s = diff / 1000, m = s / 60, h = m / 60, d = h / 24;
        if (d > 0) return d + "d " + h % 24 + "h";
        if (h > 0) return h + "h " + m % 60 + "m";
        if (m > 0) return m + "m " + s % 60 + "s";
        return s + "s";
    }
}
