package me.vennlmao.ariscore.tab.commands;

import me.vennlmao.ariscore.tab.TabModule;
import me.vennlmao.ariscore.tab.utils.ColorUtil;
import me.vennlmao.ariscore.tab.utils.SoundUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class ScoreboardToggleCommand implements CommandExecutor, TabCompleter {

    private final TabModule module;

    public ScoreboardToggleCommand(TabModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Player only.");
            return true;
        }

        module.getScoreboardManager().toggle(player);
        boolean nowDisabled = module.getScoreboardManager().isDisabled(player);

        if (nowDisabled) {
            SoundUtil.play(player, "tabtoggle_disabled");
            String chat = module.getConfig().getString("messages.tabtoggle_disabled", "");
            String ab = module.getConfig().getString("messages.tabtoggle_disabled_ab", "");
            if (!chat.isEmpty()) player.sendMessage(ColorUtil.parse(chat));
            if (!ab.isEmpty()) player.sendActionBar(ColorUtil.parse(ab));
        } else {
            SoundUtil.play(player, "tabtoggle_enabled");
            String chat = module.getConfig().getString("messages.tabtoggle_enabled", "");
            String ab = module.getConfig().getString("messages.tabtoggle_enabled_ab", "");
            if (!chat.isEmpty()) player.sendMessage(ColorUtil.parse(chat));
            if (!ab.isEmpty()) player.sendActionBar(ColorUtil.parse(ab));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return List.of();
    }
}
