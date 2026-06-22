package me.vennlmao.ariscore.order.managers;

import me.vennlmao.ariscore.ArisCore;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class OrderManager {

    private final ArisCore plugin;
    private Economy economy;
    private final Map<UUID, OrderItem> orders = new ConcurrentHashMap<>();
    private final DecimalFormat currencyFormat;

    public OrderManager(ArisCore plugin) {
        this.plugin = plugin;
        DecimalFormatSymbols sym = DecimalFormatSymbols.getInstance(Locale.ROOT);
        sym.setGroupingSeparator(',');
        sym.setDecimalSeparator('.');
        this.currencyFormat = new DecimalFormat("#,##0.##", sym);
        setupEconomy();
    }

    private void setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) return;
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) economy = rsp.getProvider();
    }

    public Economy getEconomy() { return economy; }

    public String formatCurrency(double amount) {
        return currencyFormat.format(amount);
    }

    public void addOrder(OrderItem order) {
        if (order.getOrderUuid() == null) order.setOrderUuid(UUID.randomUUID());
        orders.put(order.getOrderUuid(), order);
    }

    public void removeOrder(UUID orderUuid) {
        orders.remove(orderUuid);
    }

    public OrderItem getOrder(UUID orderUuid) {
        return orders.get(orderUuid);
    }

    public List<OrderItem> getAllOrders() {
        return Collections.unmodifiableList(new ArrayList<>(orders.values()));
    }

    public List<OrderItem> getOrdersByPlayer(UUID playerUuid) {
        return orders.values().stream()
                .filter(o -> playerUuid.equals(o.getCreator()))
                .collect(Collectors.toList());
    }

    public List<OrderItem> getVisibleOrders() {
        return orders.values().stream()
                .filter(OrderItem::shouldOrderBeVisible)
                .collect(Collectors.toList());
    }

    public List<OrderItem> getExpiredOrders() {
        return orders.values().stream()
                .filter(o -> o.isExpired() && !o.shouldBeDeleted())
                .collect(Collectors.toList());
    }

    public List<OrderItem> getOrdersToDelete() {
        return orders.values().stream()
                .filter(OrderItem::shouldBeDeleted)
                .collect(Collectors.toList());
    }

    public int countActiveOrdersByPlayer(UUID playerUuid) {
        return (int) orders.values().stream()
                .filter(o -> playerUuid.equals(o.getCreator()) && o.isActive() && !o.isExpired())
                .count();
    }
}
