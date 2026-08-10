package me.vennlmao.ariscore.spawners.commands;

import me.vennlmao.ariscore.spawners.SpawnersModule;
import me.vennlmao.ariscore.spawners.managers.MobSpawnerDefinition;
import me.vennlmao.ariscore.spawners.utils.ColorUtil;
import me.vennlmao.ariscore.spawners.utils.SpawnerItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SpawnerCommand implements CommandExecutor, TabCompleter {

    private final SpawnersModule module;

    public SpawnerCommand(SpawnersModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ColorUtil.parse("&cSử dụng: /spawner give <player> <mob> [amount] | /spawner list | /spawner reload"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give" -> handleGive(sender, args);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            default -> sender.sendMessage(ColorUtil.parse("&cSử dụng: /spawner give <player> <mob> [amount] | /spawner list | /spawner reload"));
        }
        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ariscore.spawners.give")) {
            sender.sendMessage(ColorUtil.parse("&cBạn không có quyền."));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ColorUtil.parse("&cSử dụng: /spawner give <player> <mob> [amount]"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ColorUtil.parse("&cKhông tìm thấy người chơi."));
            return;
        }
        EntityType type;
        try {
            type = EntityType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ColorUtil.parse("&cLoại quái không hợp lệ."));
            return;
        }
        if (!module.getSpawnerDefinitionManager().has(type)) {
            sender.sendMessage(ColorUtil.parse("&cLoại spawner này chưa được cấu hình."));
            return;
        }
        long amount = 1;
        if (args.length >= 4) {
            try {
                amount = Math.max(1, Long.parseLong(args[3]));
            } catch (NumberFormatException ignored) {}
        }

        ItemStack item = SpawnerItemUtil.createItem(type, amount);
        target.getInventory().addItem(item);
        sender.sendMessage(ColorUtil.parse("&aĐã cho " + target.getName() + " " + amount + "x " + SpawnerItemUtil.mobName(type) + " Spawner."));
    }

    private void handleList(CommandSender sender) {
        List<MobSpawnerDefinition> all = new ArrayList<>(module.getSpawnerDefinitionManager().getAll().values());
        if (all.isEmpty()) {
            sender.sendMessage(ColorUtil.parse("&cKhông có spawner nào được cấu hình."));
            return;
        }
        String names = all.stream().map(MobSpawnerDefinition::getSpawnerName).collect(Collectors.joining(", "));
        sender.sendMessage(ColorUtil.parse("&eCác loại spawner khả dụng: &f" + names));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("ariscore.spawners.admin")) {
            sender.sendMessage(ColorUtil.parse("&cBạn không có quyền."));
            return;
        }
        module.reload();
        sender.sendMessage(ColorUtil.parse("&aĐã reload module Spawners."));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(List.of("give", "list", "reload"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            List<String> names = module.getSpawnerDefinitionManager().getAll().keySet().stream()
                    .map(Enum::name).collect(Collectors.toList());
            return filter(names, args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String input) {
        return options.stream().filter(s -> s.toLowerCase().startsWith(input.toLowerCase())).collect(Collectors.toList());
    }
}
