package me.vennlmao.ariscore.warp.commands;

import me.vennlmao.ariscore.warp.WarpModule;
import me.vennlmao.ariscore.warp.utils.MessageUtil;
import me.vennlmao.ariscore.warp.utils.SoundUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class DelWarpCommand implements CommandExecutor, TabCompleter {

    private final WarpModule module;

    public DelWarpCommand(WarpModule module) { this.module = module; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (!player.hasPermission("ariswarp.delwarp")) {
            SoundUtil.play(player, "error");
            MessageUtil.sendChat(player, "no_permission");
            return true;
        }
        if (args.length == 0) { player.sendMessage("§cUsage: /delwarp <name>"); return true; }
        String name = args[0];
        if (!module.getWarpManager().deleteWarp(name)) {
            SoundUtil.play(player, "error");
            MessageUtil.sendChat(player, "delwarp_not_exist", s -> s.replace("%name%", name));
            MessageUtil.sendActionbar(player, "delwarp_not_exist_ab", s -> s.replace("%name%", name));
            return true;
        }
        SoundUtil.play(player, "setwarp");
        MessageUtil.sendChat(player, "delwarp", s -> s.replace("%name%", name));
        MessageUtil.sendActionbar(player, "delwarp_ab", s -> s.replace("%name%", name));
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
