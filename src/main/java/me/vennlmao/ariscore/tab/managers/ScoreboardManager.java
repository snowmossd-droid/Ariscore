package me.vennlmao.ariscore.tab.managers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.clip.placeholderapi.PlaceholderAPI;
import me.vennlmao.ariscore.tab.TabModule;
import me.vennlmao.ariscore.tab.utils.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.*;

public class ScoreboardManager {

    private static final String[] ENTRIES = {
        "§0","§1","§2","§3","§4","§5","§6","§7",
        "§8","§9","§a","§b","§c","§d","§e","§f",
        "§0§0","§1§1","§2§2","§3§3","§4§4","§5§5","§6§6","§7§7",
        "§8§8","§9§9","§a§a","§b§b","§c§c","§d§d","§e§e","§f§f"
    };

    private final TabModule module;
    private final Map<UUID, ScheduledTask> tasks = new HashMap<>();
    private final Set<UUID> disabled = new HashSet<>();

    public ScoreboardManager(TabModule module) {
        this.module = module;
    }

    public void startFor(Player player) {
        if (disabled.contains(player.getUniqueId())) return;
        setupBoard(player);
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
    }

    public void toggle(Player player) {
        UUID uuid = player.getUniqueId();
        if (disabled.contains(uuid)) {
            disabled.remove(uuid);
            startFor(player);
        } else {
            disabled.add(uuid);
            stopFor(player);
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    public boolean isDisabled(Player player) {
        return disabled.contains(player.getUniqueId());
    }

    public void removeAll() {
        new ArrayList<>(tasks.values()).forEach(ScheduledTask::cancel);
        tasks.clear();
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

    private void setupBoard(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        String title = getWorldField(player.getWorld().getName(), "title", "&lDonutSMP");
        Objective obj = board.registerNewObjective("sb", Criteria.DUMMY, ColorUtil.parse(resolve(player, title)));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        if (module.getConfig().getBoolean("belowname.enabled", false)) {
            String bnTitle = module.getConfig().getString("belowname.title", "&c❤");
            Objective bn = board.registerNewObjective("bn", Criteria.HEALTH, ColorUtil.parse(bnTitle));
            bn.setDisplaySlot(DisplaySlot.BELOW_NAME);
        }
        module.getTabManager().applySortingTeams(player, board);
        player.setScoreboard(board);
        renderLines(player, board, obj);
    }

    private void updateBoard(Player player) {
        Scoreboard board = player.getScoreboard();
        Objective obj = board.getObjective("sb");
        if (obj == null) { setupBoard(player); return; }
        String title = getWorldField(player.getWorld().getName(), "title", "&lDonutSMP");
        obj.displayName(ColorUtil.parse(resolve(player, title)));
        renderLines(player, board, obj);
    }

    private void renderLines(Player player, Scoreboard board, Objective obj) {
        List<String> lines = buildLines(player, getWorldLines(player.getWorld().getName()));
        for (String entry : new HashSet<>(board.getEntries())) {
            if (entry.startsWith("§") && entry.length() <= 4) board.resetScores(entry);
        }
        for (Team team : new HashSet<>(board.getTeams())) {
            if (team.getName().startsWith("line_")) team.unregister();
        }
        for (int i = 0; i < Math.min(lines.size(), ENTRIES.length); i++) {
            String entry = ENTRIES[i];
            Team team = board.registerNewTeam("line_" + i);
            team.addEntry(entry);
            team.prefix(lines.get(i).isEmpty() ? Component.empty() : ColorUtil.parse(lines.get(i)));
            team.suffix(Component.empty());
            obj.getScore(entry).setScore(lines.size() - i);
        }
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
                if (w.equals(worldName)) {
                    return module.getConfig().getStringList("scoreboard.worlds." + key + ".lines");
                }
            }
        }
        return module.getConfig().getStringList("scoreboard.default.lines");
    }

    private String getWorldField(String worldName, String field, String def) {
        ConfigurationSection worlds = module.getConfig().getConfigurationSection("scoreboard.worlds");
        if (worlds != null) {
            for (String key : worlds.getKeys(false)) {
                String w = module.getConfig().getString("scoreboard.worlds." + key + ".world", "");
                if (w.equals(worldName)) {
                    return module.getConfig().getString("scoreboard.worlds." + key + "." + field, def);
                }
            }
        }
        return module.getConfig().getString("scoreboard.default." + field, def);
    }

    private String resolve(Player player, String text) {
        if (text == null) return "";
        if (module.getPlugin().getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return PlaceholderAPI.setPlaceholders(player, text);
        }
        return text;
    }
}
