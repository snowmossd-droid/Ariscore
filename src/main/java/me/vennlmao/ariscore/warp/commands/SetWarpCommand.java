package me.vennlmao.ariscore.warp.commands;

import me.vennlmao.ariscore.warp.WarpModule;
import me.vennlmao.ariscore.warp.utils.MessageUtil;
import me.vennlmao.ariscore.warp.utils.SoundUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetWarpCommand implements CommandExecutor {

    private final WarpModule module;

    public SetWarpCommand(WarpModule module) { this.module = module; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (!player.hasPermission("ariswarp.setwarp")) {
            SoundUtil.play(player, "error");
            MessageUtil.sendChat(player, "no_permission");
            return true;
        }
        String name = args.length > 0 ? args[0] : module.getConfig().getString("default-name", "warp");
        module.getWarpManager().setWarp(name, player.getLocation());
        SoundUtil.play(player, "setwarp");
        MessageUtil.sendChat(player, "setwarp", s -> s.replace("%name%", name));
        MessageUtil.sendActionbar(player, "setwarp_ab", s -> s.replace("%name%", name));
        return true;
    }
}
