package me.vennlmao.ariscore.home.commands;

import me.vennlmao.ariscore.home.HomeModule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HomesCommand implements CommandExecutor {

    private final HomeModule plugin;

    public HomesCommand(HomeModule plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;
        plugin.getHomesListener().openHomesGui(player);
        return true;
    }
}
