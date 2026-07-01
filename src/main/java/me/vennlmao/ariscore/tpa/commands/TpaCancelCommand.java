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

public class TpaCancelCommand implements CommandExecutor {

    private final TpaModule plugin;

    public TpaCancelCommand(TpaModule plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (plugin.getWarmupManager().isInWarmup(player)) {
            plugin.getWarmupManager().cancelWarmup(player.getUniqueId());
            MessageUtil.sendChatList(player, "request_cancelled");
            MessageUtil.sendActionbar(player, "request_cancelled_ab");
            SoundUtil.play(player, "cancel");
            return true;
        }

        TpaRequest request = plugin.getRequestManager().getRequestBySender(player);
        if (request == null) {
            MessageUtil.sendChatList(player, "no_pending_requests");
            MessageUtil.sendActionbar(player, "no_pending_requests_ab");
            SoundUtil.play(player, "error");
            return true;
        }

        Player receiver = request.getReceiver();
        plugin.getRequestManager().removeRequestBySender(player);

        MessageUtil.sendChatList(player, "outgoing_request_cancelled");
        MessageUtil.sendActionbar(player, "outgoing_request_cancelled_ab");
        SoundUtil.play(player, "cancel");

        if (receiver.isOnline()) {
            MessageUtil.sendChatList(receiver, "incoming_request_cancelled",
                    s -> s.replace("{player}", player.getName()));
            MessageUtil.sendActionbar(receiver, "incoming_request_cancelled_ab",
                    s -> s.replace("{player}", player.getName()));
            SoundUtil.play(receiver, "cancel");
        }

        return true;
    }
}
