package me.vennlmao.ariscore.duel.commands;

import me.vennlmao.ariscore.duel.DuelModule;
import me.vennlmao.ariscore.duel.utils.MessageUtil;
import me.vennlmao.ariscore.duel.utils.SoundUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class QueueCommand implements CommandExecutor {

    private final DuelModule module;

    public QueueCommand(DuelModule module) { this.module = module; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (module.getSessionManager().isQueued(player.getUniqueId())) {
            module.getSessionManager().leaveQueue(player);
            return true;
        }

        if (module.getSessionManager().isBusy(player.getUniqueId())) {
            MessageUtil.sendBoth(player, "already_busy");
            SoundUtil.play(player, "error");
            return true;
        }

        if (!module.getArenaManager().hasArenas()) {
            MessageUtil.sendBoth(player, "no_arenas");
            SoundUtil.play(player, "error");
            return true;
        }

        module.getSessionManager().joinQueue(player);
        return true;
    }
}
