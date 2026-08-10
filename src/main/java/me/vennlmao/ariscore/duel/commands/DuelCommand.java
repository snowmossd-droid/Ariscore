package me.vennlmao.ariscore.duel.commands;

import me.vennlmao.ariscore.duel.DuelModule;
import me.vennlmao.ariscore.duel.managers.DuelStats;
import me.vennlmao.ariscore.duel.utils.MessageUtil;
import me.vennlmao.ariscore.duel.utils.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DuelCommand implements CommandExecutor, TabCompleter {

    private final DuelModule module;

    public DuelCommand(DuelModule module) { this.module = module; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length == 0) {
            MessageUtil.sendChat(player, "usage_duel");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "accept" -> {
                if (!module.getSessionManager().acceptInvite(player)) {
                    MessageUtil.sendBoth(player, "no_pending_invite");
                    SoundUtil.play(player, "error");
                }
            }
            case "deny" -> {
                if (!module.getSessionManager().denyInvite(player)) {
                    MessageUtil.sendBoth(player, "no_pending_invite");
                    SoundUtil.play(player, "error");
                }
            }
            case "stats" -> {
                OfflinePlayer target = args.length >= 2 ? Bukkit.getOfflinePlayer(args[1]) : player;
                sendStats(player, target);
            }
            default -> {
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null || !target.isOnline()) {
                    MessageUtil.sendBoth(player, "player_not_found");
                    SoundUtil.play(player, "error");
                    return true;
                }
                if (target.getUniqueId().equals(player.getUniqueId())) {
                    MessageUtil.sendBoth(player, "cannot_duel_self");
                    SoundUtil.play(player, "error");
                    return true;
                }
                if (module.getSessionManager().isBusy(player.getUniqueId())) {
                    MessageUtil.sendBoth(player, "already_busy");
                    SoundUtil.play(player, "error");
                    return true;
                }
                if (module.getSessionManager().isBusy(target.getUniqueId())) {
                    MessageUtil.sendBoth(player, "target_busy");
                    SoundUtil.play(player, "error");
                    return true;
                }
                if (!module.getArenaManager().hasArenas()) {
                    MessageUtil.sendBoth(player, "no_arenas");
                    SoundUtil.play(player, "error");
                    return true;
                }
                module.getGuiListener().openCreateDuel(player, target);
            }
        }
        return true;
    }

    private void sendStats(Player viewer, OfflinePlayer target) {
        DuelStats stats = module.getStatsManager().getStats(target.getUniqueId());
        String name = target.getName() != null ? target.getName() : "Unknown";
        MessageUtil.sendChat(viewer, "stats", s -> s
                .replace("{player}", name)
                .replace("{wins}", String.valueOf(stats.getWins()))
                .replace("{losses}", String.valueOf(stats.getLosses()))
                .replace("{draws}", String.valueOf(stats.getDraws()))
                .replace("{streak}", String.valueOf(stats.getStreak()))
                .replace("{best-streak}", String.valueOf(stats.getBestStreak()))
                .replace("{winrate}", String.format("%.1f", stats.getWinRate())));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("accept", "deny", "stats"));
            for (Player p : Bukkit.getOnlinePlayers()) options.add(p.getName());
            return options.stream().filter(o -> o.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        return List.of();
    }
}
