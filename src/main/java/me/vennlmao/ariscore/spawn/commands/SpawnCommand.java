package me.vennlmao.ariscore.spawn.commands;

import me.vennlmao.ariscore.spawn.SpawnModule;
import me.vennlmao.ariscore.spawn.utils.MessageUtil;
import me.vennlmao.ariscore.spawn.utils.SoundUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class SpawnCommand implements CommandExecutor, TabCompleter {

    private final SpawnModule module;

    public SpawnCommand(SpawnModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length == 0) {
            if (module.getSpawnManager().getAllSpawns().isEmpty()) {
                SoundUtil.play(player, "error");
                MessageUtil.sendChat(player, "no_spawns");
                MessageUtil.sendActionbar(player, "no_spawns_ab");
                return true;
            }
            module.getGuiListener().openSpawnsGui(player);
            return true;
        }

        String name = args[0];

        if (!module.getSpawnManager().spawnExists(name)) {
            SoundUtil.play(player, "error");
            MessageUtil.sendChat(player, "spawn_not_exist", s -> s.replace("%name%", name));
            MessageUtil.sendActionbar(player, "spawn_not_exist_ab", s -> s.replace("%name%", name));
            return true;
        }

        Location loc = module.getSpawnManager().getSpawn(name);
        if (loc == null || loc.getWorld() == null) {
            SoundUtil.play(player, "error");
            MessageUtil.sendChat(player, "world_not_found");
            MessageUtil.sendActionbar(player, "world_not_found_ab");
            return true;
        }

        module.getWarmupManager().startWarmup(player, name, loc);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return module.getSpawnManager().getSpawnNames().stream()
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
