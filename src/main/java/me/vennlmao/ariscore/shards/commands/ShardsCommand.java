package me.vennlmao.ariscore.shards.commands;

import me.vennlmao.ariscore.shards.ShardsModule;
import me.vennlmao.ariscore.shards.utils.ColorUtil;
import me.vennlmao.ariscore.shards.utils.SoundUtil;
import org.bukkit.Bukkit;
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

public class ShardsCommand implements CommandExecutor, TabCompleter {

    private final ShardsModule module;

    public ShardsCommand(ShardsModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length < 2) {
            sendMsg(sender, "usage");
            return true;
        }

        String sub = args[0].toLowerCase();
        String target = args[1];
        boolean all = target.equalsIgnoreCase("all");

        switch (sub) {
            case "give" -> {
                if (args.length < 3) { sendMsg(sender, "usage"); return true; }
                long amount = parseLong(args[2]);
                if (amount <= 0) { sendMsg(sender, "usage"); return true; }

                if (all) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        module.getShardsManager().addShards(p, amount);
                    }
                    sendMsg(sender, "give_all", "{amount}", String.valueOf(amount));
                } else {
                    Player p = Bukkit.getPlayer(target);
                    if (p == null) { sendMsg(sender, "player_not_found"); return true; }
                    module.getShardsManager().addShards(p, amount);
                    sendMsg(sender, "give", "{player}", p.getName(), "{amount}", String.valueOf(amount));
                    String ab = module.getConfig().getString("messages.give_ab", "&a+{amount} mảnh vụn")
                            .replace("{amount}", String.valueOf(amount));
                    p.sendActionBar(ColorUtil.parse(ab));
                    SoundUtil.play(p, "receive");
                }
            }
            case "take" -> {
                if (args.length < 3) { sendMsg(sender, "usage"); return true; }
                long amount = parseLong(args[2]);
                if (amount <= 0) { sendMsg(sender, "usage"); return true; }

                if (all) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        module.getShardsManager().takeShards(p, amount);
                    }
                    sendMsg(sender, "take_all", "{amount}", String.valueOf(amount));
                } else {
                    Player p = Bukkit.getPlayer(target);
                    if (p == null) { sendMsg(sender, "player_not_found"); return true; }
                    boolean success = module.getShardsManager().takeShards(p, amount);
                    if (!success) {
                        sendMsg(sender, "not_enough");
                        return true;
                    }
                    sendMsg(sender, "take", "{player}", p.getName(), "{amount}", String.valueOf(amount));
                    String ab = module.getConfig().getString("messages.take_ab", "&c-{amount} mảnh vụn")
                            .replace("{amount}", String.valueOf(amount));
                    p.sendActionBar(ColorUtil.parse(ab));
                }
            }
            case "reset" -> {
                if (all) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        module.getShardsManager().resetShards(p.getUniqueId());
                    }
                    sendMsg(sender, "reset_all");
                } else {
                    Player p = Bukkit.getPlayer(target);
                    if (p == null) { sendMsg(sender, "player_not_found"); return true; }
                    module.getShardsManager().resetShards(p.getUniqueId());
                    sendMsg(sender, "reset", "{player}", p.getName());
                    String ab = module.getConfig().getString("messages.reset_ab", "&cMảnh vụn đã reset");
                    p.sendActionBar(ColorUtil.parse(ab));
                }
            }
            default -> sendMsg(sender, "usage");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("give", "take", "reset").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            List<String> options = new ArrayList<>();
            options.add("all");
            Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .forEach(options::add);
            return options;
        }
        if (args.length == 3 && !args[1].equalsIgnoreCase("reset")) {
            return Arrays.asList("1", "10", "100", "1000");
        }
        return List.of();
    }

    private void sendMsg(CommandSender sender, String key, String... replacements) {
        String msg = module.getConfig().getString("messages." + key, "");
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        if (!msg.isEmpty()) sender.sendMessage(ColorUtil.parse(msg));
    }

    private long parseLong(String s) {
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return -1; }
    }
}
