package me.vennlmao.ariscore.auction.commands;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.auction.managers.AuctionConfigManager;
import me.vennlmao.ariscore.auction.managers.LangManager;
import me.vennlmao.ariscore.auction.utils.EcoUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AuctionCommand implements CommandExecutor, TabCompleter {

    private final ArisCore plugin;

    public AuctionCommand(ArisCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LangManager lang = plugin.getAuctionModule().getLangManager();
        AuctionConfigManager cfg = plugin.getAuctionModule().getConfigManager();

        if (args.length == 0) {
            if (!(sender instanceof Player)) { sender.sendMessage(lang.getPlayerOnly()); return true; }
            plugin.getAuctionModule().getAuctionGUI().open((Player) sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "sell": {
                if (!(sender instanceof Player)) { sender.sendMessage(lang.getPlayerOnly()); return true; }
                Player player = (Player) sender;
                if (!player.hasPermission("ariscore.auction.sell")) { lang.send(player, "no-permission"); return true; }
                if (args.length < 2) { player.sendMessage(lang.getHelp().toString()); return true; }

                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand == null || hand.getType().isAir()) { lang.send(player, "no-item-in-hand"); return true; }

                Double price = EcoUtil.parsePrice(args[1], cfg);
                if (price == null) { lang.send(player, "invalid-price"); return true; }

                if (cfg.isBlacklisted(hand.getType())) {
                    lang.send(player, lang.getBlacklistedItem(lang.formatItemName(hand)));
                    cfg.playSound(player, "blacklisted");
                    return true;
                }

                boolean fastSell = plugin.getAuctionModule().getPlayerDataManager().getFastSell(player.getUniqueId());
                if (fastSell) {
                    String error = plugin.getAuctionModule().getAuctionManager().createAuction(player, hand, price);
                    if (error != null) { lang.send(player, error); return true; }
                    player.getInventory().setItemInMainHand(null);
                    lang.send(player, "auction-created", "%price%", EcoUtil.format(price, true, cfg));
                    cfg.playSound(player, "item-sold");
                } else {
                    plugin.getAuctionModule().getConfirmListingGUI().open(player, hand, price);
                }
                return true;
            }

            case "view": {
                if (!(sender instanceof Player)) { sender.sendMessage(lang.getPlayerOnly()); return true; }
                Player player = (Player) sender;
                if (args.length < 2) { player.sendMessage(lang.getHelp().toString()); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { lang.send(player, "item-not-found"); return true; }
                plugin.getAuctionModule().getAuctionGUI().open(player, 1, null, null);
                return true;
            }

            case "search": {
                if (!(sender instanceof Player)) { sender.sendMessage(lang.getPlayerOnly()); return true; }
                Player player = (Player) sender;
                if (args.length < 2) { player.sendMessage(lang.getHelp().toString()); return true; }
                String term = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                plugin.getAuctionModule().getAuctionGUI().open(player, 1, term, null);
                return true;
            }

            case "reload": {
                if (!sender.hasPermission("ariscore.auction.admin")) { sender.sendMessage(lang.getNoPermission()); return true; }
                plugin.getAuctionModule().reload();
                if (sender instanceof Player) {
                    lang.send((Player) sender, "config-reloaded");
                    cfg.playSound((Player) sender, "reload");
                } else {
                    sender.sendMessage(lang.getConfigReloaded());
                }
                return true;
            }

            case "help": {
                if (sender.hasPermission("ariscore.auction.admin")) {
                    lang.getAdminHelp().forEach(sender::sendMessage);
                } else {
                    lang.getHelp().forEach(sender::sendMessage);
                }
                return true;
            }

            default:
                lang.getHelp().forEach(sender::sendMessage);
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return Arrays.asList("sell", "search", "view", "help", "reload");
        return Collections.emptyList();
    }
}
