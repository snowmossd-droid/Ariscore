package me.vennlmao.ariscore.auction.managers;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.auction.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ChatSignManager implements Listener {

    private final ArisCore plugin;
    private final Map<UUID, Consumer<String>> chatCallbacks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> callbackTimestamps = new ConcurrentHashMap<>();
    private final Map<UUID, PendingSign> pendingSigns = new ConcurrentHashMap<>();

    private static final long TIMEOUT_MS = 30_000;
    private static final int HEAD_OFFSET = 2;
    private static final int MAX_VERTICAL_SEARCH = 4;

    private static final class PendingSign {
        final Consumer<String> callback;
        final Location location;
        final BlockState previousState;

        PendingSign(Consumer<String> callback, Location location, BlockState previousState) {
            this.callback = callback;
            this.location = location;
            this.previousState = previousState;
        }
    }

    public ChatSignManager(ArisCore plugin) {
        this.plugin = plugin;
    }

    public void startCleanupTask() {
        Bukkit.getAsyncScheduler().runAtFixedRate((Plugin) plugin, task -> {
            long now = System.currentTimeMillis();
            callbackTimestamps.entrySet().removeIf(e -> {
                if (now - e.getValue() > TIMEOUT_MS) {
                    chatCallbacks.remove(e.getKey());
                    return true;
                }
                return false;
            });
        }, 0L, 10L, java.util.concurrent.TimeUnit.SECONDS);
    }

    public void requestInput(Player player, Consumer<String> callback) {
        AuctionConfigManager cfg = plugin.getAuctionModule().getConfigManager();
        if (cfg.useSign()) {
            openSign(player, callback);
        } else if (cfg.useChat()) {
            requestChat(player, callback);
        }
    }

    private Location findSignLocation(Player player) {
        Location base = player.getLocation().clone();
        base.setPitch(0);

        for (int dy = HEAD_OFFSET; dy <= HEAD_OFFSET + MAX_VERTICAL_SEARCH; dy++) {
            Location check = base.clone().add(0, dy, 0);
            if (check.getBlock().getType().isAir()) {
                return check;
            }
        }
        return null;
    }

    private void openSign(Player player, Consumer<String> callback) {
        UUID uuid = player.getUniqueId();

        if (pendingSigns.containsKey(uuid)) {
            closePendingSign(player);
        }

        player.closeInventory();

        player.getScheduler().run((Plugin) plugin, t -> {
            Location loc = findSignLocation(player);
            if (loc == null) {
                player.sendMessage(ColorUtil.colorize("&cNo space nearby to open search, please move and try again!"));
                return;
            }

            Block block = loc.getBlock();
            BlockState previousState = block.getState();

            block.setType(Material.OAK_SIGN, false);
            BlockState state = block.getState();
            if (!(state instanceof Sign)) {
                block.setType(previousState.getType(), false);
                player.sendMessage(ColorUtil.colorize("&cCould not open search, please try again!"));
                return;
            }

            Sign sign = (Sign) state;
            List<String> lines = plugin.getAuctionModule().getConfigManager().getSignLines();
            for (int i = 0; i < 4 && i < lines.size(); i++) {
                sign.getSide(Side.FRONT).setLine(i, ColorUtil.colorize(lines.get(i)));
            }
            sign.setWaxed(false);
            sign.update(true, false);

            pendingSigns.put(uuid, new PendingSign(callback, loc, previousState));

            player.openSign((Sign) block.getState(), Side.FRONT);
        }, null);
    }

    private void closePendingSign(Player player) {
        UUID uuid = player.getUniqueId();
        PendingSign pending = pendingSigns.remove(uuid);
        if (pending == null) return;

        player.getScheduler().run((Plugin) plugin, t -> pending.previousState.update(true, false), null);
    }

    public void requestChat(Player player, Consumer<String> callback) {
        chatCallbacks.put(player.getUniqueId(), callback);
        callbackTimestamps.put(player.getUniqueId(), System.currentTimeMillis());
        player.closeInventory();
        player.sendMessage(ColorUtil.colorize(plugin.getAuctionModule().getLangManager().getSearchPrompt()));
    }

    public boolean hasPendingInput(UUID uuid) {
        return chatCallbacks.containsKey(uuid) || pendingSigns.containsKey(uuid);
    }

    public void cancelInput(UUID uuid) {
        chatCallbacks.remove(uuid);
        callbackTimestamps.remove(uuid);
        pendingSigns.remove(uuid);
    }

    public void restoreAllPendingSigns() {
        for (Map.Entry<UUID, PendingSign> entry : pendingSigns.entrySet()) {
            PendingSign pending = entry.getValue();
            Bukkit.getRegionScheduler().run((Plugin) plugin, pending.location, t -> pending.previousState.update(true, false));
        }
        pendingSigns.clear();
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        PendingSign pending = pendingSigns.get(player.getUniqueId());
        if (pending == null) return;

        String raw = event.getLine(2) == null ? "" : event.getLine(2).trim();

        closePendingSign(player);

        if (raw.equalsIgnoreCase("cancel")) {
            player.sendMessage(ColorUtil.colorize(plugin.getAuctionModule().getLangManager().getSearchCancelled()));
            return;
        }

        player.getScheduler().run((Plugin) plugin, t -> pending.callback.accept(raw), null);
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
        player.getScheduler().run((Plugin) plugin, t -> callback.accept(input), null);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        chatCallbacks.remove(uuid);
        callbackTimestamps.remove(uuid);
        PendingSign pending = pendingSigns.remove(uuid);
        if (pending != null) {
            Bukkit.getRegionScheduler().run((Plugin) plugin, pending.location, t -> pending.previousState.update(true, false));
        }
    }
}
