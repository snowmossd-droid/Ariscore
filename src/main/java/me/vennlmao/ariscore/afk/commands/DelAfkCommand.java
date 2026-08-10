package me.vennlmao.ariscore.afk.commands;

import me.vennlmao.ariscore.afk.AfkModule;
import me.vennlmao.ariscore.afk.utils.MessageUtil;
import me.vennlmao.ariscore.afk.utils.SoundUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class DelAfkCommand implements CommandExecutor, TabCompleter {

    private final AfkModule module;

    public DelAfkCommand(AfkModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (!player.hasPermission("arisafk.delafk")) {
            SoundUtil.play(player, "error");
            MessageUtil.sendChat(player, "no_permission");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUsage: /delafk <name>");
            return true;
        }

        String name = args[0];

        if (!module.getAfkManager().deleteZone(name)) {
            SoundUtil.play(player, "error");
            MessageUtil.sendChat(player, "delafk_not_exist", s -> s.replace("%name%", name));
            MessageUtil.sendActionbar(player, "delafk_not_exist_ab", s -> s.replace("%name%", name));
            return true;
        }

        SoundUtil.play(player, "setafk");
        MessageUtil.sendChat(player, "delafk", s -> s.replace("%name%", name));
        MessageUtil.sendActionbar(player, "delafk_ab", s -> s.replace("%name%", name));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return module.getAfkManager().getZoneNames().stream()
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
