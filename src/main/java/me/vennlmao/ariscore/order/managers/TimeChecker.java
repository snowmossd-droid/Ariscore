package me.vennlmao.ariscore.order.managers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.vennlmao.ariscore.ArisCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class TimeChecker {

    private final ArisCore plugin;
    private ScheduledTask task;

    public TimeChecker(ArisCore plugin) {
        this.plugin = plugin;
    }

    public void start() {
        long intervalSeconds = plugin.getOrderModule().getConfigManager().getConfig().getLong("order.check-interval-seconds", 60L);
        task = Bukkit.getAsyncScheduler().runAtFixedRate((Plugin) plugin, t -> check(), 5L, intervalSeconds, TimeUnit.SECONDS);
    }

    public void stop() {
        if (task != null) { try { task.cancel(); } catch (Throwable ignored) {} task = null; }
    }

    private void check() {
        OrderManager orderManager = plugin.getOrderModule().getOrderManager();
        DataManager dataManager = plugin.getOrderModule().getDataManager();
        OrderConfigManager cfg = plugin.getOrderModule().getConfigManager();

        List<OrderItem> toDelete = orderManager.getOrdersToDelete();
        for (OrderItem order : toDelete) {
            orderManager.removeOrder(order.getOrderUuid());
            dataManager.deleteOrder(order.getOrderUuid());
        }

        List<OrderItem> expired = orderManager.getExpiredOrders();
        for (OrderItem order : expired) {
            if (order.shouldNotifyExpired()) {
                order.setExpiredNotified(true);
                dataManager.saveOrder(order);
                Player player = Bukkit.getPlayer(order.getCreator());
                if (player != null && player.isOnline()) {
                    player.sendMessage(cfg.msg("messages.order-expired",
                            "%item%", OrderItem.formatMaterialName(order.getMaterial().name()),
                            "%amount%", String.valueOf(order.getRequestedAmount())));
                }
            }
        }
    }
}
