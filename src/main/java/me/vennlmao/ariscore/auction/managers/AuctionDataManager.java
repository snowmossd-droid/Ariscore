package me.vennlmao.ariscore.auction.managers;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.auction.utils.AuctionItem;
import me.vennlmao.ariscore.auction.utils.ItemSerializer;
import me.vennlmao.ariscore.auction.utils.PendingPayment;
import me.vennlmao.ariscore.auction.utils.Transaction;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AuctionDataManager {

    private final File dataFile;
    private FileConfiguration data;
    private final File transactionFile;
    private FileConfiguration transactions;

    public AuctionDataManager(ArisCore plugin) {
        File folder = new File(plugin.getDataFolder(), "auction");
        if (!folder.exists()) folder.mkdirs();

        dataFile = new File(folder, "auctions.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);

        transactionFile = new File(folder, "transactions.yml");
        if (!transactionFile.exists()) {
            try { transactionFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        transactions = YamlConfiguration.loadConfiguration(transactionFile);
    }

    public void saveAuctions(List<AuctionItem> active, Map<UUID, List<AuctionItem>> playerMap) {
        for (String key : data.getKeys(false)) data.set(key, null);
        int i = 0;
        for (AuctionItem a : active) {
            saveAuction("active." + i++, a);
        }
        i = 0;
        for (List<AuctionItem> list : playerMap.values()) {
            for (AuctionItem a : list) saveAuction("player." + i++, a);
        }
        try { data.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    private void saveAuction(String path, AuctionItem a) {
        data.set(path + ".id", a.getAuctionId().toString());
        data.set(path + ".seller", a.getSellerUUID().toString());
        data.set(path + ".sellerName", a.getSellerName());
        data.set(path + ".item", ItemSerializer.toBase64(a.getItemStack()));
        data.set(path + ".price", a.getPrice());
        data.set(path + ".createTime", a.getCreateTime());
        data.set(path + ".expireTime", a.getExpireTime());
    }

    public List<AuctionItem> loadAuctions() {
        List<AuctionItem> list = new ArrayList<>();
        List<UUID> seen = new ArrayList<>();
        loadAuctionsFromSection("active", list, seen);
        loadAuctionsFromSection("player", list, seen);
        return list;
    }

    private void loadAuctionsFromSection(String section, List<AuctionItem> list, List<UUID> seen) {
        if (!data.contains(section)) return;
        for (String key : data.getConfigurationSection(section).getKeys(false)) {
            String path = section + "." + key;
            try {
                UUID id = UUID.fromString(data.getString(path + ".id"));
                if (seen.contains(id)) continue;
                seen.add(id);
                UUID seller = UUID.fromString(data.getString(path + ".seller"));
                String sellerName = data.getString(path + ".sellerName");
                ItemStack item = ItemSerializer.fromBase64(data.getString(path + ".item"));
                if (item == null) continue;
                double price = data.getDouble(path + ".price");
                long create = data.getLong(path + ".createTime");
                long expire = data.getLong(path + ".expireTime");
                list.add(new AuctionItem(id, seller, sellerName, item, price, create, expire));
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public void savePendingPayments(List<PendingPayment> payments) {
        data.set("pendingPayments", null);
        int i = 0;
        for (PendingPayment p : payments) {
            String path = "pendingPayments." + i++;
            data.set(path + ".seller", p.getSellerUUID().toString());
            data.set(path + ".sellerName", p.getSellerName());
            data.set(path + ".amount", p.getAmount());
            data.set(path + ".itemName", p.getItemName());
            data.set(path + ".itemAmount", p.getItemAmount());
            data.set(path + ".timestamp", p.getTimestamp());
        }
        try { data.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public List<PendingPayment> loadPendingPayments() {
        List<PendingPayment> list = new ArrayList<>();
        if (!data.contains("pendingPayments")) return list;
        for (String key : data.getConfigurationSection("pendingPayments").getKeys(false)) {
            String path = "pendingPayments." + key;
            try {
                UUID seller = UUID.fromString(data.getString(path + ".seller"));
                String sellerName = data.getString(path + ".sellerName");
                double amount = data.getDouble(path + ".amount");
                String itemName = data.getString(path + ".itemName");
                int itemAmount = data.getInt(path + ".itemAmount");
                list.add(new PendingPayment(seller, sellerName, amount, itemName, itemAmount));
            } catch (Exception e) { e.printStackTrace(); }
        }
        return list;
    }

    public void saveTransaction(Transaction t) {
        String path = "transactions." + t.getTransactionId().toString();
        transactions.set(path + ".buyer", t.getBuyerUUID().toString());
        transactions.set(path + ".buyerName", t.getBuyerName());
        transactions.set(path + ".seller", t.getSellerUUID().toString());
        transactions.set(path + ".sellerName", t.getSellerName());
        transactions.set(path + ".auctionId", t.getAuctionId().toString());
        transactions.set(path + ".item", t.getItemBase64());
        transactions.set(path + ".itemName", t.getItemName());
        transactions.set(path + ".itemAmount", t.getItemAmount());
        transactions.set(path + ".price", t.getPrice());
        transactions.set(path + ".fee", t.getFee());
        transactions.set(path + ".time", t.getTime());
        transactions.set(path + ".type", t.getType().name());
        try { transactions.save(transactionFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public List<Transaction> getPlayerTransactions(UUID playerUUID, int page, int perPage) {
        List<Transaction> all = new ArrayList<>();
        if (!transactions.contains("transactions")) return all;
        for (String key : transactions.getConfigurationSection("transactions").getKeys(false)) {
            Transaction t = loadTransaction(key);
            if (t != null && (t.getBuyerUUID().equals(playerUUID) || t.getSellerUUID().equals(playerUUID))) {
                all.add(t);
            }
        }
        all.sort((a, b) -> Long.compare(b.getTime(), a.getTime()));
        int from = (page - 1) * perPage;
        int to = Math.min(from + perPage, all.size());
        return from >= all.size() ? new ArrayList<>() : all.subList(from, to);
    }

    public List<Transaction> searchPlayerTransactions(UUID playerUUID, String term, int page, int perPage) {
        List<Transaction> all = new ArrayList<>();
        if (!transactions.contains("transactions")) return all;
        String lower = term.toLowerCase();
        for (String key : transactions.getConfigurationSection("transactions").getKeys(false)) {
            Transaction t = loadTransaction(key);
            if (t != null && (t.getBuyerUUID().equals(playerUUID) || t.getSellerUUID().equals(playerUUID))) {
                if (t.getItemName().toLowerCase().contains(lower) ||
                    t.getBuyerName().toLowerCase().contains(lower) ||
                    t.getSellerName().toLowerCase().contains(lower)) {
                    all.add(t);
                }
            }
        }
        all.sort((a, b) -> Long.compare(b.getTime(), a.getTime()));
        int from = (page - 1) * perPage;
        int to = Math.min(from + perPage, all.size());
        return from >= all.size() ? new ArrayList<>() : all.subList(from, to);
    }

    public int getPlayerTransactionCount(UUID playerUUID) {
        if (!transactions.contains("transactions")) return 0;
        int count = 0;
        for (String key : transactions.getConfigurationSection("transactions").getKeys(false)) {
            Transaction t = loadTransaction(key);
            if (t != null && (t.getBuyerUUID().equals(playerUUID) || t.getSellerUUID().equals(playerUUID))) count++;
        }
        return count;
    }

    public double getTotalSpent(UUID playerUUID) {
        if (!transactions.contains("transactions")) return 0;
        double total = 0;
        for (String key : transactions.getConfigurationSection("transactions").getKeys(false)) {
            Transaction t = loadTransaction(key);
            if (t != null && t.getBuyerUUID().equals(playerUUID) && t.getType() == Transaction.Type.PURCHASE) {
                total += t.getPrice();
            }
        }
        return total;
    }

    public double getTotalMade(UUID playerUUID) {
        if (!transactions.contains("transactions")) return 0;
        double total = 0;
        for (String key : transactions.getConfigurationSection("transactions").getKeys(false)) {
            Transaction t = loadTransaction(key);
            if (t != null && t.getSellerUUID().equals(playerUUID) && t.getType() == Transaction.Type.SALE) {
                total += t.getPrice();
            }
        }
        return total;
    }

    private Transaction loadTransaction(String key) {
        String path = "transactions." + key;
        try {
            UUID id = UUID.fromString(key);
            UUID buyer = UUID.fromString(transactions.getString(path + ".buyer"));
            String buyerName = transactions.getString(path + ".buyerName");
            UUID seller = UUID.fromString(transactions.getString(path + ".seller"));
            String sellerName = transactions.getString(path + ".sellerName");
            UUID auctionId = UUID.fromString(transactions.getString(path + ".auctionId"));
            String item = transactions.getString(path + ".item");
            String itemName = transactions.getString(path + ".itemName");
            int itemAmount = transactions.getInt(path + ".itemAmount");
            double price = transactions.getDouble(path + ".price");
            double fee = transactions.getDouble(path + ".fee");
            long time = transactions.getLong(path + ".time");
            Transaction.Type type = Transaction.Type.valueOf(transactions.getString(path + ".type", "PURCHASE"));
            return new Transaction(id, buyer, buyerName, seller, sellerName, auctionId, item, itemName, itemAmount, price, fee, time, type);
        } catch (Exception e) {
            return null;
        }
    }

    public void closeConnection() {}
}
