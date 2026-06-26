package me.vennlmao.ariscore.order.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.vennlmao.ariscore.ArisCore;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class DataManager {

    private final ArisCore plugin;
    private HikariDataSource dataSource;
    private boolean mysql;
    private String prefix;

    public DataManager(ArisCore plugin) {
        this.plugin = plugin;
    }

    public void init() {
        FileConfiguration cfg = plugin.getOrderModule().getConfigManager().getConfig();
        mysql = cfg.getBoolean("mysql.enabled", false);
        prefix = cfg.getString("mysql.table-prefix", "ariscore_");

        HikariConfig hikari = new HikariConfig();
        if (mysql) {
            String host = cfg.getString("mysql.host", "localhost");
            int port = cfg.getInt("mysql.port", 3306);
            String database = cfg.getString("mysql.database", "ariscore");
            String username = cfg.getString("mysql.username", "root");
            String password = cfg.getString("mysql.password", "");
            boolean ssl = cfg.getBoolean("mysql.use-ssl", false);
            hikari.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + ssl + "&autoReconnect=true&characterEncoding=utf8");
            hikari.setUsername(username);
            hikari.setPassword(password);
            hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            File dbFile = new File(plugin.getDataFolder(), "order/orders.db");
            dbFile.getParentFile().mkdirs();
            hikari.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            hikari.setDriverClassName("org.sqlite.JDBC");
        }

        hikari.setMaximumPoolSize(10);
        hikari.setMinimumIdle(2);
        hikari.setPoolName("ArisOrder-Pool");
        hikari.setConnectionTestQuery("SELECT 1");
        dataSource = new HikariDataSource(hikari);
        createTables();
    }

    private void createTables() {
        String autoinc = mysql ? "INT AUTO_INCREMENT PRIMARY KEY" : "INTEGER PRIMARY KEY AUTOINCREMENT";
        String sql = "CREATE TABLE IF NOT EXISTS " + prefix + "orders ("
                + "id " + autoinc + ","
                + "order_uuid VARCHAR(36) NOT NULL UNIQUE,"
                + "item_id VARCHAR(128) NOT NULL,"
                + "item_type VARCHAR(64),"
                + "sub_type VARCHAR(128),"
                + "material VARCHAR(64) NOT NULL,"
                + "price_per_item DOUBLE NOT NULL,"
                + "requested_amount INT NOT NULL,"
                + "delivered_amount INT NOT NULL DEFAULT 0,"
                + "collected_amount INT NOT NULL DEFAULT 0,"
                + "paid_amount DOUBLE NOT NULL DEFAULT 0,"
                + "creator VARCHAR(36) NOT NULL,"
                + "creation_date BIGINT NOT NULL,"
                + "expire_date BIGINT NOT NULL,"
                + "deletion_date BIGINT NOT NULL,"
                + "delivery_time INT NOT NULL,"
                + "active BOOLEAN NOT NULL DEFAULT 1,"
                + "expired_notified BOOLEAN NOT NULL DEFAULT 0,"
                + "was_fully_delivered BOOLEAN NOT NULL DEFAULT 0,"
                + "commands TEXT,"
                + "description TEXT)";
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().severe("[Order] Failed to create tables: " + e.getMessage());
        }
    }

    public void saveOrder(OrderItem order) {
        String sql = mysql
                ? "INSERT INTO " + prefix + "orders (order_uuid,item_id,item_type,sub_type,material,price_per_item,requested_amount,delivered_amount,collected_amount,paid_amount,creator,creation_date,expire_date,deletion_date,delivery_time,active,expired_notified,was_fully_delivered,commands,description) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE item_type=VALUES(item_type),sub_type=VALUES(sub_type),requested_amount=VALUES(requested_amount),delivered_amount=VALUES(delivered_amount),collected_amount=VALUES(collected_amount),paid_amount=VALUES(paid_amount),expire_date=VALUES(expire_date),deletion_date=VALUES(deletion_date),active=VALUES(active),expired_notified=VALUES(expired_notified),was_fully_delivered=VALUES(was_fully_delivered),commands=VALUES(commands),description=VALUES(description)"
                : "INSERT OR REPLACE INTO " + prefix + "orders (order_uuid,item_id,item_type,sub_type,material,price_per_item,requested_amount,delivered_amount,collected_amount,paid_amount,creator,creation_date,expire_date,deletion_date,delivery_time,active,expired_notified,was_fully_delivered,commands,description) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, order.getOrderUuid().toString());
            ps.setString(2, order.getItemId());
            ps.setString(3, order.getItemType());
            ps.setString(4, order.getSubType());
            ps.setString(5, order.getMaterial().name());
            ps.setDouble(6, order.getPricePerItem());
            ps.setInt(7, order.getRequestedAmount());
            ps.setInt(8, order.getDeliveredAmount());
            ps.setInt(9, order.getCollectedAmount());
            ps.setDouble(10, order.getPaidAmount());
            ps.setString(11, order.getCreator().toString());
            ps.setLong(12, order.getCreationDate().getTime());
            ps.setLong(13, order.getExpireDate().getTime());
            ps.setLong(14, order.getDeletionDate().getTime());
            ps.setInt(15, order.getDeliveryTime());
            ps.setBoolean(16, order.isActive());
            ps.setBoolean(17, order.isExpiredNotified());
            ps.setBoolean(18, order.wasFullyDelivered());
            ps.setString(19, String.join("|", order.getCommands()));
            ps.setString(20, String.join("|", order.getDescription()));
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void deleteOrder(UUID orderUuid) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM " + prefix + "orders WHERE order_uuid=?")) {
            ps.setString(1, orderUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<OrderItem> loadAllOrders() {
        List<OrderItem> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM " + prefix + "orders");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                try {
                    OrderItem order = rowToOrder(rs);
                    if (order != null) list.add(order);
                } catch (Exception e) { e.printStackTrace(); }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    private OrderItem rowToOrder(ResultSet rs) throws SQLException {
        UUID orderUuid = UUID.fromString(rs.getString("order_uuid"));
        String itemId = rs.getString("item_id");
        String itemType = rs.getString("item_type");
        String subType = rs.getString("sub_type");
        Material material = Material.valueOf(rs.getString("material"));
        double pricePerItem = rs.getDouble("price_per_item");
        int requestedAmount = rs.getInt("requested_amount");
        UUID creator = UUID.fromString(rs.getString("creator"));

        String cmdsRaw = rs.getString("commands");
        List<String> commands = (cmdsRaw != null && !cmdsRaw.isEmpty()) ? Arrays.asList(cmdsRaw.split("\\|")) : new ArrayList<>();
        String descRaw = rs.getString("description");
        List<String> description = (descRaw != null && !descRaw.isEmpty()) ? Arrays.asList(descRaw.split("\\|")) : new ArrayList<>();

        OrderItem order = new OrderItem(itemId, description, pricePerItem, material, itemId, commands, creator, itemType, subType, requestedAmount);
        order.setOrderUuid(orderUuid);
        order.setDeliveredAmount(rs.getInt("delivered_amount"));
        order.setCollectedAmount(rs.getInt("collected_amount"));
        order.setPaidAmount(rs.getDouble("paid_amount"));
        order.setActive(rs.getBoolean("active"));
        order.setExpiredNotified(rs.getBoolean("expired_notified"));
        order.setExpireDate(new Date(rs.getLong("expire_date")));
        order.setDeletionDate(new Date(rs.getLong("deletion_date")));
        order.setDeliveryTime(rs.getInt("delivery_time"));
        return order;
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }
}
