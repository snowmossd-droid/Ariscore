package me.vennlmao.ariscore.sell.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.vennlmao.ariscore.sell.SellModule;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SellDatabaseManager {

    private final SellModule module;
    private HikariDataSource dataSource;
    private boolean mysql;

    public SellDatabaseManager(SellModule module) {
        this.module = module;
    }

    public void init() {
        mysql = module.getConfig().getBoolean("mysql.enabled", false);
        if (!mysql && !module.getConfig().getBoolean("sqlite.enabled", true)) mysql = false;
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
            File dbFile = new File(module.getPlugin().getDataFolder(), "sell/sell.db");
            dbFile.getParentFile().mkdirs();
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
        }

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setPoolName("ArisSell-Pool");
        config.setConnectionTestQuery("SELECT 1");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        dataSource = new HikariDataSource(config);
        createTables();
    }

    private void createTables() {
        String prefix = module.getConfig().getString("mysql.table-prefix", "ariscore_");
        String salesSql = mysql
                ? "CREATE TABLE IF NOT EXISTS " + prefix + "sell_sales (" +
                  "id INT AUTO_INCREMENT PRIMARY KEY," +
                  "uuid VARCHAR(36) NOT NULL," +
                  "item_name VARCHAR(64) NOT NULL," +
                  "quantity INT NOT NULL," +
                  "price DOUBLE NOT NULL," +
                  "timestamp BIGINT NOT NULL)"
                : "CREATE TABLE IF NOT EXISTS " + prefix + "sell_sales (" +
                  "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                  "uuid TEXT NOT NULL," +
                  "item_name TEXT NOT NULL," +
                  "quantity INTEGER NOT NULL," +
                  "price REAL NOT NULL," +
                  "timestamp INTEGER NOT NULL)";

        String multipliersSql = "CREATE TABLE IF NOT EXISTS " + prefix + "sell_multipliers (" +
                "uuid VARCHAR(36) NOT NULL," +
                "category VARCHAR(32) NOT NULL," +
                "progress DOUBLE DEFAULT 0," +
                "level INT DEFAULT 0," +
                "PRIMARY KEY (uuid, category))";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(salesSql);
            stmt.execute(multipliersSql);
        } catch (SQLException e) {
            module.getPlugin().getLogger().severe("[Sell] Failed to create tables: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public String getTablePrefix() {
        return module.getConfig().getString("mysql.table-prefix", "ariscore_");
    }

    public boolean isMysql() {
        return mysql;
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }
}
