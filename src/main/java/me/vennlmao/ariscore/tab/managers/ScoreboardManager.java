package me.vennlmao.ariscore.tab.managers;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisplayScoreboard;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateScore;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.clip.placeholderapi.PlaceholderAPI;
import me.vennlmao.ariscore.tab.TabModule;
import me.vennlmao.ariscore.tab.utils.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.Collection;

public class ScoreboardManager {

    private static final String OBJ = "arsb";
    private static final String[] ENTRIES = {
        "§0","§1","§2","§3","§4","§5","§6","§7",
        "§8","§9","§a","§b","§c","§d","§e","§f",
        "§0§0","§1§1","§2§2","§3§3","§4§4","§5§5","§6§6","§7§7",
        "§8§8","§9§9","§a§a","§b§b","§c§c","§d§d","§e§e","§f§f"
    };

    private final TabModule module;
    private final Map<UUID, ScheduledTask> tasks = new HashMap<>();
    private final Map<UUID, Integer> lineCounts = new HashMap<>();
    private final Set<UUID> disabled = new HashSet<>();
    private final Set<UUID> active = new HashSet<>();

    public ScoreboardManager(TabModule module) {
        this.module = module;
    }

    public void startFor(Player player) {
        if (disabled.contains(player.getUniqueId())) return;
        createObjective(player);
        long period = module.getConfig().getLong("scoreboard.update-tick", 2) * 20L;
        ScheduledTask task = player.getScheduler().runAtFixedRate(module.getPlugin(), t -> {
            if (!player.isOnline()) { t.cancel(); return; }
            updateBoard(player);
        }, null, 1L, period);
        if (task != null) tasks.put(player.getUniqueId(), task);
    }

    public void stopFor(Player player) {
        ScheduledTask task = tasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
        if (active.remove(player.getUniqueId())) removeObjective(player);
        lineCounts.remove(player.getUniqueId());
    }

    public void toggle(Player player) {
        UUID uuid = player.getUniqueId();
        if (disabled.contains(uuid)) {
            disabled.remove(uuid);
            startFor(player);
        } else {
            disabled.add(uuid);
            stopFor(player);
        }
    }

    public boolean isDisabled(Player player) {
        return disabled.contains(player.getUniqueId());
    }

