package me.vennlmao.ariscore.shop.commands;

import me.vennlmao.ariscore.shop.ShopModule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ShopCommand implements CommandExecutor {

    private final ShopModule plugin;

    public ShopCommand(ShopModule plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;
        player.getScheduler().run(plugin.getPlugin(), t ->
                plugin.getShopListener().openShop(player), null);
        return true;
    }
}
