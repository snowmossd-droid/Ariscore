package me.vennlmao.ariscore.rtp.commands;

import me.vennlmao.ariscore.rtp.RtpModule;
import me.vennlmao.ariscore.rtp.utils.GuiUtil;
import me.vennlmao.ariscore.rtp.utils.MessageUtil;
import me.vennlmao.ariscore.rtp.utils.SoundUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RtpCommand implements CommandExecutor {

    private final RtpModule plugin;

    public RtpCommand(RtpModule plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (plugin.getCooldownManager().isOnCooldown(player.getUniqueId())) {
            long remaining = plugin.getCooldownManager().getRemainingSeconds(player.getUniqueId());
            MessageUtil.sendChatList(player, "cooldown",
                    s -> s.replace("{seconds}", String.valueOf(remaining)));
            MessageUtil.sendActionbar(player, "cooldown_ab",
                    s -> s.replace("{seconds}", String.valueOf(remaining)));
            SoundUtil.play(player, "error");
            return true;
        }

        player.getScheduler().run(plugin.getPlugin(), t -> {
            player.openInventory(GuiUtil.buildMainGui(plugin, player));
            SoundUtil.play(player, "open_gui");
        }, null);

        return true;
    }
                             }
