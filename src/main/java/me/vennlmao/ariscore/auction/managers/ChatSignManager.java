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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ChatSignManager implements Listener {

    private final ArisCore plugin;
    private final Map<UUID, Consumer<String>> chatCallbacks = new HashMap<>();
    private final Map<UUID, Long> callbackTimestamps = new HashMap<>();
    private final Map<UUID, Consumer<String>> pendingSigns = new HashMap<>();
    private final Map<UUID, Location> pendingSignLocations = new HashMap<>();
    private final Map<UUID, BlockState> pendingSignPrevState = new HashMap<>();
    private static final long TIMEOUT_MS = 30_000;
    private static final int SIGN_HEIGHT_OFFSET = 5;

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

    private void openSign(Player player, Consumer<String> callback) {
        player.closeInventory();

        Location loc = player.getLocation().clone();
        loc.setY(loc.getWorld().getMaxHeight() - SIGN_HEIGHT_OFFSET);
        loc.setPitch(0);

        player.getScheduler().run((Plugin) plugin, t -> {
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
            sign.setAllowedEditor(player);
            sign.update(true, false);

            pendingSigns.put(player.getUniqueId(), callback);
            pendingSignLocations.put(player.getUniqueId(), loc);
            pendingSignPrevState.put(player.getUniqueId(), previousState);

            player.openSign((Sign) block.getState(), Side.FRONT);
        }, null);
    }

    private void closePendingSign(Player player) {
        UUID uuid = player.getUniqueId();
        Location loc = pendingSignLocations.remove(uuid);
        BlockState previousState = pendingSignPrevState.remove(uuid);
        pendingSigns.remove(uuid);
        if (loc == null || previousState == null) return;

        player.getScheduler().run((Plugin) plugin, t -> previousState.update(true, false), null);
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
        pendingSignLocations.remove(uuid);
        pendingSignPrevState.remove(uuid);
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        Consumer<String> callback = pendingSigns.get(player.getUniqueId());
        if (callback == null) return;

        String raw = event.getLine(2) == null ? "" : event.getLine(2).trim();

        closePendingSign(player);

        if (raw.equalsIgnoreCase("cancel")) {
            player.sendMessage(ColorUtil.colorize(plugin.getAuctionModule().getLangManager().getSearchCancelled()));
            return;
        }

        player.getScheduler().run((Plugin) plugin, t -> callback.accept(raw), null);
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
        Location loc = pendingSignLocations.remove(uuid);
        BlockState previousState = pendingSignPrevState.remove(uuid);
        pendingSigns.remove(uuid);
        if (loc != null && previousState != null) {
            Bukkit.getRegionScheduler().run((Plugin) plugin, loc, t -> previousState.update(true, false));
        }
    }
}
