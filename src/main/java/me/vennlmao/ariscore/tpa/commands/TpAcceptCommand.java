package me.vennlmao.ariscore.tpa.commands;

import me.vennlmao.ariscore.tpa.TpaModule;
import me.vennlmao.ariscore.tpa.managers.TpaRequest;
import me.vennlmao.ariscore.tpa.utils.GuiUtil;
import me.vennlmao.ariscore.tpa.utils.MessageUtil;
import me.vennlmao.ariscore.tpa.utils.SoundUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TpAcceptCommand implements CommandExecutor {

    private final TpaModule plugin;

    public TpAcceptCommand(TpaModule plugin) {
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
        if (!requester.isOnline()) {
            MessageUtil.sendChatList(player, "requester_offline");
            MessageUtil.sendActionbar(player, "requester_offline_ab");
            SoundUtil.play(player, "error");
            plugin.getRequestManager().removeRequest(player);
            return true;
        }

        player.getScheduler().run(plugin.getPlugin(), t ->
                player.openInventory(GuiUtil.buildAcceptGui(plugin, request)), null);

        return true;
    }
}
