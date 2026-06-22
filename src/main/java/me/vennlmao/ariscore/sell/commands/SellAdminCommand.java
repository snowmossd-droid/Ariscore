package me.vennlmao.ariscore.sell.commands;

import me.vennlmao.ariscore.sell.SellModule;
import me.vennlmao.ariscore.sell.utils.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SellAdminCommand implements CommandExecutor, TabCompleter {

    private final SellModule module;

    public SellAdminCommand(SellModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ariscore.sell.admin")) {
            sender.sendMessage(ColorUtil.colorize(module.getConfig().getString("no-permission")));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            module.reload();
            sender.sendMessage(ColorUtil.colorize(module.getConfig().getString("reload-success")));
            return true;
        }
        sender.sendMessage(ColorUtil.colorize("&cUsage: /selladmin reload"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("ariscore.sell.admin")) return new ArrayList<>();
        if (args.length == 1) return Arrays.asList("reload").stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        return new ArrayList<>();
    }
}
