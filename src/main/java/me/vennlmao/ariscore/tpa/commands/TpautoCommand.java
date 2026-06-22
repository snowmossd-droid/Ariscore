package me.vennlmao.ariscore.tpa.commands;

import me.vennlmao.ariscore.tpa.TpaModule;
import me.vennlmao.ariscore.tpa.utils.MessageUtil;
import me.vennlmao.ariscore.tpa.utils.SoundUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TpautoCommand implements CommandExecutor {

    private final TpaModule plugin;

    public TpautoCommand(TpaModule plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        boolean newState = !plugin.getRequestManager().isTpautoEnabled(player);
        plugin.getRequestManager().setTpauto(player, newState);

        if (newState) {
            MessageUtil.sendChatList(player, "tpauto_enabled");
            SoundUtil.play(player, "toggle_on");
            plugin.getTpautoActionbarManager().start(player);
        } else {
            MessageUtil.sendChatList(player, "tpauto_disabled");
            MessageUtil.sendActionbar(player, "tpauto_disabled_ab");
            SoundUtil.play(player, "toggle_off");
            plugin.getTpautoActionbarManager().stop(player);
        }

        return true;
    }
}
