package me.vennlmao.ariscore.auction.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.auction.utils.AuctionItem;
import me.vennlmao.ariscore.auction.utils.ItemSerializer;
import me.vennlmao.ariscore.auction.utils.PendingPayment;
import me.vennlmao.ariscore.auction.utils.Transaction;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuctionDatabaseManager implements AutoCloseable {

    private final ArisCore plugin;
    private HikariDataSource dataSource;
    private boolean mysql;
    private String prefix;

    public AuctionDatabaseManager(ArisCore plugin) {
        this.plugin = plugin;
    }

    public void init() {
        FileConfiguration cfg = plugin.getAuctionModule().getConfigManager().getConfig();
        mysql = cfg.getBoolean("mysql.enabled", false);
        prefix = cfg.getString("mysql.table-prefix", "ariscore_");

        HikariConfig hikari = new HikariConfig();
        if (mysql) {
            String host     = cfg.getString("mysql.host", "localhost");
            int    port     = cfg.getInt("mysql.port", 3306);
            String database = cfg.getString("mysql.database", "ariscore");
            String username = cfg.getString("mysql.username", "root");
            String password = cfg.getString("mysql.password", "");
            boolean ssl     = cfg.getBoolean("mysql.use-ssl", false);
            hikari.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=" + ssl + "&autoReconnect=true&characterEncoding=utf8");
            hikari.setUsername(username);
            hikari.setPassword(password);
            hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            File dbFile = new File(plugin.getDataFolder(), "auction/auction.db");
            dbFile.getParentFile().mkdirs();
            hikari.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            hikari.setDriverClassName("org.sqlite.JDBC");
        }

        hikari.setMaximumPoolSize(10);
        hikari.setMinimumIdle(2);
        hikari.setPoolName("ArisAuction-Pool");
        hikari.setConnectionTestQuery("SELECT 1");
        hikari.addDataSourceProperty("cachePrepStmts", "true");
        hikari.addDataSourceProperty("prepStmtCacheSize", "250");
        hikari.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        dataSource = new HikariDataSource(hikari);
        createTables();
    }

    private void createTables() {
        String autoinc = mysql ? "INT AUTO_INCREMENT PRIMARY KEY" : "INTEGER PRIMARY KEY AUTOINCREMENT";
        String uuidType = mysql ? "VARCHAR(36)" : "TEXT";
        String textType = mysql ? "VARCHAR(512)" : "TEXT";
        String dupKey   = mysql ? " ON DUPLICATE KEY UPDATE item=VALUES(item),price=VALUES(price),create_time=VALUES(create_time),expire_time=VALUES(expire_time),seller_name=VALUES(seller_name)" : "";

        String auctions = "CREATE TABLE IF NOT EXISTS " + prefix + "auction_listings ("
                + "id " + autoinc + ","
                + "auction_id " + uuidType + " NOT NULL UNIQUE,"
                + "seller_uuid " + uuidType + " NOT NULL,"
                + "seller_name " + textType + " NOT NULL,"
                + "item " + textType + " NOT NULL,"
                + "price DOUBLE NOT NULL,"
                + "create_time BIGINT NOT NULL,"
                + "expire_time BIGINT NOT NULL)";

        String pending = "CREATE TABLE IF NOT EXISTS " + prefix + "auction_pending ("
                + "id " + autoinc + ","
                + "seller_uuid " + uuidType + " NOT NULL,"
                + "seller_name " + textType + " NOT NULL,"
                + "amount DOUBLE NOT NULL,"
                + "item_name " + textType + " NOT NULL,"
                + "item_amount INT NOT NULL,"
                + "timestamp BIGINT NOT NULL)";

        String transactions = "CREATE TABLE IF NOT EXISTS " + prefix + "auction_transactions ("
                + "id " + autoinc + ","
                + "transaction_id " + uuidType + " NOT NULL UNIQUE,"
                + "buyer_uuid " + uuidType + " NOT NULL,"
                + "buyer_name " + textType + " NOT NULL,"
                + "seller_uuid " + uuidType + " NOT NULL,"
                + "seller_name " + textType + " NOT NULL,"
                + "auction_id " + uuidType + " NOT NULL,"
                + "item " + textType + " NOT NULL,"
                + "item_name " + textType + " NOT NULL,"
                + "item_amount INT NOT NULL,"
                + "price DOUBLE NOT NULL,"
                + "fee DOUBLE NOT NULL,"
                + "time BIGINT NOT NULL,"
                + "type VARCHAR(16) NOT NULL)";

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(auctions);
            stmt.execute(pending);
            stmt.execute(transactions);
        } catch (SQLException e) {
            plugin.getLogger().severe("[Auction] Failed to create tables: " + e.getMessage());
        }
    }

    public void saveAuction(AuctionItem a) {
        String sql = mysql
                ? "INSERT INTO " + prefix + "auction_listings(auction_id,seller_uuid,seller_name,item,price,create_time,expire_time) VALUES(?,?,?,?,?,?,?)"
                  + " ON DUPLICATE KEY UPDATE item=VALUES(item),price=VALUES(price),expire_time=VALUES(expire_time),seller_name=VALUES(seller_name)"
                : "INSERT OR REPLACE INTO " + prefix + "auction_listings(auction_id,seller_uuid,seller_name,item,price,create_time,expire_time) VALUES(?,?,?,?,?,?,?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, a.getAuctionId().toString());
            ps.setString(2, a.getSellerUUID().toString());
            ps.setString(3, a.getSellerName());
            ps.setString(4, ItemSerializer.toBase64(a.getItemStack()));
            ps.setDouble(5, a.getPrice());
            ps.setLong(6, a.getCreateTime());
            ps.setLong(7, a.getExpireTime());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void saveAllAuctions(List<AuctionItem> auctions) {
        String sql = mysql
                ? "INSERT INTO " + prefix + "auction_listings(auction_id,seller_uuid,seller_name,item,price,create_time,expire_time) VALUES(?,?,?,?,?,?,?)"
                  + " ON DUPLICATE KEY UPDATE item=VALUES(item),price=VALUES(price),expire_time=VALUES(expire_time),seller_name=VALUES(seller_name)"
                : "INSERT OR REPLACE INTO " + prefix + "auction_listings(auction_id,seller_uuid,seller_name,item,price,create_time,expire_time) VALUES(?,?,?,?,?,?,?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            for (AuctionItem a : auctions) {
                ps.setString(1, a.getAuctionId().toString());
                ps.setString(2, a.getSellerUUID().toString());
                ps.setString(3, a.getSellerName());
                ps.setString(4, ItemSerializer.toBase64(a.getItemStack()));
                ps.setDouble(5, a.getPrice());
                ps.setLong(6, a.getCreateTime());
                ps.setLong(7, a.getExpireTime());
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void deleteAuction(UUID auctionId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM " + prefix + "auction_listings WHERE auction_id=?")) {
            ps.setString(1, auctionId.toString());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<AuctionItem> loadAuctions() {
        List<AuctionItem> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM " + prefix + "auction_listings");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                try {
                    UUID id     = UUID.fromString(rs.getString("auction_id"));
                    UUID seller = UUID.fromString(rs.getString("seller_uuid"));
                    String name = rs.getString("seller_name");
                    ItemStack item = ItemSerializer.fromBase64(rs.getString("item"));
                    if (item == null) continue;
                    double price  = rs.getDouble("price");
                    long create   = rs.getLong("create_time");
                    long expire   = rs.getLong("expire_time");
                    list.add(new AuctionItem(id, seller, name, item, price, create, expire));
                } catch (Exception e) { e.printStackTrace(); }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public void savePendingPayment(PendingPayment p) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO " + prefix + "auction_pending(seller_uuid,seller_name,amount,item_name,item_amount,timestamp) VALUES(?,?,?,?,?,?)")) {
            ps.setString(1, p.getSellerUUID().toString());
            ps.setString(2, p.getSellerName());
            ps.setDouble(3, p.getAmount());
            ps.setString(4, p.getItemName());
            ps.setInt(5, p.getItemAmount());
            ps.setLong(6, p.getTimestamp());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<PendingPayment> loadPendingPayments() {
        List<PendingPayment> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM " + prefix + "auction_pending");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                try {
                    UUID seller    = UUID.fromString(rs.getString("seller_uuid"));
                    String name    = rs.getString("seller_name");
                    double amount  = rs.getDouble("amount");
                    String iname   = rs.getString("item_name");
                    int iamount    = rs.getInt("item_amount");
                    list.add(new PendingPayment(seller, name, amount, iname, iamount));
                } catch (Exception e) { e.printStackTrace(); }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public void deletePendingPaymentsBySeller(UUID sellerUUID) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM " + prefix + "auction_pending WHERE seller_uuid=?")) {
            ps.setString(1, sellerUUID.toString());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void saveTransaction(Transaction t) {
        String sql = mysql
                ? "INSERT IGNORE INTO " + prefix + "auction_transactions(transaction_id,buyer_uuid,buyer_name,seller_uuid,seller_name,auction_id,item,item_name,item_amount,price,fee,time,type) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)"
                : "INSERT OR IGNORE INTO " + prefix + "auction_transactions(transaction_id,buyer_uuid,buyer_name,seller_uuid,seller_name,auction_id,item,item_name,item_amount,price,fee,time,type) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1,  t.getTransactionId().toString());
            ps.setString(2,  t.getBuyerUUID().toString());
            ps.setString(3,  t.getBuyerName());
            ps.setString(4,  t.getSellerUUID().toString());
            ps.setString(5,  t.getSellerName());
            ps.setString(6,  t.getAuctionId().toString());
            ps.setString(7,  t.getItemBase64());
            ps.setString(8,  t.getItemName());
            ps.setInt(9,     t.getItemAmount());
            ps.setDouble(10, t.getPrice());
            ps.setDouble(11, t.getFee());
            ps.setLong(12,   t.getTime());
            ps.setString(13, t.getType().name());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<Transaction> getPlayerTransactions(UUID uuid, int page, int perPage) {
        return queryTransactions("WHERE buyer_uuid=? OR seller_uuid=?", uuid, page, perPage, null);
    }

    public List<Transaction> searchPlayerTransactions(UUID uuid, String term, int page, int perPage) {
        return queryTransactions("WHERE (buyer_uuid=? OR seller_uuid=?) AND item_name LIKE ?", uuid, page, perPage, "%" + term + "%");
    }

    private List<Transaction> queryTransactions(String where, UUID uuid, int page, int perPage, String term) {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM " + prefix + "auction_transactions " + where + " ORDER BY time DESC LIMIT ? OFFSET ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, uuid.toString());
            int idx = 3;
            if (term != null) ps.setString(idx++, term);
            ps.setInt(idx++, perPage);
            ps.setInt(idx, (page - 1) * perPage);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Transaction t = rowToTransaction(rs);
                    if (t != null) list.add(t);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public int getPlayerTransactionCount(UUID uuid) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM " + prefix + "auction_transactions WHERE buyer_uuid=? OR seller_uuid=?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public double getTotalSpent(UUID uuid) {
        return sumColumn(uuid, "price", "buyer_uuid", "PURCHASE");
    }

    public double getTotalMade(UUID uuid) {
        return sumColumn(uuid, "price", "seller_uuid", "SALE");
    }

    private double sumColumn(UUID uuid, String col, String uuidCol, String type) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "SELECT SUM(" + col + ") FROM " + prefix + "auction_transactions WHERE " + uuidCol + "=? AND type=?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, type);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }

    private Transaction rowToTransaction(ResultSet rs) {
        try {
            return new Transaction(
                UUID.fromString(rs.getString("transaction_id")),
                UUID.fromString(rs.getString("buyer_uuid")),
                rs.getString("buyer_name"),
                UUID.fromString(rs.getString("seller_uuid")),
                rs.getString("seller_name"),
                UUID.fromString(rs.getString("auction_id")),
                rs.getString("item"),
                rs.getString("item_name"),
                rs.getInt("item_amount"),
                rs.getDouble("price"),
                rs.getDouble("fee"),
                rs.getLong("time"),
                Transaction.Type.valueOf(rs.getString("type"))
            );
        } catch (Exception e) { e.printStackTrace(); return null; }
    }

    public boolean isMysql() { return mysql; }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }
}
