package me.vennlmao.ariscore.afk.commands;

import me.vennlmao.ariscore.afk.AfkModule;
import me.vennlmao.ariscore.afk.utils.MessageUtil;
import me.vennlmao.ariscore.afk.utils.SoundUtil;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class AfkCommand implements CommandExecutor, TabCompleter {

    private final AfkModule module;

    public AfkCommand(AfkModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length == 0) {
            if (module.getAfkManager().getAllZones().isEmpty()) {
                SoundUtil.play(player, "error");
                MessageUtil.sendChat(player, "no_afks");
                MessageUtil.sendActionbar(player, "no_afks_ab");
                return true;
            }
            module.getGuiListener().openAfksGui(player);
            return true;
        }

        String name = args[0];

        if (!module.getAfkManager().zoneExists(name)) {
            SoundUtil.play(player, "error");
            MessageUtil.sendChat(player, "afk_not_exist", s -> s.replace("%name%", name));
            MessageUtil.sendActionbar(player, "afk_not_exist_ab", s -> s.replace("%name%", name));
            return true;
        }

        Location loc = module.getAfkManager().getZone(name);
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
            return module.getAfkManager().getZoneNames().stream()
                    .filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
