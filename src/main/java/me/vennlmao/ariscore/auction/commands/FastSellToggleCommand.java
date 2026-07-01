package me.vennlmao.ariscore.auction.commands;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.auction.managers.LangManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FastSellToggleCommand implements CommandExecutor {

    private final ArisCore plugin;

    public FastSellToggleCommand(ArisCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LangManager lang = plugin.getAuctionModule().getLangManager();
        if (!(sender instanceof Player)) { sender.sendMessage(lang.getPlayerOnly()); return true; }
        Player player = (Player) sender;
        if (!player.hasPermission("ariscore.auction.fastsell")) { player.sendMessage(lang.getNoPermission()); return true; }

        boolean current = plugin.getAuctionModule().getPlayerDataManager().getFastSell(player.getUniqueId());
        boolean next = !current;
        plugin.getAuctionModule().getPlayerDataManager().setFastSell(player.getUniqueId(), next);
        player.sendMessage(next ? lang.getFastSellOn() : lang.getFastSellOff());
        return true;
    }
}
