package me.vennlmao.ariscore.auction.managers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.auction.utils.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ChatSignManager implements Listener {

    private final ArisCore plugin;
    private final Map<UUID, Consumer<String>> chatCallbacks = new HashMap<>();
    private final Map<UUID, Long> callbackTimestamps = new HashMap<>();
    private ScheduledTask cleanupTask;
    private static final long TIMEOUT_MS = 30_000;

    public ChatSignManager(ArisCore plugin) {
        this.plugin = plugin;
    }

    public void startCleanupTask() {
        cleanupTask = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, t -> {
            long now = System.currentTimeMillis();
            callbackTimestamps.entrySet().removeIf(e -> {
                if (now - e.getValue() > TIMEOUT_MS) {
                    chatCallbacks.remove(e.getKey());
                    return true;
                }
                return false;
            });
        }, 200L, 200L);
    }

    public void stopCleanupTask() {
        if (cleanupTask != null) cleanupTask.cancel();
    }

    public void requestInput(Player player, Consumer<String> callback) {
        AuctionConfigManager cfg = plugin.getAuctionModule().getConfigManager();
        if (cfg.useSign()) {
            openSign(player, callback);
        } else if (cfg.useChat()) {
            requestChat(player, callback);
        }
    }

    private void openSign(Player player, Consumer<String> callback) {
        List<String> lines = plugin.getAuctionModule().getConfigManager().getSignLines();
        player.sendSignChange(player.getLocation(), lines.toArray(new String[0]));
        requestChat(player, callback);
    }

    public void requestChat(Player player, Consumer<String> callback) {
        chatCallbacks.put(player.getUniqueId(), callback);
        callbackTimestamps.put(player.getUniqueId(), System.currentTimeMillis());
        player.closeInventory();
        player.sendMessage(ColorUtil.colorize(plugin.getAuctionModule().getLangManager().getSearchPrompt()));
    }

    public boolean hasPendingInput(UUID uuid) {
        return chatCallbacks.containsKey(uuid);
    }

    public void cancelInput(UUID uuid) {
        chatCallbacks.remove(uuid);
        callbackTimestamps.remove(uuid);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Consumer<String> callback = chatCallbacks.get(player.getUniqueId());
        if (callback == null) return;
        event.setCancelled(true);
        String input = event.getMessage().trim();
        chatCallbacks.remove(player.getUniqueId());
        callbackTimestamps.remove(player.getUniqueId());
        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage(ColorUtil.colorize(plugin.getAuctionModule().getLangManager().getSearchCancelled()));
            return;
        }
        player.getScheduler().run(plugin, t -> callback.accept(input), null);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        chatCallbacks.remove(uuid);
        callbackTimestamps.remove(uuid);
    }
            }
