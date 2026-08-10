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
            case "tpa"    -> reloadOne(sender, "tpa", "TPA", plugin.getTpaModule());
            case "home"   -> reloadOne(sender, "home", "Home", plugin.getHomeModule());
            case "shop"   -> reloadOne(sender, "shop", "Shop", plugin.getShopModule());
            case "shards" -> reloadOne(sender, "shards", "Shards", plugin.getShardsModule());
            case "team"   -> reloadOne(sender, "team", "Team", plugin.getTeamModule());
            case "auction"-> reloadOne(sender, "auction", "Auction", plugin.getAuctionModule());
            case "sell"   -> reloadOne(sender, "sell", "Sell", plugin.getSellModule());
            case "tab"    -> reloadOne(sender, "tab", "Tab", plugin.getTabModule());
            case "order"  -> reloadOne(sender, "order", "Order", plugin.getOrderModule());
            case "spawn"  -> reloadOne(sender, "spawn", "Spawn", plugin.getSpawnModule());
            case "afk"    -> reloadOne(sender, "afk", "AFK", plugin.getAfkModule());
            case "warp"   -> reloadOne(sender, "warp", "Warp", plugin.getWarpModule());
            case "crates" -> reloadOne(sender, "crates", "Crates", plugin.getCratesModule());
            case "amethyst"-> reloadOne(sender, "amethyst", "Amethyst", plugin.getAmethystModule());
            case "duel"   -> reloadOne(sender, "duel", "Duel", plugin.getDuelModule());
            case "spawners" -> reloadOne(sender, "spawners", "Spawners", plugin.getSpawnersModule());
            default -> {
                reloadOne(sender, "tpa", "TPA", plugin.getTpaModule());
                reloadOne(sender, "home", "Home", plugin.getHomeModule());
                reloadOne(sender, "shop", "Shop", plugin.getShopModule());
                reloadOne(sender, "shards", "Shards", plugin.getShardsModule());
                reloadOne(sender, "team", "Team", plugin.getTeamModule());
                reloadOne(sender, "auction", "Auction", plugin.getAuctionModule());
                reloadOne(sender, "sell", "Sell", plugin.getSellModule());
                reloadOne(sender, "tab", "Tab", plugin.getTabModule());
                reloadOne(sender, "order", "Order", plugin.getOrderModule());
                reloadOne(sender, "spawn", "Spawn", plugin.getSpawnModule());
                reloadOne(sender, "afk", "AFK", plugin.getAfkModule());
                reloadOne(sender, "warp", "Warp", plugin.getWarpModule());
                reloadOne(sender, "crates", "Crates", plugin.getCratesModule());
                reloadOne(sender, "amethyst", "Amethyst", plugin.getAmethystModule());
                reloadOne(sender, "duel", "Duel", plugin.getDuelModule());
                reloadOne(sender, "spawners", "Spawners", plugin.getSpawnersModule());
                sender.sendMessage(MM.deserialize("<green>All modules reloaded."));
            }
        }
        return true;
    }

    private void reloadOne(CommandSender sender, String key, String label, Object moduleInstance) {
        if (!plugin.isModuleEnabled(key)) {
            sender.sendMessage(MM.deserialize("<yellow>" + label + " module is disabled in config.yml, skipped."));
            return;
        }
        try {
            moduleInstance.getClass().getMethod("reload").invoke(moduleInstance);
            sender.sendMessage(MM.deserialize("<green>" + label + " module reloaded."));
        } catch (Exception e) {
            sender.sendMessage(MM.deserialize("<red>Failed to reload " + label + " module."));
            plugin.getLogger().warning("[ArisCore] Failed to reload " + label + " module: " + e.getMessage());
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("all", "tpa", "home", "shop", "shards", "team", "auction", "sell", "tab", "order", "spawn", "afk", "warp", "crates", "amethyst", "duel", "spawners").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
