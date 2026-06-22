package me.vennlmao.ariscore.afk.commands;

import me.vennlmao.ariscore.afk.AfkModule;
import me.vennlmao.ariscore.afk.utils.MessageUtil;
import me.vennlmao.ariscore.afk.utils.SoundUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetAfkCommand implements CommandExecutor {

    private final AfkModule module;

    public SetAfkCommand(AfkModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (!player.hasPermission("arisafk.setafk")) {
            SoundUtil.play(player, "error");
            MessageUtil.sendChat(player, "no_permission");
            return true;
        }

        String defaultName = module.getConfig().getString("default-name", "afk");
        String name = args.length > 0 ? args[0] : defaultName;

        module.getAfkManager().setZone(name, player.getLocation());
        SoundUtil.play(player, "setafk");
        MessageUtil.sendChat(player, "setafk", s -> s.replace("%name%", name));
        MessageUtil.sendActionbar(player, "setafk_ab", s -> s.replace("%name%", name));

        return true;
    }
}
