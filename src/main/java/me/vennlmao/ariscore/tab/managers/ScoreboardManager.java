package me.vennlmao.ariscore.tab.managers;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisplayScoreboard;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.github.retrooper.packetevents.protocol.score.ScoreFormat;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateScore;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.clip.placeholderapi.PlaceholderAPI;
import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.tab.TabModule;
import me.vennlmao.ariscore.tab.utils.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;

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
    private final Set<UUID> disabled = new HashSet<>();
    private final Set<UUID> active = new HashSet<>();

    private final Map<UUID, List<String>> currentLines = new HashMap<>();

    public ScoreboardManager(TabModule module) {
        this.module = module;
    }

    public void startFor(Player player) {
        if (disabled.contains(player.getUniqueId())) return;
        setupObjective(player);
        long period = module.getConfig().getLong("scoreboard.update-tick", 2) * 20L;
        ScheduledTask task = player.getScheduler().runAtFixedRate(module.getPlugin(), t -> {
            if (!player.isOnline()) { t.cancel(); return; }
            tick(player);
        }, null, 1L, period);
        if (task != null) tasks.put(player.getUniqueId(), task);
    }

    public void stopFor(Player player) {
        ScheduledTask task = tasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
        if (active.remove(player.getUniqueId())) teardownObjective(player);
        currentLines.remove(player.getUniqueId());
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
            if (p != null) teardownObjective(p);
        });
        active.clear();
        currentLines.clear();
    }

    public void reloadAll() {
        Bukkit.getOnlinePlayers().forEach(p -> {
            stopFor(p);
            if (!disabled.contains(p.getUniqueId())) startFor(p);
        });
    }

    public void refreshWorld(Player player) {
        player.getScheduler().run(module.getPlugin(), t -> tick(player), null);
    }

    private void setupObjective(Player player) {
        String title = getWorldField(player.getWorld().getName(), "title");
        sendPacket(player, new WrapperPlayServerScoreboardObjective(
            OBJ,
            WrapperPlayServerScoreboardObjective.ObjectiveMode.CREATE,
            ColorUtil.parse(resolve(player, title)),
            WrapperPlayServerScoreboardObjective.RenderType.HEARTS
        ));
        sendPacket(player, new WrapperPlayServerDisplayScoreboard(1, OBJ));
        active.add(player.getUniqueId());
        currentLines.put(player.getUniqueId(), new ArrayList<>());
        applyLines(player, buildLines(player, getWorldLines(player.getWorld().getName())));
    }

    private void teardownObjective(Player player) {
        List<String> lines = currentLines.remove(player.getUniqueId());
        if (lines != null) {
            for (int i = 0; i < lines.size(); i++) {
                sendRemoveScore(player, ENTRIES[i]);
                sendRemoveTeam(player, i);
            }
        }
        sendPacket(player, new WrapperPlayServerScoreboardObjective(
            OBJ,
            WrapperPlayServerScoreboardObjective.ObjectiveMode.REMOVE,
            null, null
        ));
    }

    private void tick(Player player) {
        if (!active.contains(player.getUniqueId())) {
            setupObjective(player);
            return;
        }
        String title = getWorldField(player.getWorld().getName(), "title");
        sendPacket(player, new WrapperPlayServerScoreboardObjective(
            OBJ,
            WrapperPlayServerScoreboardObjective.ObjectiveMode.UPDATE,
            ColorUtil.parse(resolve(player, title)),
            WrapperPlayServerScoreboardObjective.RenderType.HEARTS
        ));
        applyLines(player, buildLines(player, getWorldLines(player.getWorld().getName())));
    }

    private void applyLines(Player player, List<String> newLines) {
        UUID uuid = player.getUniqueId();
        List<String> prev = currentLines.getOrDefault(uuid, new ArrayList<>());
        int prevSize = prev.size();
        int newSize = Math.min(newLines.size(), ENTRIES.length);

        if (prevSize != newSize) {

            for (int i = 0; i < prevSize; i++) {
                sendRemoveScore(player, ENTRIES[i]);
                sendRemoveTeam(player, i);
            }
            for (int i = 0; i < newSize; i++) {
                sendCreateTeam(player, i, ENTRIES[i], newLines.get(i));
                sendSetScore(player, ENTRIES[i], newSize - i);
            }
        } else {

            for (int i = 0; i < newSize; i++) {
                String newLine = newLines.get(i);
                if (i >= prevSize || !newLine.equals(prev.get(i))) {
                    sendUpdateTeam(player, i, newLine);
                }
            }
        }

        currentLines.put(uuid, new ArrayList<>(newLines.subList(0, newSize)));
    }

    private void sendCreateTeam(Player player, int index, String entry, String text) {
        Component prefix = text.isEmpty() ? Component.empty() : ColorUtil.parse(text);
        WrapperPlayServerTeams.ScoreBoardTeamInfo info = new WrapperPlayServerTeams.ScoreBoardTeamInfo(
            Component.empty(), prefix, Component.empty(),
            WrapperPlayServerTeams.NameTagVisibility.ALWAYS,
            WrapperPlayServerTeams.CollisionRule.ALWAYS,
            null, WrapperPlayServerTeams.OptionData.NONE
        );
        sendPacket(player, new WrapperPlayServerTeams(
            "arsb_" + index, WrapperPlayServerTeams.TeamMode.CREATE,
            Optional.of(info), Collections.singletonList(entry)
        ));
    }

    private void sendUpdateTeam(Player player, int index, String text) {
        Component prefix = text.isEmpty() ? Component.empty() : ColorUtil.parse(text);
        WrapperPlayServerTeams.ScoreBoardTeamInfo info = new WrapperPlayServerTeams.ScoreBoardTeamInfo(
            Component.empty(), prefix, Component.empty(),
            WrapperPlayServerTeams.NameTagVisibility.ALWAYS,
            WrapperPlayServerTeams.CollisionRule.ALWAYS,
            null, WrapperPlayServerTeams.OptionData.NONE
        );
        sendPacket(player, new WrapperPlayServerTeams(
            "arsb_" + index, WrapperPlayServerTeams.TeamMode.UPDATE,
            Optional.of(info), Collections.emptyList()
        ));
    }

    private void sendRemoveTeam(Player player, int index) {
        sendPacket(player, new WrapperPlayServerTeams(
            "arsb_" + index, WrapperPlayServerTeams.TeamMode.REMOVE,
            Optional.empty(), Collections.emptyList()
        ));
    }

    private void sendSetScore(Player player, String entry, int score) {
        sendPacket(player, new WrapperPlayServerUpdateScore(
            entry, WrapperPlayServerUpdateScore.Action.CREATE_OR_UPDATE_ITEM,
            OBJ, score, null, ScoreFormat.blankScore()
        ));
    }

    private void sendRemoveScore(Player player, String entry) {
        sendPacket(player, new WrapperPlayServerUpdateScore(
            entry, WrapperPlayServerUpdateScore.Action.REMOVE_ITEM,
            OBJ, 0, null, null
        ));
    }

    private void sendPacket(Player player, PacketWrapper<?> packet) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    private List<String> buildLines(Player player, List<String> rawLines) {
        List<String> result = new ArrayList<>();
        String teamFormat = module.getConfig().getString("scoreboard.team-line.format", "");
        String teamName = getTeamName(player);
        for (String line : rawLines) {
            if ("{team}".equals(line)) {
                if (teamName != null && !teamName.isEmpty()) {
                    String formatted = teamFormat.replace("%ariscore_team%", teamName);
                    result.add(resolve(player, formatted));
                }
            } else {
                result.add(resolve(player, line));
            }
        }
        return result;
    }

    private String getTeamName(Player player) {
        try {
            ArisCore core = ArisCore.getInstance();
            if (core != null && core.getTeamModule() != null) {
                return core.getTeamModule().getTeamManager().getPlayerTeamName(player.getUniqueId());
            }
        } catch (Throwable ignored) {}
        return null;
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
