package me.vennlmao.ariscore.duel.commands;

import me.vennlmao.ariscore.duel.DuelModule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LeaveCommand implements CommandExecutor {

    private final DuelModule module;

    public LeaveCommand(DuelModule module) { this.module = module; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;
        module.getSessionManager().handleLeave(player);
        return true;
    }
}
