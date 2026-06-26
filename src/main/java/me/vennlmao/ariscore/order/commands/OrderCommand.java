package me.vennlmao.ariscore.order.commands;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.order.managers.OrderConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class OrderCommand implements CommandExecutor, TabCompleter {

    private final ArisCore plugin;

    public OrderCommand(ArisCore plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        OrderConfigManager cfg = plugin.getOrderModule().getConfigManager();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(cfg.msg("messages.player-only"));
            return true;
        }

        if (!player.hasPermission("ariscore.order.use")) {
            player.sendMessage(cfg.msg("messages.no-permission"));
            return true;
        }

        if (args.length == 0) {
            plugin.getOrderModule().getOrderViewGUI().open(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "my", "mine", "yours", "your" -> plugin.getOrderModule().getYourOrdersGUI().open(player);
            case "new", "create" -> plugin.getOrderModule().getListMaterialsGUI().open(player);
            case "reload" -> {
                if (!player.hasPermission("ariscore.order.admin")) {
                    player.sendMessage(cfg.msg("messages.no-permission"));
                    return true;
                }
                plugin.getOrderModule().reload();
                player.sendMessage(cfg.msg("messages.reload-success"));
            }
            default -> player.sendMessage(cfg.msg("messages.unknown-command"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();
        if (!player.hasPermission("ariscore.order.use")) return Collections.emptyList();
        if (args.length == 1) {
            List<String> subs = Arrays.asList("my", "new", "reload");
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
