package me.vennlmao.ariscore.home.commands;

import me.vennlmao.ariscore.home.HomeModule;
import me.vennlmao.ariscore.home.utils.MessageUtil;
import me.vennlmao.ariscore.home.utils.SoundUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class DelHomeCommand implements CommandExecutor, TabCompleter {

    private final HomeModule plugin;

    public DelHomeCommand(HomeModule plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length == 0) {
            SoundUtil.play(player, "error");
            return true;
        }

        String homeName = args[0];

        if (!plugin.getHomeManager().homeExists(player, homeName)) {
            SoundUtil.play(player, "error");
            MessageUtil.sendChat(player, "home_not_exist");
            MessageUtil.sendActionbar(player, "home_not_exist_ab");
            return true;
        }

        plugin.getHomeManager().deleteHome(player, homeName);
        SoundUtil.play(player, "deletehome");
        MessageUtil.sendChat(player, "deletehome", s -> s.replace("%home%", homeName));
        MessageUtil.sendActionbar(player, "deletehome_ab", s -> s.replace("%home%", homeName));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return List.of();
        if (args.length == 1) {
            return plugin.getHomeManager().getHomes(player).stream()
                    .filter(h -> h.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
