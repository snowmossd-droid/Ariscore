package me.vennlmao.ariscore.afk.commands;

import me.vennlmao.ariscore.afk.AfkModule;
import me.vennlmao.ariscore.afk.utils.MessageUtil;
import me.vennlmao.ariscore.afk.utils.SoundUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AfksCommand implements CommandExecutor {

    private final AfkModule module;

    public AfksCommand(AfkModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (module.getAfkManager().getAllZones().isEmpty()) {
            SoundUtil.play(player, "error");
            MessageUtil.sendChat(player, "no_afks");
            MessageUtil.sendActionbar(player, "no_afks_ab");
            return true;
        }

        module.getGuiListener().openAfksGui(player);
        return true;
    }
}
