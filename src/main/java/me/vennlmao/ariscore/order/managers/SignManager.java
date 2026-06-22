package me.vennlmao.ariscore.order.managers;

import me.vennlmao.ariscore.ArisCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SignManager implements Listener {

    private final ArisCore plugin;
    private final Map<UUID, Consumer<String>> callbacks = new ConcurrentHashMap<>();
    private final Map<UUID, Location> signLocations = new ConcurrentHashMap<>();
    private final Map<UUID, Long> timestamps = new ConcurrentHashMap<>();
    private static final long TIMEOUT_MS = 60_000L;

    public SignManager(ArisCore plugin) {
        this.plugin = plugin;
    }

    private final Map<UUID, Material> originalBlockTypes = new ConcurrentHashMap<>();

    public void requestInput(Player player, String signKey, Consumer<String> callback) {
        player.closeInventory();
        Location loc = player.getLocation().clone();
        loc.setY(Math.min(loc.getWorld().getMaxHeight() - 5, loc.getBlockY() + 50));
        Block block = loc.getBlock();
        Material originalType = block.getType();

        block.setType(Material.OAK_SIGN, false);
        if (!(block.getState() instanceof Sign sign)) {
            block.setType(originalType, false);
            return;
        }

        List<String> lines = plugin.getOrderModule().getConfigManager().getConfig().getStringList("sign-gui." + signKey);
        if (lines.isEmpty()) lines = List.of("", "^^^^^^^^^^^^^", "Type value", "");
        for (int i = 0; i < 4 && i < lines.size(); i++) {
            sign.getSide(Side.FRONT).setLine(i, me.vennlmao.ariscore.order.utils.ColorUtil.color(lines.get(i)));
        }
        sign.update(true, false);

        callbacks.put(player.getUniqueId(), callback);
        signLocations.put(player.getUniqueId(), loc);
        originalBlockTypes.put(player.getUniqueId(), originalType);
        timestamps.put(player.getUniqueId(), System.currentTimeMillis());

        player.openSign(sign, Side.FRONT);
    }

    public boolean hasPending(UUID uuid) {
        return callbacks.containsKey(uuid);
    }

    public void cancel(UUID uuid) {
        callbacks.remove(uuid);
        timestamps.remove(uuid);
        restoreBlock(uuid);
    }

    private void restoreBlock(UUID uuid) {
        Location loc = signLocations.remove(uuid);
        Material original = originalBlockTypes.remove(uuid);
        if (loc != null) {
            loc.getBlock().setType(original != null ? original : Material.AIR, false);
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Consumer<String> callback = callbacks.get(uuid);
        if (callback == null) return;
        if (event.getSide() != Side.FRONT) return;

        String input = event.getLine(2) != null ? event.getLine(2).trim() : "";

        callbacks.remove(uuid);
        timestamps.remove(uuid);
        Player player = event.getPlayer();

        Bukkit.getGlobalRegionScheduler().run((Plugin) plugin, t -> restoreBlock(uuid));

        if (input.isEmpty() || input.equalsIgnoreCase("cancel")) {
            player.sendMessage(plugin.getOrderModule().getConfigManager().msg("messages.input-cancelled"));
            return;
        }

        player.getScheduler().run((Plugin) plugin, t -> callback.accept(input), null);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer().getUniqueId());
    }

    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        timestamps.entrySet().removeIf(e -> {
            if (now - e.getValue() > TIMEOUT_MS) {
                callbacks.remove(e.getKey());
                restoreBlock(e.getKey());
                return true;
            }
            return false;
        });
    }
}
