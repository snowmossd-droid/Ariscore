package me.vennlmao.ariscore.tab.managers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.clip.placeholderapi.PlaceholderAPI;
import me.vennlmao.ariscore.tab.TabModule;
import me.vennlmao.ariscore.tab.utils.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class TabManager {

    private final TabModule module;
    private ScheduledTask globalTask;
    private final Map<UUID, Scoreboard> playerBoards = new HashMap<>();

    public TabManager(TabModule module) {
        this.module = module;
        startGlobalTask();
        hookLuckPerms();
    }

    private void startGlobalTask() {
        if (globalTask != null) globalTask.cancel();
        long period = module.getConfig().getLong("tab.update-tick", 2) * 20L;
        globalTask = module.getPlugin().getServer().getGlobalRegionScheduler().runAtFixedRate(
            module.getPlugin(), t -> Bukkit.getOnlinePlayers().forEach(this::updateTab), 1L, period
        );
    }

    private void hookLuckPerms() {
        try {
            org.bukkit.plugin.Plugin lp = module.getPlugin().getServer().getPluginManager().getPlugin("LuckPerms");
            if (lp == null) return;
            net.luckperms.api.LuckPerms api = net.luckperms.api.LuckPermsProvider.get();
            api.getEventBus().subscribe(module.getPlugin(),
                net.luckperms.api.event.user.UserDataRecalculateEvent.class, event -> {
                    UUID uuid = event.getUser().getUniqueId();
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        player.getScheduler().run(module.getPlugin(), t -> {
                            applyLpName(player);
                            Scoreboard board = playerBoards.get(uuid);
                            if (board != null) rebuildSortingTeam(board, player);
                        }, null);
                    }
                });
        } catch (Throwable ignored) {
        }
    }

    public void setupFor(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        playerBoards.put(player.getUniqueId(), board);
        player.setScoreboard(board);
        applyLpName(player);
        applySortingTeams(player, board);
        updateTab(player);
    }

    public void removeAll() {
        if (globalTask != null) globalTask.cancel();
        playerBoards.clear();
    }

    public void reloadAll() {
        startGlobalTask();
        Bukkit.getOnlinePlayers().forEach(p -> {
            playerBoards.remove(p.getUniqueId());
            setupFor(p);
        });
    }

    public void onPlayerJoin(Player joining) {
        setupFor(joining);
        UUID joiningUuid = joining.getUniqueId();
        Bukkit.getOnlinePlayers().forEach(viewer -> {
            if (!viewer.getUniqueId().equals(joiningUuid)) {
                Scoreboard board = playerBoards.get(viewer.getUniqueId());
                if (board != null) addSortingTeam(board, joining);
            }
        });
    }

    public void onPlayerQuit(Player quitting) {
        UUID quittingUuid = quitting.getUniqueId();
        playerBoards.remove(quittingUuid);
        Bukkit.getOnlinePlayers().forEach(viewer -> {
            if (!viewer.getUniqueId().equals(quittingUuid)) {
                Scoreboard board = playerBoards.get(viewer.getUniqueId());
                if (board != null) removeSortingTeam(board, quitting);
            }
        });
    }

    public void applySortingTeams(Player viewer, Scoreboard board) {
        if (!module.getConfig().getBoolean("luckperms-sorting.enabled", false)) return;
        Bukkit.getOnlinePlayers().forEach(p -> addSortingTeam(board, p));
    }

    private void addSortingTeam(Scoreboard board, Player player) {
        if (!module.getConfig().getBoolean("luckperms-sorting.enabled", false)) return;
        removeSortingTeam(board, player);
        List<String> groups = module.getConfig().getStringList("luckperms-sorting.groups");
        String group = resolve(player, "%luckperms_primary_group%");
        int rank = groups.indexOf(group);
        if (rank < 0) rank = groups.size();
        String teamName = "lp_" + String.format("%03d", rank) + "_" + player.getName();
        if (teamName.length() > 16) teamName = teamName.substring(0, 16);
        Team team = board.getTeam(teamName);
        if (team == null) team = board.registerNewTeam(teamName);
        if (!team.hasEntry(player.getName())) team.addEntry(player.getName());
    }

    private void rebuildSortingTeam(Scoreboard board, Player player) {
        addSortingTeam(board, player);
    }

    private void removeSortingTeam(Scoreboard board, Player player) {
        for (Team team : new HashSet<>(board.getTeams())) {
            if (team.getName().startsWith("lp_") && team.hasEntry(player.getName())) {
                team.removeEntry(player.getName());
                if (team.getEntries().isEmpty()) team.unregister();
            }
        }
    }

    private void updateTab(Player player) {
        if (!module.getConfig().getBoolean("tab.enabled", true)) return;
        List<String> rawHeader = module.getConfig().getStringList("tab.default.header");
        List<String> rawFooter = module.getConfig().getStringList("tab.default.footer");
        player.sendPlayerListHeaderAndFooter(buildMultiline(player, rawHeader), buildMultiline(player, rawFooter));
    }

    private Component buildMultiline(Player player, List<String> lines) {
        if (lines.isEmpty()) return Component.empty();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            sb.append(resolve(player, lines.get(i)));
            if (i < lines.size() - 1) sb.append("\n");
        }
        return ColorUtil.parse(sb.toString());
    }

    public void applyLpName(Player player) {
        if (!module.getConfig().getBoolean("luckperms-name.enabled", false)) return;
        String format = module.getConfig().getString("luckperms-name.format", "%player_name%");
        player.playerListName(ColorUtil.parse(resolve(player, format)));
    }

    private String resolve(Player player, String text) {
        if (text == null) return "";
        if (module.getPlugin().getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return PlaceholderAPI.setPlaceholders(player, text);
        }
        return text;
    }
                                                  }
            
