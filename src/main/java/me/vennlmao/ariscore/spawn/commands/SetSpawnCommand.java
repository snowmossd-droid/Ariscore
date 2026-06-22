package me.vennlmao.ariscore.spawn.commands;

import me.vennlmao.ariscore.spawn.SpawnModule;
import me.vennlmao.ariscore.spawn.utils.MessageUtil;
import me.vennlmao.ariscore.spawn.utils.SoundUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetSpawnCommand implements CommandExecutor {

    private final SpawnModule module;

    public SetSpawnCommand(SpawnModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (!player.hasPermission("arisspawn.setspawn")) {
            SoundUtil.play(player, "error");
            MessageUtil.sendChat(player, "no_permission");
            return true;
        }

        String defaultName = module.getConfig().getString("default-name", "spawn");
        String name = args.length > 0 ? args[0] : defaultName;

        module.getSpawnManager().setSpawn(name, player.getLocation());
        SoundUtil.play(player, "setspawn");
        MessageUtil.sendChat(player, "setspawn", s -> s.replace("%name%", name));
        MessageUtil.sendActionbar(player, "setspawn_ab", s -> s.replace("%name%", name));

        return true;
    }
}