    public void removeAll() {
        new ArrayList<>(tasks.values()).forEach(ScheduledTask::cancel);
        tasks.clear();
        new HashSet<>(active).forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) removeObjective(p);
        });
        active.clear();
        lineCounts.clear();
    }

    public void reloadAll() {
        Bukkit.getOnlinePlayers().forEach(p -> {
            stopFor(p);
            if (!disabled.contains(p.getUniqueId())) startFor(p);
        });
    }

    public void refreshWorld(Player player) {
        player.getScheduler().run(module.getPlugin(), t -> updateBoard(player), null);
    }

    private void createObjective(Player player) {
        String title = getWorldField(player.getWorld().getName(), "title");
        sendPacket(player, new WrapperPlayServerScoreboardObjective(
            OBJ,
            WrapperPlayServerScoreboardObjective.ObjectiveMode.CREATE,
            ColorUtil.parse(resolve(player, title)),
            WrapperPlayServerScoreboardObjective.RenderType.INTEGER
        ));
        sendPacket(player, new WrapperPlayServerDisplayScoreboard(1, OBJ));
        active.add(player.getUniqueId());
        renderLines(player, getWorldLines(player.getWorld().getName()));
    }

    private void removeObjective(Player player) {
        int prev = lineCounts.getOrDefault(player.getUniqueId(), 0);
        for (int i = 0; i < prev; i++) removeTeam(player, i);
        sendPacket(player, new WrapperPlayServerScoreboardObjective(
            OBJ,
            WrapperPlayServerScoreboardObjective.ObjectiveMode.REMOVE,
            null,
            null
        ));
    }

    private void updateBoard(Player player) {
        if (!active.contains(player.getUniqueId())) { createObjective(player); return; }
        String title = getWorldField(player.getWorld().getName(), "title");
        sendPacket(player, new WrapperPlayServerScoreboardObjective(
            OBJ,
            WrapperPlayServerScoreboardObjective.ObjectiveMode.UPDATE,
            ColorUtil.parse(resolve(player, title)),
            WrapperPlayServerScoreboardObjective.RenderType.INTEGER
        ));
        renderLines(player, getWorldLines(player.getWorld().getName()));
    }

    private void renderLines(Player player, List<String> rawLines) {
        List<String> lines = buildLines(player, rawLines);
        int prev = lineCounts.getOrDefault(player.getUniqueId(), -1);

        for (int i = 0; i < Math.min(lines.size(), ENTRIES.length); i++) {
            String entry = ENTRIES[i];
            Component prefix = lines.get(i).isEmpty() ? Component.empty() : ColorUtil.parse(lines.get(i));
            if (i < prev) {
                updateTeam(player, i, prefix);
            } else {
                createTeam(player, i, entry, prefix);
                setScore(player, entry, lines.size() - i);
            }
        }
        if (prev > lines.size()) {
            for (int i = lines.size(); i < prev; i++) {
                removeScore(player, ENTRIES[i]);
                removeTeam(player, i);
            }
        } else if (prev >= 0 && lines.size() != prev) {
            for (int i = 0; i < lines.size(); i++) {
                setScore(player, ENTRIES[i], lines.size() - i);
            }
        }
        lineCounts.put(player.getUniqueId(), lines.size());
    }

    private void createTeam(Player player, int index, String entry, Component prefix) {
        WrapperPlayServerTeams.ScoreBoardTeamInfo info = new WrapperPlayServerTeams.ScoreBoardTeamInfo(
            Component.empty(), prefix, Component.empty(),
            WrapperPlayServerTeams.NameTagVisibility.ALWAYS, WrapperPlayServerTeams.CollisionRule.ALWAYS,
            null, WrapperPlayServerTeams.OptionData.NONE
        );
        sendPacket(player, new WrapperPlayServerTeams(
            "arsb_" + index, WrapperPlayServerTeams.TeamMode.CREATE,
            Optional.of(info), Collections.singletonList(entry)
        ));
    }

    private void updateTeam(Player player, int index, Component prefix) {
        WrapperPlayServerTeams.ScoreBoardTeamInfo info = new WrapperPlayServerTeams.ScoreBoardTeamInfo(
            Component.empty(), prefix, Component.empty(),
            WrapperPlayServerTeams.NameTagVisibility.ALWAYS, WrapperPlayServerTeams.CollisionRule.ALWAYS,
            null, WrapperPlayServerTeams.OptionData.NONE
        );
        sendPacket(player, new WrapperPlayServerTeams(
            "arsb_" + index, WrapperPlayServerTeams.TeamMode.UPDATE,
            Optional.of(info), Collections.emptyList()
        ));
    }

    private void removeTeam(Player player, int index) {
        sendPacket(player, new WrapperPlayServerTeams(
            "arsb_" + index, WrapperPlayServerTeams.TeamMode.REMOVE,
            Optional.empty(), Collections.emptyList()
        ));
    }

    private void setScore(Player player, String entry, int score) {
        sendPacket(player, new WrapperPlayServerUpdateScore(
            entry, WrapperPlayServerUpdateScore.Action.CREATE_OR_UPDATE_ITEM,
            OBJ, Optional.of(score)
        ));
    }

    private void removeScore(Player player, String entry) {
        sendPacket(player, new WrapperPlayServerUpdateScore(
            entry, WrapperPlayServerUpdateScore.Action.REMOVE_ITEM,
            OBJ, Optional.empty()
        ));
    }

    private void sendPacket(Player player, com.github.retrooper.packetevents.wrapper.PacketWrapper<?> packet) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    private List<String> buildLines(Player player, List<String> rawLines) {
        List<String> result = new ArrayList<>();
        String teamFormat = module.getConfig().getString("scoreboard.team-line.format", "");
        for (String line : rawLines) {
            if ("{team}".equals(line)) {
                String teamName = resolve(player, "%ariscore_team%");
                if (!teamName.isEmpty() && !teamName.startsWith("%")) {
                    result.add(resolve(player, teamFormat));
                }
            } else {
                result.add(resolve(player, line));
            }
        }
        return result;
    }

    private List<String> getWorldLines(String worldName) {
        ConfigurationSection worlds = module.getConfig().getConfigurationSection("scoreboard.worlds");
        if (worlds != null) {
            for (String key : worlds.getKeys(false)) {
                String w = module.getConfig().getString("scoreboard.worlds." + key + ".world", "");
                if (w.equals(worldName)) return module.getConfig().getStringList("scoreboard.worlds." + key + ".lines");
            }
        }
        return module.getConfig().getStringList("scoreboard.default.lines");
    }

    private String getWorldField(String worldName, String field) {
        String def = module.getConfig().getString("scoreboard.default." + field, "");
        ConfigurationSection worlds = module.getConfig().getConfigurationSection("scoreboard.worlds");
        if (worlds != null) {
            for (String key : worlds.getKeys(false)) {
                String w = module.getConfig().getString("scoreboard.worlds." + key + ".world", "");
                if (w.equals(worldName)) return module.getConfig().getString("scoreboard.worlds." + key + "." + field, def);
            }
        }
        return def;
    }

    private String resolve(Player player, String text) {
        if (text == null) return "";
        if (module.getPlugin().getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return PlaceholderAPI.setPlaceholders(player, text);
        }
        return text;
    }
                                                }
                                     
