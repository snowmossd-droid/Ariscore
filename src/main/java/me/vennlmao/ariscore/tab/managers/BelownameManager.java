package me.vennlmao.ariscore.tab.managers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.vennlmao.ariscore.ArisCore;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BelownameManager implements Listener {

    private static final String OBJECTIVE_NAME = "aris_belowname";

    private final ArisCore plugin;
    private final PapiManager papi;
    private final TabConfigManager config;

    private final Map<UUID, ScheduledTask> tasks   = new ConcurrentHashMap<>();
    private final Map<UUID, Integer>       lastVal = new ConcurrentHashMap<>();

    public BelownameManager(ArisCore plugin, PapiManager papi, TabConfigManager config) {
        this.plugin = plugin;
        this.papi   = papi;
        this.config = config;
    }

    public void start() {
        if (!config.isBelownameEnabled()) return;
        setupObjective();
        for (Player p : Bukkit.getOnlinePlayers()) schedule(p);
    }

    public void stop() {
        tasks.values().forEach(t -> { try { t.cancel(); } catch (Throwable ignored) {} });
        tasks.clear();
        lastVal.clear();
        removeObjective();
    }

    public void reload() { stop(); start(); }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!config.isBelownameEnabled()) return;
        Player player = e.getPlayer();
        player.getScheduler().runDelayed((Plugin) plugin, t -> schedule(player), () -> {}, 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        ScheduledTask t = tasks.remove(id);
        if (t != null) try { t.cancel(); } catch (Throwable ignored) {}
        lastVal.remove(id);
    }

    private void setupObjective() {
        try {
            Scoreboard main = Bukkit.getScoreboardManager() != null
                    ? Bukkit.getScoreboardManager().getMainScoreboard() : null;
            if (main == null) return;
            Objective obj = main.getObjective(OBJECTIVE_NAME);
            if (obj == null) obj = main.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY,
                    LegacyComponentSerializer.legacySection().deserialize(config.getBelownameText()));
            else obj.displayName(LegacyComponentSerializer.legacySection().deserialize(config.getBelownameText()));
            obj.setDisplaySlot(DisplaySlot.BELOW_NAME);
        } catch (Throwable ignored) {}
    }

    private void removeObjective() {
        try {
            Scoreboard main = Bukkit.getScoreboardManager() != null
                    ? Bukkit.getScoreboardManager().getMainScoreboard() : null;
            if (main == null) return;
            Objective obj = main.getObjective(OBJECTIVE_NAME);
            if (obj != null) obj.unregister();
        } catch (Throwable ignored) {}
    }

    private void schedule(Player player) {
        ScheduledTask old = tasks.remove(player.getUniqueId());
        if (old != null) try { old.cancel(); } catch (Throwable ignored) {}

        long ticks = Math.max(1L, config.getBelownameUpdateTicks());
        ScheduledTask task = player.getScheduler().runAtFixedRate(
                (Plugin) plugin, t -> tick(player), () -> {}, 1L, ticks);
        if (task != null) tasks.put(player.getUniqueId(), task);
    }

    private void tick(Player player) {
        if (!player.isOnline()) return;
        try {
            String parsed = papi.parse(player, config.getBelownameValuePlaceholder());
            int value = parseIntSafe(parsed);

            UUID id = player.getUniqueId();
            Integer last = lastVal.get(id);
            if (last != null && last == value) return;
            lastVal.put(id, value);

            Scoreboard main = Bukkit.getScoreboardManager() != null
                    ? Bukkit.getScoreboardManager().getMainScoreboard() : null;
            if (main == null) return;
            Objective obj = main.getObjective(OBJECTIVE_NAME);
            if (obj == null) return;
            obj.getScore(player.getName()).setScore(value);
        } catch (Throwable ignored) {}
    }

    private int parseIntSafe(String s) {
        try {
            String cleaned = s.replaceAll("[^0-9.\\-]", "");
            if (cleaned.isEmpty()) return 0;
            return (int) Double.parseDouble(cleaned);
        } catch (Exception e) {
            return 0;
        }
    }
}
