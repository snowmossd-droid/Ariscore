package me.vennlmao.ariscore.sell.commands;

import me.vennlmao.ariscore.sell.SellModule;
import me.vennlmao.ariscore.sell.menus.SellHistoryMenu;
import me.vennlmao.ariscore.sell.utils.ColorUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SellHistoryCommand implements CommandExecutor {

    private final SellModule module;

    public SellHistoryCommand(SellModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ColorUtil.colorize(module.getConfig().getString("player-only", "&cThis command is only for players!")));
            return true;
        }
        player.openInventory(new SellHistoryMenu(module, player).getInventory());
        return true;
    }
}
