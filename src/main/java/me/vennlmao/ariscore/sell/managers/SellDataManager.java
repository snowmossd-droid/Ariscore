package me.vennlmao.ariscore.sell.managers;

import me.vennlmao.ariscore.sell.SellModule;
import me.vennlmao.ariscore.sell.utils.ColorUtil;
import me.vennlmao.ariscore.sell.utils.PlayerStats;
import me.vennlmao.ariscore.sell.utils.ProgressUpdate;
import me.vennlmao.ariscore.sell.utils.SaleEntry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SellDataManager {

    private final SellModule module;
    private final SellDatabaseManager db;
    private final Map<UUID, List<SaleEntry>> historyCache = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Double>> multiplierCache = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, PlayerStats>> statsCache = new ConcurrentHashMap<>();
    private final Set<String> loadingMultiplierKeys = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> loadingStatsKeys = Collections.synchronizedSet(new HashSet<>());
    private final Set<UUID> loadingHistoryKeys = Collections.synchronizedSet(new HashSet<>());

    public SellDataManager(SellModule module, SellDatabaseManager db) {
        this.module = module;
        this.db = db;
    }

    public void updateProgress(UUID uuid, String category, double amount) {
        if (category == null || amount <= 0.0) return;
        Map<String, Double> map = new ConcurrentHashMap<>();
        map.put(category, amount);
        updateProgressBulk(uuid, map);
    }

    public void updateProgressBulk(UUID uuid, Map<String, Double> map) {
        if (map == null || map.isEmpty()) return;
        List<ProgressUpdate> updates = new ArrayList<>();

        for (Map.Entry<String, Double> entry : map.entrySet()) {
            String category = entry.getKey();
            double amount = entry.getValue() == null ? 0.0 : entry.getValue();
            if (category == null || amount <= 0.0) continue;

            PlayerStats stats = getStats(uuid, category);
            double newProgress = stats.progress() + amount;
            int level = stats.level();
            boolean levelUp = false;

            double needed;
            int nextLevel = level + 1;
            while ((needed = module.getGuiManager().getGuiConfig("sellmulti").getDouble("multipliers.levels." + nextLevel + ".amountNeeded", -1.0)) != -1.0 && newProgress >= needed) {
                level = nextLevel;
                levelUp = true;
                nextLevel++;
            }

            statsCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(category, new PlayerStats(newProgress, level));

            double multiplier = module.getGuiManager().getGuiConfig("sellmulti").getDouble("multipliers.levels." + level + ".multi", 1.0);
            if (level == 0) multiplier = getBaseMultiplier();
            multiplierCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(category, multiplier);

            updates.add(new ProgressUpdate(category, newProgress, level, levelUp, multiplier));
        }

        if (updates.isEmpty()) return;

        Bukkit.getAsyncScheduler().runNow((Plugin) module.getPlugin(), task -> {
            String prefix = db.getTablePrefix();
            String sql = db.isMysql()
                    ? "INSERT INTO " + prefix + "sell_multipliers(uuid, category, progress, level) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE progress = ?, level = ?"
                    : "INSERT OR REPLACE INTO " + prefix + "sell_multipliers(uuid, category, progress, level) VALUES(?,?,?,?)";
            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                conn.setAutoCommit(false);
                for (ProgressUpdate update : updates) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, update.category());
                    ps.setDouble(3, update.progress());
                    ps.setInt(4, update.level());
                    if (db.isMysql()) {
                        ps.setDouble(5, update.progress());
                        ps.setInt(6, update.level());
                    }
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            for (ProgressUpdate update : updates) {
                if (update.levelUp()) {
                    Bukkit.getGlobalRegionScheduler().run((Plugin) module.getPlugin(), t -> handleLevelUp(uuid, update));
                }
            }
        });
    }

    private double getBaseMultiplier() {
        return module.getGuiManager().getGuiConfig("sellmulti").getDouble("multipliers.base-multiplier", 1.0);
    }

    private String multiplierKey(UUID uuid, String category) {
        return uuid + "|" + category;
    }

    private void warmMultiplierAsync(UUID uuid, String category) {
        String key = multiplierKey(uuid, category);
        if (!loadingMultiplierKeys.add(key)) return;
        Bukkit.getAsyncScheduler().runNow((Plugin) module.getPlugin(), task -> {
            try {
                getMultiplier(uuid, category);
            } finally {
                loadingMultiplierKeys.remove(key);
            }
        });
    }

    private void warmStatsAsync(UUID uuid, String category) {
        String key = multiplierKey(uuid, category);
        if (!loadingStatsKeys.add(key)) return;
        Bukkit.getAsyncScheduler().runNow((Plugin) module.getPlugin(), task -> {
            try {
                getStats(uuid, category);
            } finally {
                loadingStatsKeys.remove(key);
            }
        });
    }

    private void warmHistoryAsync(UUID uuid) {
        if (!loadingHistoryKeys.add(uuid)) return;
        Bukkit.getAsyncScheduler().runNow((Plugin) module.getPlugin(), task -> {
            try {
                historyCache.put(uuid, loadHistoryFromDb(uuid));
            } finally {
                loadingHistoryKeys.remove(uuid);
            }
        });
    }

    public double getMultiplier(UUID uuid, String category) {
        if (category == null) return getBaseMultiplier();

        Map<String, Double> playerMultipliers = multiplierCache.get(uuid);
        if (playerMultipliers != null && playerMultipliers.containsKey(category)) {
            Double mult = playerMultipliers.get(category);
            return mult != null ? mult : getBaseMultiplier();
        }

        if (Bukkit.isPrimaryThread()) {
            warmMultiplierAsync(uuid, category);
            return getBaseMultiplier();
        }

        PlayerStats stats = getStats(uuid, category);
        double multiplier = module.getGuiManager().getGuiConfig("sellmulti").getDouble("multipliers.levels." + stats.level() + ".multi", 1.0);
        if (stats.level() == 0) multiplier = getBaseMultiplier();
        multiplierCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(category, multiplier);
        return multiplier;
    }

    public PlayerStats getStats(UUID uuid, String category) {
        if (category == null) return new PlayerStats(0.0, 0);

        Map<String, PlayerStats> map = statsCache.get(uuid);
        if (map != null && map.containsKey(category)) return map.get(category);

        if (Bukkit.isPrimaryThread()) {
            warmStatsAsync(uuid, category);
            return new PlayerStats(0.0, 0);
        }

        String prefix = db.getTablePrefix();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT progress, level FROM " + prefix + "sell_multipliers WHERE uuid = ? AND category = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, category);
            try (ResultSet rs = ps.executeQuery()) {
                PlayerStats stats = new PlayerStats(0.0, 0);
                if (rs.next()) stats = new PlayerStats(rs.getDouble("progress"), rs.getInt("level"));
                statsCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(category, stats);
                return stats;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return new PlayerStats(0.0, 0);
        }
    }

    public void clearCache(UUID uuid) {
        historyCache.remove(uuid);
        multiplierCache.remove(uuid);
        statsCache.remove(uuid);
    }

    public void preloadData(UUID uuid) {
        String prefix = db.getTablePrefix();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT category, progress, level FROM " + prefix + "sell_multipliers WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String category = rs.getString("category");
                    double progress = rs.getDouble("progress");
                    int level = rs.getInt("level");
                    statsCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(category, new PlayerStats(progress, level));

                    double multiplier = module.getGuiManager().getGuiConfig("sellmulti").getDouble("multipliers.levels." + level + ".multi", 1.0);
                    if (level == 0) multiplier = getBaseMultiplier();
                    multiplierCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(category, multiplier);
                }
            }
            historyCache.put(uuid, loadHistoryFromDb(uuid));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addSales(UUID uuid, List<SaleEntry> sales) {
        if (sales.isEmpty()) return;

        List<SaleEntry> cached = historyCache.get(uuid);
        if (cached != null) cached.addAll(sales);

        Bukkit.getAsyncScheduler().runNow((Plugin) module.getPlugin(), task -> {
            String prefix = db.getTablePrefix();
            try (Connection conn = db.getConnection();
                 PreparedStatement ps = conn.prepareStatement("INSERT INTO " + prefix + "sell_sales(uuid, item_name, quantity, price, timestamp) VALUES(?,?,?,?,?)")) {
                conn.setAutoCommit(false);
                for (SaleEntry sale : sales) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, sale.itemName());
                    ps.setInt(3, sale.quantity());
                    ps.setDouble(4, sale.price());
                    ps.setLong(5, sale.timestamp());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void addSale(UUID uuid, String itemName, int quantity, double price) {
        addSales(uuid, Collections.singletonList(new SaleEntry(itemName, quantity, price, System.currentTimeMillis())));
    }

    public List<SaleEntry> getHistory(UUID uuid) {
        List<SaleEntry> cached = historyCache.get(uuid);
        if (cached != null) return cached;

        if (Bukkit.isPrimaryThread()) {
            warmHistoryAsync(uuid);
            return new ArrayList<>();
        }

        List<SaleEntry> loaded = loadHistoryFromDb(uuid);
        historyCache.put(uuid, loaded);
        return loaded;
    }

    private List<SaleEntry> loadHistoryFromDb(UUID uuid) {
        List<SaleEntry> list = new ArrayList<>();
        String prefix = db.getTablePrefix();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT item_name, quantity, price, timestamp FROM " + prefix + "sell_sales WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new SaleEntry(rs.getString("item_name"), rs.getInt("quantity"), rs.getDouble("price"), rs.getLong("timestamp")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private void handleLevelUp(UUID uuid, ProgressUpdate update) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) return;
        String message = module.getConfig().getString("level-up", "");
        message = ColorUtil.colorize(message
                .replace("%category%", update.category().replace("_", " ").toUpperCase())
                .replace("%multiplier%", String.format("%.1f", update.multiplier()))
                .replace("%level%", String.valueOf(update.level())));
        player.sendMessage(message);
    }
}
