package me.vennlmao.ariscore.tpa.commands;

import me.vennlmao.ariscore.tpa.TpaModule;
import me.vennlmao.ariscore.tpa.managers.TpaRequest;
import me.vennlmao.ariscore.tpa.utils.GuiUtil;
import me.vennlmao.ariscore.tpa.utils.MessageUtil;
import me.vennlmao.ariscore.tpa.utils.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class TpaCommand implements CommandExecutor, TabCompleter {

    private final TpaModule plugin;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public TpaCommand(TpaModule plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length == 0) {
            SoundUtil.play(player, "error");
            return true;
        }

        List<String> blockedWorlds = plugin.getConfig().getStringList("blocked_worlds");
        if (blockedWorlds.contains(player.getWorld().getName())) {
            MessageUtil.sendChatList(player, "world_blocked");
            MessageUtil.sendActionbar(player, "world_blocked_ab");
            SoundUtil.play(player, "error");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            MessageUtil.sendChatList(player, "target_offline");
            MessageUtil.sendActionbar(player, "target_offline_ab");
            SoundUtil.play(player, "error");
            return true;
        }

        if (target.equals(player)) {
            SoundUtil.play(player, "error");
            return true;
        }

        if (plugin.getRequestManager().isTpaDisabled(target)) {
            MessageUtil.sendChatList(player, "target_tpa_disabled",
                    s -> s.replace("{player}", target.getName()));
            MessageUtil.sendActionbar(player, "target_tpa_disabled_ab",
                    s -> s.replace("{player}", target.getName()));
            SoundUtil.play(player, "error");
            return true;
        }

        TpaRequest request = new TpaRequest(player, target, TpaRequest.Type.TPA);
        plugin.getRequestManager().addRequest(request);

        player.getScheduler().run(plugin.getPlugin(), t ->
                player.openInventory(GuiUtil.buildSenderGui(plugin, request)), null);

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return List.of();
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !p.equals(player))
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
