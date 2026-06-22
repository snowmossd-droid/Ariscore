package me.vennlmao.ariscore.auction.utils;

import java.util.UUID;

public class PendingPayment {

    private final UUID sellerUUID;
    private final String sellerName;
    private final double amount;
    private final String itemName;
    private final int itemAmount;
    private final long timestamp;

    public PendingPayment(UUID sellerUUID, String sellerName, double amount, String itemName, int itemAmount) {
        this.sellerUUID = sellerUUID;
        this.sellerName = sellerName;
        this.amount = amount;
        this.itemName = itemName;
        this.itemAmount = itemAmount;
        this.timestamp = System.currentTimeMillis();
    }

    public UUID getSellerUUID() { return sellerUUID; }
    public String getSellerName() { return sellerName; }
    public double getAmount() { return amount; }
    public String getItemName() { return itemName; }
    public int getItemAmount() { return itemAmount; }
    public long getTimestamp() { return timestamp; }
}
