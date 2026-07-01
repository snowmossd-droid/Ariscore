package me.vennlmao.ariscore.tab.managers;

import fr.mrmicky.fastboard.adventure.FastBoard;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.tab.models.ScoreboardProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardManager implements Listener {

    private final ArisCore plugin;
    private final PapiManager papi;
    private final ConditionEvaluator conditions;
    private final TabConfigManager config;
    private final Map<UUID, FastBoard>     boards = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> tasks  = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerState>   states = new ConcurrentHashMap<>();

    public ScoreboardManager(ArisCore plugin, PapiManager papi, ConditionEvaluator conditions, TabConfigManager config) {
        this.plugin = plugin; this.papi = papi; this.conditions = conditions; this.config = config;
    }

    public void start() {
        if (!config.isScoreboardEnabled()) return;
        for (Player p : Bukkit.getOnlinePlayers()) schedule(p);
    }

    public void stop() {
        tasks.values().forEach(t -> { try { t.cancel(); } catch (Throwable ignored) {} });
        tasks.clear();
        states.clear();
        boards.values().forEach(b -> { try { b.delete(); } catch (Throwable ignored) {} });
        boards.clear();
    }

    public void reload() { stop(); start(); }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) { if (config.isScoreboardEnabled()) schedule(e.getPlayer()); }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        ScheduledTask t = tasks.remove(id);
        if (t != null) try { t.cancel(); } catch (Throwable ignored) {}
        FastBoard b = boards.remove(id);
        if (b != null) try { b.delete(); } catch (Throwable ignored) {}
        states.remove(id);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        PlayerState s = states.get(e.getPlayer().getUniqueId());
        if (s != null) s.profileExpiry = 0L;
    }

    private void schedule(Player player) {
        ScheduledTask old = tasks.remove(player.getUniqueId());
        if (old != null) try { old.cancel(); } catch (Throwable ignored) {}
        long ticks = Math.max(1L, config.getScoreboardUpdateTicks());
        ScheduledTask task = player.getScheduler().runAtFixedRate(
                (Plugin) plugin, t -> tick(player), () -> {}, 1L, ticks);
        if (task != null) tasks.put(player.getUniqueId(), task);
    }

    private void tick(Player player) {
        if (!player.isOnline()) return;

        if (player.hasPermission("ariscore.tab.bypass")) {
            FastBoard b = boards.remove(player.getUniqueId());
            if (b != null) try { b.delete(); } catch (Throwable ignored) {}
            return;
        }

        PlayerState state = states.computeIfAbsent(player.getUniqueId(), k -> new PlayerState());
        ScoreboardProfile profile = resolveProfile(player, state);

        if (profile == null) {
            FastBoard b = boards.remove(player.getUniqueId());
            if (b != null) try { b.delete(); } catch (Throwable ignored) {}
            return;
        }

        FastBoard board = boards.computeIfAbsent(player.getUniqueId(), k -> new FastBoard(player));

        try {
            String title = papi.parse(player, profile.getTitle());
            if (!title.equals(state.lastTitle)) {
                state.lastTitle = title;
                board.updateTitle(LegacyComponentSerializer.legacySection().deserialize(title));
            }

            String rawLines = joinLines(profile.getLines());
            String parsedLines = papi.parse(player, rawLines);
            if (!parsedLines.equals(state.lastLines)) {
                state.lastLines = parsedLines;
                String[] split = parsedLines.isEmpty() ? new String[0] : parsedLines.split("\n", -1);
                List<Component> components = new ArrayList<>(split.length);
                for (String line : split) {
                    components.add(LegacyComponentSerializer.legacySection().deserialize(line));
                }
                board.updateLines(components);
            }
        } catch (Throwable e) {
            plugin.getLogger().warning("[Tab/Scoreboard] render error for " + player.getName() + ": " + e.getMessage());
        }
    }

    private ScoreboardProfile resolveProfile(Player player, PlayerState state) {
        long now = System.currentTimeMillis();
        if (now < state.profileExpiry) return state.cachedProfile;
        ScoreboardProfile found = null;
        String worldName = player.getWorld().getName();
        for (ScoreboardProfile p : config.getScoreboardProfiles()) {
            if (p.getWorld() != null && !p.getWorld().trim().isEmpty()
                    && !p.getWorld().trim().equalsIgnoreCase(worldName)) continue;
            if (conditions.evaluate(player, p.getDisplayCondition())) { found = p; break; }
        }
        state.cachedProfile = found;
        state.profileExpiry = now + 2000L;
        return found;
    }

    private String joinLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) return "";
        if (lines.size() == 1) return lines.get(0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) { if (i > 0) sb.append('\n'); sb.append(lines.get(i)); }
        return sb.toString();
    }

    private static class PlayerState {
        ScoreboardProfile cachedProfile;
        long profileExpiry;
        String lastTitle = "\0UNSENT";
        String lastLines = "\0UNSENT";
    }
}