package me.vennlmao.ariscore.home.commands;

import me.vennlmao.ariscore.home.HomeModule;
import me.vennlmao.ariscore.home.utils.MessageUtil;
import me.vennlmao.ariscore.home.utils.SoundUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class SetHomeCommand implements CommandExecutor {

    private final HomeModule plugin;

    public SetHomeCommand(HomeModule plugin) {
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

        List<String> blockedWorlds = plugin.getConfig().getStringList("blocked_worlds");
        if (blockedWorlds.contains(player.getWorld().getName())) {
            SoundUtil.play(player, "error");
            MessageUtil.sendChat(player, "world_blocked");
            MessageUtil.sendActionbar(player, "world_blocked_ab");
            return true;
        }

        int maxHomes = plugin.getHomeManager().getMaxHomes(player);
        java.util.List<String> homes = plugin.getHomeManager().getHomes(player);

        if (!plugin.getHomeManager().homeExists(player, homeName) && homes.size() >= maxHomes) {
            SoundUtil.play(player, "error");
            MessageUtil.sendChat(player, "max_homes_reached");
            MessageUtil.sendActionbar(player, "max_homes_reached_ab");
            return true;
        }

        plugin.getHomeManager().setHome(player, homeName);
        SoundUtil.play(player, "sethome");
        MessageUtil.sendChat(player, "sethome", s -> s.replace("%home%", homeName));
        MessageUtil.sendActionbar(player, "sethome_ab", s -> s.replace("%home%", homeName));
        return true;
    }
}
