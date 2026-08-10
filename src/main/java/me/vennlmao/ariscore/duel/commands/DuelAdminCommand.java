package me.vennlmao.ariscore.duel.commands;

import me.vennlmao.ariscore.duel.DuelModule;
import me.vennlmao.ariscore.duel.utils.MessageUtil;
import me.vennlmao.ariscore.duel.utils.SoundUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class DuelAdminCommand implements CommandExecutor, TabCompleter {

    private final DuelModule module;
    private final Map<UUID, Location> pendingPos1 = new HashMap<>();
    private final Map<UUID, Location> pendingPos2 = new HashMap<>();

    public DuelAdminCommand(DuelModule module) { this.module = module; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (!player.hasPermission("arisduel.admin")) {
            MessageUtil.sendChat(player, "no_permission");
            return true;
        }

        if (args.length == 0) {
            MessageUtil.sendChat(player, "usage_arisduel");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set" -> {
                if (args.length < 2 || (!args[1].equals("1") && !args[1].equals("2"))) {
                    MessageUtil.sendChat(player, "usage_arisduel_set");
                    return true;
                }
                Location loc = player.getLocation().clone();
                if (args[1].equals("1")) {
                    pendingPos1.put(player.getUniqueId(), loc);
                    MessageUtil.sendChat(player, "arena_pos1_set");
                } else {
                    pendingPos2.put(player.getUniqueId(), loc);
                    MessageUtil.sendChat(player, "arena_pos2_set");
                }
                SoundUtil.play(player, "success");
            }
            case "create" -> {
                if (args.length < 2) {
                    MessageUtil.sendChat(player, "usage_arisduel_create");
                    return true;
                }
                String name = args[1];
                Location pos1 = pendingPos1.get(player.getUniqueId());
                Location pos2 = pendingPos2.get(player.getUniqueId());
                if (pos1 == null || pos2 == null) {
                    MessageUtil.sendChat(player, "arena_positions_missing");
                    SoundUtil.play(player, "error");
                    return true;
                }
                if (pos1.getWorld() == null || pos2.getWorld() == null || !pos1.getWorld().equals(pos2.getWorld())) {
                    MessageUtil.sendChat(player, "arena_world_mismatch");
                    SoundUtil.play(player, "error");
                    return true;
                }
                if (module.getArenaManager().exists(name)) {
                    MessageUtil.sendChat(player, "arena_name_taken");
                    SoundUtil.play(player, "error");
                    return true;
                }
                module.getArenaManager().createArena(name, pos1, pos2);
                pendingPos1.remove(player.getUniqueId());
                pendingPos2.remove(player.getUniqueId());
                MessageUtil.sendChat(player, "arena_created", s -> s.replace("{arena}", name));
                SoundUtil.play(player, "success");
            }
            case "delete" -> {
                if (args.length < 2) {
                    MessageUtil.sendChat(player, "usage_arisduel_delete");
                    return true;
                }
                String name = args[1];
                if (!module.getArenaManager().deleteArena(name)) {
                    MessageUtil.sendChat(player, "arena_not_found");
                    SoundUtil.play(player, "error");
                    return true;
                }
                MessageUtil.sendChat(player, "arena_deleted", s -> s.replace("{arena}", name));
                SoundUtil.play(player, "success");
            }
            case "list" -> {
                List<String> names = module.getArenaManager().getArenaNames();
                MessageUtil.sendChat(player, "arena_list", s -> s.replace("{arenas}", names.isEmpty() ? "-" : String.join(", ", names)));
            }
            default -> MessageUtil.sendChat(player, "usage_arisduel");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("set", "create", "delete", "list").stream()
                    .filter(o -> o.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("set")) return List.of("1", "2");
            if (args[0].equalsIgnoreCase("delete")) return module.getArenaManager().getArenaNames();
        }
        return List.of();
    }
}
