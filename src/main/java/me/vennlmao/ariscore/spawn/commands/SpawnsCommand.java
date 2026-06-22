package me.vennlmao.ariscore.spawn.commands;

import me.vennlmao.ariscore.spawn.SpawnModule;
import me.vennlmao.ariscore.spawn.utils.MessageUtil;
import me.vennlmao.ariscore.spawn.utils.SoundUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpawnsCommand implements CommandExecutor {

    private final SpawnModule module;

    public SpawnsCommand(SpawnModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (module.getSpawnManager().getAllSpawns().isEmpty()) {
            SoundUtil.play(player, "error");
            MessageUtil.sendChat(player, "no_spawns");
            MessageUtil.sendActionbar(player, "no_spawns_ab");
            return true;
        }

        module.getGuiListener().openSpawnsGui(player);
        return true;
    }
}
