package me.vennlmao.ariscore.tab.managers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.tab.models.TabProfile;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TabListManager implements Listener {

    private final ArisCore plugin;
    private final PapiManager papi;
    private final ConditionEvaluator conditions;
    private final TabConfigManager config;
    private final Map<UUID, ScheduledTask> tasks  = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerState>   states = new ConcurrentHashMap<>();

    public TabListManager(ArisCore plugin, PapiManager papi, ConditionEvaluator conditions, TabConfigManager config) {
        this.plugin = plugin; this.papi = papi; this.conditions = conditions; this.config = config;
    }

    public void start() {
        if (!config.isTabEnabled()) return;
        for (Player p : Bukkit.getOnlinePlayers()) schedule(p);
    }

    public void stop() {
        tasks.values().forEach(t -> { try { t.cancel(); } catch (Throwable ignored) {} });
        tasks.clear();
        states.clear();
        for (Player p : Bukkit.getOnlinePlayers()) {
            try { p.sendPlayerListHeader(Component.empty()); p.sendPlayerListFooter(Component.empty()); p.playerListName(null); }
            catch (Throwable ignored) {}
        }
    }

    public void reload() { stop(); start(); }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) { if (config.isTabEnabled()) schedule(e.getPlayer()); }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();
        ScheduledTask t = tasks.remove(id);
        if (t != null) try { t.cancel(); } catch (Throwable ignored) {}
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
        long ticks = Math.max(1L, config.getTabUpdateTicks());
        ScheduledTask task = player.getScheduler().runAtFixedRate((Plugin) plugin, t -> tick(player), () -> {}, 1L, ticks);
        if (task != null) tasks.put(player.getUniqueId(), task);
    }

    private void tick(Player player) {
        if (!player.isOnline() || player.hasPermission("ariscore.tab.bypass")) return;
        PlayerState state = states.computeIfAbsent(player.getUniqueId(), k -> new PlayerState());
        TabProfile profile = resolveProfile(player, state);

        if (profile == null) {
            if (!state.lastHeader.isEmpty() || !state.lastFooter.isEmpty()) {
                try { player.sendPlayerListHeader(Component.empty()); player.sendPlayerListFooter(Component.empty()); } catch (Throwable ignored) {}
                state.lastHeader = ""; state.lastFooter = "";
            }
            return;
        }

        String header = papi.parse(player, joinLines(profile.getHeader()));
        if (!header.equals(state.lastHeader)) {
            state.lastHeader = header;
            try { player.sendPlayerListHeader(header.isEmpty() ? Component.empty() : LegacyComponentSerializer.legacySection().deserialize(header)); }
            catch (Throwable ignored) {}
        }

        String footer = papi.parse(player, joinLines(profile.getFooter()));
        if (!footer.equals(state.lastFooter)) {
            state.lastFooter = footer;
            try { player.sendPlayerListFooter(footer.isEmpty() ? Component.empty() : LegacyComponentSerializer.legacySection().deserialize(footer)); }
            catch (Throwable ignored) {}
        }

        String fmt = profile.getTablistNameFormat();
        if (fmt != null && !fmt.isEmpty()) {
            String name = papi.parse(player, fmt);
            if (!name.equals(state.lastListName)) {
                state.lastListName = name;
                try { player.playerListName(LegacyComponentSerializer.legacySection().deserialize(name)); }
                catch (Throwable t) { try { player.setPlayerListName(name); } catch (Throwable ignored) {} }
            }
        }
    }

    private TabProfile resolveProfile(Player player, PlayerState state) {
        long now = System.currentTimeMillis();
        if (now < state.profileExpiry) return state.cachedProfile;
        TabProfile found = null;
        String worldName = player.getWorld().getName();
        for (TabProfile p : config.getTabProfiles()) {
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
        TabProfile cachedProfile;
        long profileExpiry;
        String lastHeader   = "\0UNSENT";
        String lastFooter   = "\0UNSENT";
        String lastListName = "\0UNSENT";
    }
    }
