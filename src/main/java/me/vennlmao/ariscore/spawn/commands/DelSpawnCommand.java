package me.vennlmao.ariscore.spawn.commands;

import me.vennlmao.ariscore.spawn.SpawnModule;
import me.vennlmao.ariscore.spawn.utils.MessageUtil;
import me.vennlmao.ariscore.spawn.utils.SoundUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class DelSpawnCommand implements CommandExecutor, TabCompleter {

    private final SpawnModule module;

    public DelSpawnCommand(SpawnModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (!player.hasPermission("arisspawn.delspawn")) {
            SoundUtil.play(player, "error");
            MessageUtil.sendChat(player, "no_permission");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUsage: /delspawn <name>");
            return true;
        }

        String name = args[0];

        if (!module.getSpawnManager().deleteSpawn(name)) {
            SoundUtil.play(player, "error");
            MessageUtil.sendChat(player, "delspawn_not_exist", s -> s.replace("%name%", name));
            MessageUtil.sendActionbar(player, "delspawn_not_exist_ab", s -> s.replace("%name%", name));
            return true;
        }

        SoundUtil.play(player, "setspawn");
        MessageUtil.sendChat(player, "delspawn", s -> s.replace("%name%", name));
        MessageUtil.sendActionbar(player, "delspawn_ab", s -> s.replace("%name%", name));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return module.getSpawnManager().getSpawnNames().stream()
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
