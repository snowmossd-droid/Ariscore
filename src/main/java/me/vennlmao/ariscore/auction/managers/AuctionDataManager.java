package me.vennlmao.ariscore.auction.managers;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.auction.utils.AuctionItem;
import me.vennlmao.ariscore.auction.utils.PendingPayment;
import me.vennlmao.ariscore.auction.utils.Transaction;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AuctionDataManager {

    private final AuctionDatabaseManager db;

    public AuctionDataManager(ArisCore plugin) {
        this.db = new AuctionDatabaseManager(plugin);
        this.db.init();
    }

    public void saveAuctions(List<AuctionItem> active, Map<UUID, List<AuctionItem>> playerMap) {
        db.saveAllAuctions(active);
    }

    public void saveAuction(AuctionItem a) {
        db.saveAuction(a);
    }

    public void deleteAuction(UUID auctionId) {
        db.deleteAuction(auctionId);
    }

    public List<AuctionItem> loadAuctions() {
        return db.loadAuctions();
    }

    public void savePendingPayments(List<PendingPayment> payments) {
        for (PendingPayment p : payments) db.savePendingPayment(p);
    }

    public List<PendingPayment> loadPendingPayments() {
        return db.loadPendingPayments();
    }

    public void deletePendingPaymentsBySeller(UUID sellerUUID) {
        db.deletePendingPaymentsBySeller(sellerUUID);
    }

    public void saveTransaction(Transaction t) {
        db.saveTransaction(t);
    }

    public List<Transaction> getPlayerTransactions(UUID playerUUID, int page, int perPage) {
        return db.getPlayerTransactions(playerUUID, page, perPage);
    }

    public List<Transaction> searchPlayerTransactions(UUID playerUUID, String term, int page, int perPage) {
        return db.searchPlayerTransactions(playerUUID, term, page, perPage);
    }

    public int getPlayerTransactionCount(UUID playerUUID) {
        return db.getPlayerTransactionCount(playerUUID);
    }

    public double getTotalSpent(UUID playerUUID) {
        return db.getTotalSpent(playerUUID);
    }

    public double getTotalMade(UUID playerUUID) {
        return db.getTotalMade(playerUUID);
    }

    public void closeConnection() {
        db.close();
    }
}
