package me.vennlmao.ariscore.commands;

import me.vennlmao.ariscore.ArisCore;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ArisCoreReloadCommand implements CommandExecutor, TabCompleter {

    private final ArisCore plugin;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public ArisCoreReloadCommand(ArisCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("ariscore.reload")) {
            sender.sendMessage(MM.deserialize("<red>No permission."));
            return true;
        }

        String module = args.length > 0 ? args[0].toLowerCase() : "all";

        switch (module) {
            case "tpa"    -> { plugin.getTpaModule().reload();    sender.sendMessage(MM.deserialize("<green>TPA module reloaded.")); }
            case "home"   -> { plugin.getHomeModule().reload();   sender.sendMessage(MM.deserialize("<green>Home module reloaded.")); }
            case "shop"   -> { plugin.getShopModule().reload();   sender.sendMessage(MM.deserialize("<green>Shop module reloaded.")); }
            case "shards" -> { plugin.getShardsModule().reload(); sender.sendMessage(MM.deserialize("<green>Shards module reloaded.")); }
            case "team"   -> { plugin.getTeamModule().reload();   sender.sendMessage(MM.deserialize("<green>Team module reloaded.")); }
            case "auction"-> { plugin.getAuctionModule().reload();sender.sendMessage(MM.deserialize("<green>Auction module reloaded.")); }
            case "sell"   -> { plugin.getSellModule().reload();   sender.sendMessage(MM.deserialize("<green>Sell module reloaded.")); }
            case "tab"    -> { plugin.getTabModule().reload();    sender.sendMessage(MM.deserialize("<green>Tab module reloaded.")); }
            case "order"  -> { plugin.getOrderModule().reload();  sender.sendMessage(MM.deserialize("<green>Order module reloaded.")); }
            case "spawn"  -> { plugin.getSpawnModule().reload();  sender.sendMessage(MM.deserialize("<green>Spawn module reloaded.")); }
            case "afk"    -> { plugin.getAfkModule().reload();    sender.sendMessage(MM.deserialize("<green>AFK module reloaded.")); }
            default -> {
                plugin.getTpaModule().reload();
                plugin.getHomeModule().reload();
                plugin.getShopModule().reload();
                plugin.getShardsModule().reload();
                plugin.getTeamModule().reload();
                plugin.getAuctionModule().reload();
                plugin.getSellModule().reload();
                plugin.getTabModule().reload();
                plugin.getOrderModule().reload();
                plugin.getSpawnModule().reload();
                plugin.getAfkModule().reload();
                sender.sendMessage(MM.deserialize("<green>All modules reloaded."));
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("all", "tpa", "home", "shop", "shards", "team", "auction", "sell", "tab", "order", "spawn", "afk").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
