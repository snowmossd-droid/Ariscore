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

    private final Map<UUID, ScheduledTask> tasks    = new ConcurrentHashMap<>();
    private final Map<UUID, String>        lastTag  = new ConcurrentHashMap<>();
    private final Map<UUID, String>        lastSbId = new ConcurrentHashMap<>();

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
        lastTag.clear();
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
        lastTag.remove(id);
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

        String tag = truncate(papi.parse(player, config.getNametagTag()), 16);

        UUID id = player.getUniqueId();
        Scoreboard sb = player.getScoreboard();
        String sbId = sb == null ? "null" : sb.toString();

        boolean changed = !tag.equals(lastTag.get(id)) || !sbId.equals(lastSbId.get(id));
        if (!changed) return;

        lastTag.put(id, tag);
        lastSbId.put(id, sbId);

        applyToAllBoards(player, tag);
    }

    private void applyToAllBoards(Player player, String tag) {
        try {
            if (!player.isOnline()) return;

            String rawName   = player.getName();
            int maxLen       = 16 - TEAM_PREFIX.length();
            String shortName = rawName.length() > maxLen ? rawName.substring(0, maxLen) : rawName;
            String teamName  = TEAM_PREFIX + shortName;

            Scoreboard sb = player.getScoreboard();
            if (sb != null) applyTeam(sb, teamName, rawName, tag);

            Scoreboard main = Bukkit.getScoreboardManager() != null
                    ? Bukkit.getScoreboardManager().getMainScoreboard() : null;
            if (main != null && !main.equals(sb)) {
                applyTeam(main, teamName, rawName, tag);
            }

            for (Player other : Bukkit.getOnlinePlayers()) {
                Scoreboard otherSb = other.getScoreboard();
                if (otherSb != null && !otherSb.equals(sb) && !otherSb.equals(main)) {
                    applyTeam(otherSb, teamName, rawName, tag);
                }
            }

        } catch (Throwable e) {
            plugin.getLogger().warning("[Tab/Nametag] Failed to apply team for "
                    + player.getName() + ": " + e.getMessage());
        }
    }

    private void applyTeam(Scoreboard sb, String teamName, String entry, String tag) {
        try {
            Team team = sb.getTeam(teamName);
            if (team == null) team = sb.registerNewTeam(teamName);
            team.setPrefix(tag != null ? tag : "");
            team.setSuffix("");
            if (!team.hasEntry(entry)) team.addEntry(entry);
        } catch (Throwable ignored) {}
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }
}
