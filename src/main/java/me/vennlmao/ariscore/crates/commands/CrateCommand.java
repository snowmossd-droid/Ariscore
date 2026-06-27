package me.vennlmao.ariscore.crates.commands;

import me.vennlmao.ariscore.crates.CratesModule;
import me.vennlmao.ariscore.crates.models.CrateModel;
import me.vennlmao.ariscore.crates.models.GamerModel;
import me.vennlmao.ariscore.crates.utils.FoliaUtil;
import me.vennlmao.ariscore.crates.utils.MessageUtil;
import me.vennlmao.ariscore.crates.views.CrateEditView;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CrateCommand implements CommandExecutor, TabCompleter {

    private final CratesModule module;

    public CrateCommand(CratesModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        MessageUtil msg = module.getMessageUtil();

        if (args.length == 0) {
            msg.getList("usage-help").forEach(sender::sendMessage);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("ariscrates.admin")) {
                    msg.send(sender, "no-permission");
                    return true;
                }
                module.reload();
                msg.send(sender, "reloaded");
            }
            case "givekey" -> {
                if (!sender.hasPermission("ariscrates.admin")) {
                    msg.send(sender, "no-permission");
                    return true;
                }
                if (args.length < 3) {
                    msg.send(sender, "usage-givekey");
                    return true;
                }
                handleGiveKey(sender, args);
            }
            case "takekey" -> {
                if (!sender.hasPermission("ariscrates.admin")) {
                    msg.send(sender, "no-permission");
                    return true;
                }
                if (args.length < 3) {
                    msg.send(sender, "usage-takekey");
                    return true;
                }
                handleTakeKey(sender, args);
            }
            case "create" -> {
                if (!(sender instanceof Player player)) {
                    msg.send(sender, "players-only");
                    return true;
                }
                if (!player.hasPermission("ariscrates.admin")) {
                    msg.send(sender, "no-permission");
                    return true;
                }
                if (args.length < 2) {
                    msg.send(sender, "usage-create");
                    return true;
                }
                handleCreateCrate(player, args[1]);
            }
            case "movehere" -> {
                if (!(sender instanceof Player player)) {
                    msg.send(sender, "players-only");
                    return true;
                }
                if (!player.hasPermission("ariscrates.admin")) {
                    msg.send(sender, "no-permission");
                    return true;
                }
                if (args.length < 2) {
                    msg.send(sender, "usage-movehere");
                    return true;
                }
                handleMoveHere(player, args[1]);
            }
            case "edit" -> {
                if (!(sender instanceof Player player)) {
                    msg.send(sender, "players-only");
                    return true;
                }
                if (!player.hasPermission("ariscrates.admin")) {
                    msg.send(sender, "no-permission");
                    return true;
                }
                if (args.length < 2) {
                    msg.send(sender, "usage-edit");
                    return true;
                }
                handleEditCrate(player, args[1]);
            }
            default -> msg.getList("usage-help").forEach(sender::sendMessage);
        }
        return true;
    }

    private void handleGiveKey(CommandSender sender, String[] args) {
        MessageUtil msg = module.getMessageUtil();
        String playerName = args[1];
        String crateName = args[2];
        int amount = 1;

        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                msg.send(sender, "invalid-amount");
                playErrorSound(sender);
                return;
            }
        }

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            msg.send(sender, "player-not-found");
            playErrorSound(sender);
            return;
        }

        CrateModel crate = module.getCrateRegistry().find(crateName);
        if (crate == null) {
            msg.send(sender, "crate-not-found");
            playErrorSound(sender);
            return;
        }

        GamerModel gamer = module.getGamerDataManager().find(target.getUniqueId());
        if (gamer == null) {
            msg.send(sender, "player-data-not-loaded");
            playErrorSound(sender);
            return;
        }

        int finalAmount = amount;
        gamer.addKeyAmount(crateName, finalAmount);
        msg.send(sender, "key-given",
                "{amount}", String.valueOf(finalAmount),
                "{crate}", crateName,
                "{player}", target.getName());
    }

    private void handleTakeKey(CommandSender sender, String[] args) {
        MessageUtil msg = module.getMessageUtil();
        String playerName = args[1];
        String crateName = args[2];
        int amount = 1;

        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                msg.send(sender, "invalid-amount");
                playErrorSound(sender);
                return;
            }
        }

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            msg.send(sender, "player-not-found");
            playErrorSound(sender);
            return;
        }

        CrateModel crate = module.getCrateRegistry().find(crateName);
        if (crate == null) {
            msg.send(sender, "crate-not-found");
            playErrorSound(sender);
            return;
        }

        GamerModel gamer = module.getGamerDataManager().find(target.getUniqueId());
        if (gamer == null) {
            msg.send(sender, "player-data-not-loaded");
            playErrorSound(sender);
            return;
        }

        int finalAmount = amount;
        gamer.removeKeyAmount(crateName, finalAmount);
        msg.send(sender, "key-taken",
                "{amount}", String.valueOf(finalAmount),
                "{crate}", crateName,
                "{player}", target.getName());
    }

    private void handleCreateCrate(Player player, String crateName) {
        MessageUtil msg = module.getMessageUtil();

        if (module.getCrateRegistry().find(crateName) != null) {
            msg.send(player, "crate-already-exists");
            playErrorSound(player);
            return;
        }

        CrateModel crate = module.getCrateConfigManager().createCrateFile(crateName);
        module.getCrateRegistry().cache(crate);

        Location loc = player.getLocation().getBlock().getLocation();
        crate.addLocation(loc);

        FoliaUtil.runAsync(module.getPlugin(), () -> module.getCrateConfigManager().saveLocation(crate, loc));
        msg.send(player, "crate-created", "{crate}", crateName);
    }

    private void handleMoveHere(Player player, String crateName) {
        MessageUtil msg = module.getMessageUtil();
        CrateModel crate = module.getCrateRegistry().find(crateName);
        if (crate == null) {
            msg.send(player, "crate-not-found");
            playErrorSound(player);
            return;
        }

        crate.clearLocations();
        Location loc = player.getLocation().getBlock().getLocation();
        crate.addLocation(loc);

        FoliaUtil.runAsync(module.getPlugin(), () -> {
            module.getCrateConfigManager().removeAllLocations(crate);
            module.getCrateConfigManager().saveLocation(crate, loc);
        });
        msg.send(player, "crate-moved", "{crate}", crateName);
    }

    private void handleEditCrate(Player player, String crateName) {
        MessageUtil msg = module.getMessageUtil();
        CrateModel crate = module.getCrateRegistry().find(crateName);
        if (crate == null) {
            msg.send(player, "crate-not-found");
            playErrorSound(player);
            return;
        }
        CrateEditView.open(module, player, crate);
    }

    private void playErrorSound(CommandSender sender) {
        if (sender instanceof Player player) {
            module.getMessageUtil().playSound(player, "error");
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "givekey", "takekey", "create", "movehere", "edit").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("givekey") || args[0].equalsIgnoreCase("takekey")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (args[0].equalsIgnoreCase("movehere") || args[0].equalsIgnoreCase("edit")) {
                return module.getCrateRegistry().values().stream()
                        .map(CrateModel::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3 && (args[0].equalsIgnoreCase("givekey") || args[0].equalsIgnoreCase("takekey"))) {
            return module.getCrateRegistry().values().stream()
                    .map(CrateModel::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
