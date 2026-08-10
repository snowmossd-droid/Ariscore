package me.vennlmao.ariscore.warp.commands;

import me.vennlmao.ariscore.warp.WarpModule;
import me.vennlmao.ariscore.warp.utils.MessageUtil;
import me.vennlmao.ariscore.warp.utils.SoundUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class WarpsCommand implements CommandExecutor {

    private final WarpModule module;

    public WarpsCommand(WarpModule module) { this.module = module; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (module.getWarpManager().getAllWarps().isEmpty()) {
            SoundUtil.play(player, "error");
            MessageUtil.sendChat(player, "no_warps");
            MessageUtil.sendActionbar(player, "no_warps_ab");
            return true;
        }
        module.getGuiListener().openWarpsGui(player);
        return true;
    }
}
