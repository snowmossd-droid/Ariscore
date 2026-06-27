package me.vennlmao.ariscore.tab.managers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.vennlmao.ariscore.ArisCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NameTagManager implements Listener {

    private static final String TEAM_PREFIX = "nt_";

    private final ArisCore plugin;
    private final PapiManager papi;
    private final TabConfigManager config;

    private final Map<UUID, ScheduledTask> tasks      = new ConcurrentHashMap<>();
    private final Map<UUID, String>        lastPrefix = new ConcurrentHashMap<>();
    private final Map<UUID, String>        lastSuffix = new ConcurrentHashMap<>();
    private final Map<UUID, String>        lastSbId   = new ConcurrentHashMap<>();

    public NameTagManager(ArisCore plugin, PapiManager papi, TabConfigManager config, Scoreboard ignored) {
        this.plugin = plugin;
        this.papi   = papi;
        this.config = config;
    }

    public void start() {
        if (!config.isNametagEnabled()) return;
        for (Player p : Bukkit.getOnlinePlayers()) schedule(p);
    }

    public void stop() {
        tasks.values().forEach(t -> { try { t.cancel(); } catch (Throwable ignored) {} });
        tasks.clear();
        lastPrefix.clear();
        lastSuffix.clear();
        lastSbId.clear();
    }

    public void reload() { stop(); start(); }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!config.isNametagEnabled()) return;
        Player player = e.getPlayer();
        player.getScheduler().runDelayed((Plugin) plugin, t -> schedule(player), () -> {}, 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        ScheduledTask t = tasks.remove(id);
        if (t != null) try { t.cancel(); } catch (Throwable ignored) {}
        lastPrefix.remove(id);
        lastSuffix.remove(id);
        lastSbId.remove(id);
    }

    private void schedule(Player player) {
        ScheduledTask old = tasks.remove(player.getUniqueId());
        if (old != null) try { old.cancel(); } catch (Throwable ignored) {}

        long ticks = Math.max(1L, config.getNametagUpdateTicks());
        ScheduledTask task = player.getScheduler().runAtFixedRate(
                (Plugin) plugin, t -> tick(player), () -> {}, 1L, ticks);
        if (task != null) tasks.put(player.getUniqueId(), task);
    }

    private void tick(Player player) {
        if (!player.isOnline()) return;

        String prefix = truncate(papi.parse(player, config.getNametagPrefix()), 16);
        String suffix = truncate(papi.parse(player, config.getNametagSuffix()), 16);

        UUID id = player.getUniqueId();
        Scoreboard sb = player.getScoreboard();
        String sbId = sb == null ? "null" : sb.toString();

        boolean changed = !prefix.equals(lastPrefix.get(id))
                || !suffix.equals(lastSuffix.get(id))
                || !sbId.equals(lastSbId.get(id));

        if (!changed) return;

        lastPrefix.put(id, prefix);
        lastSuffix.put(id, suffix);
        lastSbId.put(id, sbId);

        applyToAllBoards(player, prefix, suffix);
    }

    private void applyToAllBoards(Player player, String prefix, String suffix) {
        try {
            if (!player.isOnline()) return;

            String rawName   = player.getName();
            int maxLen       = 16 - TEAM_PREFIX.length();
            String shortName = rawName.length() > maxLen ? rawName.substring(0, maxLen) : rawName;
            String teamName  = TEAM_PREFIX + shortName;

            
            Scoreboard sb = player.getScoreboard();
            if (sb != null) applyTeam(sb, teamName, rawName, prefix, suffix);

            
            Scoreboard main = Bukkit.getScoreboardManager() != null
                    ? Bukkit.getScoreboardManager().getMainScoreboard() : null;
            if (main != null && !main.equals(sb)) {
                applyTeam(main, teamName, rawName, prefix, suffix);
            }

            
            for (Player other : Bukkit.getOnlinePlayers()) {
                Scoreboard otherSb = other.getScoreboard();
                if (otherSb != null && !otherSb.equals(sb) && !otherSb.equals(main)) {
                    applyTeam(otherSb, teamName, rawName, prefix, suffix);
                }
            }

        } catch (Throwable e) {
            plugin.getLogger().warning("[Tab/Nametag] Failed to apply team for "
                    + player.getName() + ": " + e.getMessage());
        }
    }

    private void applyTeam(Scoreboard sb, String teamName, String entry, String prefix, String suffix) {
        try {
            Team team = sb.getTeam(teamName);
            if (team == null) team = sb.registerNewTeam(teamName);
            team.setPrefix(prefix != null ? prefix : "");
            team.setSuffix(suffix != null ? suffix : "");
            if (!team.hasEntry(entry)) team.addEntry(entry);
        } catch (Throwable ignored) {}
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }
}
