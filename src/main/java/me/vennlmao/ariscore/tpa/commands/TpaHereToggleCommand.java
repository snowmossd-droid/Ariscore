package me.vennlmao.ariscore.tpa.commands;

import me.vennlmao.ariscore.tpa.TpaModule;
import me.vennlmao.ariscore.tpa.managers.TpaRequest;
import me.vennlmao.ariscore.tpa.utils.MessageUtil;
import me.vennlmao.ariscore.tpa.utils.SoundUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TpaHereToggleCommand implements CommandExecutor {

    private final TpaModule plugin;

    public TpaHereToggleCommand(TpaModule plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        boolean nowDisabled = !plugin.getRequestManager().isTpahereDisabled(player);
        plugin.getRequestManager().setTpahereDisabled(player, nowDisabled);

        if (!nowDisabled) {
            MessageUtil.sendChatList(player, "tpaheretoggle_enabled");
            MessageUtil.sendActionbar(player, "tpaheretoggle_enabled_ab");
            SoundUtil.play(player, "toggle_on");
        } else {
            MessageUtil.sendChatList(player, "tpaheretoggle_disabled");
            MessageUtil.sendActionbar(player, "tpaheretoggle_disabled_ab");
            SoundUtil.play(player, "toggle_off");

            
            TpaRequest pending = plugin.getRequestManager().getRequest(player);
            if (pending != null && pending.getType() == TpaRequest.Type.TPAHERE) {
                Player requester = pending.getSender();
                plugin.getRequestManager().removeRequest(player);
                if (requester.isOnline()) {
                    MessageUtil.sendChatList(requester, "target_tpahere_disabled",
                            s -> s.replace("{player}", player.getName()));
                    MessageUtil.sendActionbar(requester, "target_tpahere_disabled_ab",
                            s -> s.replace("{player}", player.getName()));
                    SoundUtil.play(requester, "cancel");
                }
            }
        }

        return true;
    }
}
