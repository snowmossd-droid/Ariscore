package me.vennlmao.ariscore.warp.commands;

import me.vennlmao.ariscore.warp.WarpModule;
import me.vennlmao.ariscore.warp.utils.MessageUtil;
import me.vennlmao.ariscore.warp.utils.SoundUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class WarpCommand implements CommandExecutor, TabCompleter {

    private final WarpModule module;

    public WarpCommand(WarpModule module) { this.module = module; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length == 0) {
            if (module.getWarpManager().getAllWarps().isEmpty()) {
                SoundUtil.play(player, "error");
                MessageUtil.sendChat(player, "no_warps");
                MessageUtil.sendActionbar(player, "no_warps_ab");
                return true;
            }
            module.getGuiListener().openWarpsGui(player);
            return true;
        }

        String name = args[0];
        if (!module.getWarpManager().warpExists(name)) {
            SoundUtil.play(player, "error");
            MessageUtil.sendChat(player, "warp_not_exist", s -> s.replace("%name%", name));
            MessageUtil.sendActionbar(player, "warp_not_exist_ab", s -> s.replace("%name%", name));
            return true;
        }

        Location loc = module.getWarpManager().getWarp(name);
        if (loc == null || loc.getWorld() == null) {
            SoundUtil.play(player, "error");
            MessageUtil.sendChat(player, "world_not_found");
            MessageUtil.sendActionbar(player, "world_not_found_ab");
            return true;
        }

        module.getWarmupManager().startWarmup(player, name, loc);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1)
            return module.getWarpManager().getWarpNames().stream()
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        return List.of();
    }
}
