package me.vennlmao.ariscore.sell.commands;

import me.vennlmao.ariscore.sell.SellModule;
import me.vennlmao.ariscore.sell.managers.SellWandManager;
import me.vennlmao.ariscore.sell.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SellWandCommand implements CommandExecutor, TabCompleter {

    private final SellModule module;

    public SellWandCommand(SellModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ariscore.sell.sellwand")) {
            sender.sendMessage(ColorUtil.colorize(module.getConfig().getString("sellwand.no-permission")));
            return true;
        }
        if (args.length < 4 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(ColorUtil.colorize(module.getConfig().getString("sellwand.usage")));
            sender.sendMessage(ColorUtil.colorize(module.getConfig().getString("sellwand.usage-types")));
            sender.sendMessage(ColorUtil.colorize(module.getConfig().getString("sellwand.usage-example-uses")));
            sender.sendMessage(ColorUtil.colorize(module.getConfig().getString("sellwand.usage-example-time")));
            return true;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ColorUtil.colorize(module.getConfig().getString("sellwand.player-not-found")));
            return true;
        }
        String typeStr = args[2].toLowerCase();
        SellWandManager.WandType type;
        long value;
        if (typeStr.equals("uses")) {
            type = SellWandManager.WandType.USES;
            try {
                value = Long.parseLong(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ColorUtil.colorize(module.getConfig().getString("sellwand.invalid-uses")));
                return true;
            }
        } else if (typeStr.equals("time")) {
            type = SellWandManager.WandType.TIME;
            value = module.getWandManager().parseTime(args[3]);
            if (value <= 0L) {
                sender.sendMessage(ColorUtil.colorize(module.getConfig().getString("sellwand.invalid-time")));
                return true;
            }
        } else {
            sender.sendMessage(ColorUtil.colorize(module.getConfig().getString("sellwand.invalid-type")));
            return true;
        }
        ItemStack wand = module.getWandManager().createWand(type, value);
        target.getInventory().addItem(wand);
        sender.sendMessage(ColorUtil.colorize(module.getConfig().getString("sellwand.given")
                .replace("%player%", target.getName())
                .replace("%type%", type.name())
                .replace("%value%", args[3])));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("ariscore.sell.sellwand")) return new ArrayList<>();
        if (args.length == 1) return Arrays.asList("give").stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2) return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        if (args.length == 3) return Arrays.asList("uses", "time").stream().filter(s -> s.startsWith(args[2].toLowerCase())).collect(Collectors.toList());
        if (args.length == 4) {
            if (args[2].equalsIgnoreCase("uses")) return Arrays.asList("100", "500", "1000", "-1");
            if (args[2].equalsIgnoreCase("time")) return Arrays.asList("1d", "3d", "7d", "30d");
        }
        return new ArrayList<>();
    }
}
