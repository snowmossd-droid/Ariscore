package me.vennlmao.ariscore.amethyst.commands;

import me.vennlmao.ariscore.amethyst.AmethystModule;
import me.vennlmao.ariscore.amethyst.utils.ColorUtil;
import me.vennlmao.ariscore.amethyst.utils.TimeParser;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AmethystCommand implements CommandExecutor, TabCompleter {

    private final AmethystModule module;
    private static final List<String> TOOL_TYPES = Arrays.asList(
            "pickaxe", "shovel", "bucket", "booster", "firework",
            "multitool", "treechopper", "magichoe");

    public AmethystCommand(AmethystModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendMessage(sender, "usage-give");
            sendMessage(sender, "usage-self");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give" -> handleGive(sender, args);
            case "self" -> handleSelf(sender, args);
            default -> {
                sendMessage(sender, "usage-give");
                sendMessage(sender, "usage-self");
            }
        }
        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("amethyst.admin")) {
            sendMessage(sender, "no-permission");
            return;
        }

        if (args.length < 3) {
            sendMessage(sender, "usage-give");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sendMessage(sender, "player-not-found");
            return;
        }

        String toolId = args[2].toLowerCase();
        if (!TOOL_TYPES.contains(toolId)) {
            sendMessage(sender, "invalid-tool");
            return;
        }

        ItemStack item = switch (toolId) {
            case "firework" -> module.getItemManager().createFireworkItem("firework", "firework");
            default -> module.getItemManager().createTool(toolId);
        };

        if (item == null) {
            sendMessage(sender, "invalid-tool");
            return;
        }

        target.getInventory().addItem(item);
        sendMessage(sender, "give-success", "{tool}", toolId, "{player}", target.getName());
    }

    private void handleSelf(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sendMessage(sender, "must-hold-item");
            return;
        }

        if (!player.hasPermission("amethyst.self")) {
            sendMessage(sender, "no-permission");
            return;
        }

        if (args.length < 2) {
            sendMessage(sender, "usage-self");
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (module.getItemManager().getToolType(item) == null) {
            sendMessage(sender, "must-hold-item");
            return;
        }

        if (!module.getItemManager().canSetExpiry(item)) {
            sendMessage(sender, "not-amethyst-item");
            return;
        }

        long millis = TimeParser.parseToMillis(args[1]);
        if (millis <= 0L) {
            sendMessage(sender, "invalid-time-format");
            return;
        }

        long maxDays = module.getConfig().getLong("self-destruct.max-time-days", 30);
        long maxMillis = maxDays * 86400000L;
        if (millis > maxMillis) millis = maxMillis;

        module.getItemManager().setExpiry(item, millis);
        sendMessage(sender, "self-set-success", "{time}", args[1]);
    }

    private void sendMessage(CommandSender sender, String key, String... replacements) {
        String raw = module.getConfig().getString("messages." + key, "");
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace(replacements[i], replacements[i + 1]);
        }
        sender.sendMessage(ColorUtil.translate(raw));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("give", "self").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return TOOL_TYPES.stream()
                    .filter(t -> t.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("self")) {
            return List.of("3d", "1d12h", "12h", "30m");
        }

        return new ArrayList<>();
    }
            }
            
