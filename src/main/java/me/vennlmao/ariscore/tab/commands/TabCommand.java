package me.vennlmao.ariscore.tab.commands;

import me.vennlmao.ariscore.tab.TabModule;
import me.vennlmao.ariscore.tab.managers.PapiManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TabCommand implements CommandExecutor, TabCompleter {

    private final TabModule module;

    public TabCommand(TabModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ariscore.tab.admin")) {
            sender.sendMessage(PapiManager.colorize("&cNo permission."));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            module.reload();
            sender.sendMessage(PapiManager.colorize("&aTab module reloaded!"));
            return true;
        }
        sender.sendMessage(PapiManager.colorize("&eUsage: /tab reload"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("ariscore.tab.admin")) return Collections.emptyList();
        if (args.length == 1) return Arrays.asList("reload").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        return Collections.emptyList();
    }
}
