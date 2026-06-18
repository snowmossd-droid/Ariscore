package me.vennlmao.ariscore.order.managers;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.order.utils.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SignManager implements Listener {

    private final ArisCore plugin;
    private final Map<UUID, Consumer<String>> callbacks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> timestamps = new ConcurrentHashMap<>();
    private static final long TIMEOUT_MS = 60_000L;

    public SignManager(ArisCore plugin) {
        this.plugin = plugin;
    }

    public void requestInput(Player player, String prompt, Consumer<String> callback) {
        callbacks.put(player.getUniqueId(), callback);
        timestamps.put(player.getUniqueId(), System.currentTimeMillis());
        player.closeInventory();
        player.sendMessage(ColorUtil.color(prompt));
        plugin.getOrderModule().getConfigManager().getConfig().getString("chat-input.cancel-hint", "");
        String hint = plugin.getOrderModule().getConfigManager().msg("messages.chat-input-cancel-hint");
        if (!hint.isEmpty()) player.sendMessage(hint);
    }

    public boolean hasPending(UUID uuid) { return callbacks.containsKey(uuid); }

    public void cancel(UUID uuid) {
        callbacks.remove(uuid);
        timestamps.remove(uuid);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Consumer<String> callback = callbacks.remove(uuid);
        if (callback == null) return;
        event.setCancelled(true);
        timestamps.remove(uuid);
        String input = event.getMessage().trim();
        if (input.equalsIgnoreCase("cancel")) {
            event.getPlayer().sendMessage(plugin.getOrderModule().getConfigManager().msg("messages.input-cancelled"));
            return;
        }
        event.getPlayer().getScheduler().run((Plugin) plugin, t -> callback.accept(input), null);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer().getUniqueId());
    }

    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        timestamps.entrySet().removeIf(e -> {
            if (now - e.getValue() > TIMEOUT_MS) { callbacks.remove(e.getKey()); return true; }
            return false;
        });
    }
}
