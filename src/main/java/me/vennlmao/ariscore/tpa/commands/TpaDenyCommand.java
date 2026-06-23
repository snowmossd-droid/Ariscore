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

public class TpaDenyCommand implements CommandExecutor {

    private final TpaModule plugin;

    public TpaDenyCommand(TpaModule plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        TpaRequest request = plugin.getRequestManager().getRequest(player);
        if (request == null) {
            MessageUtil.sendChatList(player, "no_pending_requests");
            MessageUtil.sendActionbar(player, "no_pending_requests_ab");
            SoundUtil.play(player, "error");
            return true;
        }

        Player requester = request.getSender();
        plugin.getRequestManager().removeRequest(player);

        MessageUtil.sendChatList(player, "request_denied_receiver");
        MessageUtil.sendActionbar(player, "request_denied_receiver_ab");
        SoundUtil.play(player, "cancel");

        if (requester.isOnline()) {
            MessageUtil.sendChatList(requester, "request_denied_sender",
                    s -> s.replace("{player}", player.getName()));
            MessageUtil.sendActionbar(requester, "request_denied_sender_ab",
                    s -> s.replace("{player}", player.getName()));
            SoundUtil.play(requester, "cancel");
        }

        return true;
    }
                                  }
                      
