package me.vennlmao.ariscore.auction.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.vennlmao.ariscore.auction.AuctionModule;
import me.vennlmao.ariscore.auction.data.AuctionListing;
import me.vennlmao.ariscore.auction.data.Transaction;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class AuctionDatabaseManager {

    private final AuctionModule module;
    private HikariDataSource dataSource;
    private boolean mysql;

    public AuctionDatabaseManager(AuctionModule module) {
        this.module = module;
    }

    public void init() {
        mysql = module.getConfig().getBoolean("mysql.enabled", false);
        HikariConfig config = new HikariConfig();

        if (mysql) {
            String host = module.getConfig().getString("mysql.host", "localhost");
            int port = module.getConfig().getInt("mysql.port", 3306);
            String database = module.getConfig().getString("mysql.database", "ariscore");
            String username = module.getConfig().getString("mysql.username", "root");
            String password = module.getConfig().getString("mysql.password", "");
            boolean ssl = module.getConfig().getBoolean("mysql.use-ssl", false);
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + ssl + "&autoReconnect=true");
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            File dbFile = new File(module.getPlugin().getDataFolder(), "auction/auction.db");
            dbFile.getParentFile().mkdirs();
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
        }

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("ArisAuction-Pool");

        dataSource = new HikariDataSource(config);
        createTables();
    }

    private void createTables() {
        String prefix = module.getConfig().getString("mysql.table-prefix", "ariscore_");
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS " + prefix + "auction_listings (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "seller_uuid VARCHAR(36) NOT NULL," +
                    "seller_name VARCHAR(64) NOT NULL," +
                    "item BLOB NOT NULL," +
                    "price DOUBLE NOT NULL," +
                    "listed_at BIGINT NOT NULL," +
                    "expires_at BIGINT NOT NULL" +
                    ")");
            stmt.execute("CREATE TABLE IF NOT EXISTS " + prefix + "auction_transactions (" +
                    "id INTEGER PRIMARY KEY " + (mysql ? "AUTO_INCREMENT" : "AUTOINCREMENT") + "," +
                    "player_uuid VARCHAR(36) NOT NULL," +
                    "other_name VARCHAR(64) NOT NULL," +
                    "item BLOB NOT NULL," +
                    "amount DOUBLE NOT NULL," +
                    "type VARCHAR(10) NOT NULL," +
                    "time BIGINT NOT NULL" +
                    ")");
        } catch (SQLException e) {
            module.getPlugin().getLogger().log(Level.SEVERE, "Failed to create auction tables", e);
        }
    }

    public void saveListings(List<AuctionListing> listings) {
        String prefix = module.getConfig().getString("mysql.table-prefix", "ariscore_");
        String table = prefix + "auction_listings";
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute("DELETE FROM " + table);
            String sql = "INSERT INTO " + table + " (id,seller_uuid,seller_name,item,price,listed_at,expires_at) VALUES (?,?,?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (AuctionListing l : listings) {
                    ps.setString(1, l.getId().toString());
                    ps.setString(2, l.getSellerUuid().toString());
                    ps.setString(3, l.getSellerName());
                    ps.setBytes(4, serializeItem(l.getItem()));
                    ps.setDouble(5, l.getPrice());
                    ps.setLong(6, l.getListedAt());
                    ps.setLong(7, l.getExpiresAt());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        } catch (SQLException e) {
            module.getPlugin().getLogger().log(Level.SEVERE, "Failed to save listings", e);
        }
    }

    public List<AuctionListing> loadListings() {
        String prefix = module.getConfig().getString("mysql.table-prefix", "ariscore_");
        List<AuctionListing> list = new ArrayList<>();
        String sql = "SELECT * FROM " + prefix + "auction_listings";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ItemStack item = deserializeItem(rs.getBytes("item"));
                if (item == null) continue;
                list.add(new AuctionListing(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("seller_uuid")),
                        rs.getString("seller_name"),
                        item,
                        rs.getDouble("price"),
                        rs.getLong("listed_at"),
                        rs.getLong("expires_at")
                ));
            }
        } catch (SQLException e) {
            module.getPlugin().getLogger().log(Level.SEVERE, "Failed to load listings", e);
        }
        return list;
    }

    public void saveTransaction(Transaction tx) {
        String prefix = module.getConfig().getString("mysql.table-prefix", "ariscore_");
        String sql = "INSERT INTO " + prefix + "auction_transactions (player_uuid,other_name,item,amount,type,time) VALUES (?,?,?,?,?,?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tx.getPlayerUuid().toString());
            ps.setString(2, tx.getOtherName());
            ps.setBytes(3, serializeItem(tx.getItem()));
            ps.setDouble(4, tx.getAmount());
            ps.setString(5, tx.getType().name());
            ps.setLong(6, tx.getTime());
            ps.executeUpdate();
        } catch (SQLException e) {
            module.getPlugin().getLogger().log(Level.SEVERE, "Failed to save transaction", e);
        }
    }

    public List<Transaction> loadTransactions(UUID uuid) {
        String prefix = module.getConfig().getString("mysql.table-prefix", "ariscore_");
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT * FROM " + prefix + "auction_transactions WHERE player_uuid = ? ORDER BY time ASC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ItemStack item = deserializeItem(rs.getBytes("item"));
                if (item == null) continue;
                list.add(new Transaction(
                        UUID.fromString(rs.getString("player_uuid")),
                        rs.getString("other_name"),
                        item,
                        rs.getDouble("amount"),
                        Transaction.Type.valueOf(rs.getString("type")),
                        rs.getLong("time")
                ));
            }
        } catch (SQLException e) {
            module.getPlugin().getLogger().log(Level.SEVERE, "Failed to load transactions", e);
        }
        return list;
    }

    private byte[] serializeItem(ItemStack item) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             BukkitObjectOutputStream oos = new BukkitObjectOutputStream(bos)) {
            oos.writeObject(item);
            return bos.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    private ItemStack deserializeItem(byte[] data) {
        if (data == null) return null;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             BukkitObjectInputStream ois = new BukkitObjectInputStream(bis)) {
            return (ItemStack) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }
              }
              
