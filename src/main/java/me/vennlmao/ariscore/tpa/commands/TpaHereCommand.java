package me.vennlmao.ariscore.tpa.commands;

import me.vennlmao.ariscore.tpa.TpaModule;
import me.vennlmao.ariscore.tpa.managers.TpaRequest;
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

public class TpaHereCommand implements CommandExecutor, TabCompleter {

    private final TpaModule plugin;
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public TpaHereCommand(TpaModule plugin) {
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

        if (plugin.getRequestManager().isTpahereDisabled(target)) {
            MessageUtil.sendChatList(player, "target_tpahere_disabled",
                    s -> s.replace("{player}", target.getName()));
            MessageUtil.sendActionbar(player, "target_tpahere_disabled_ab",
                    s -> s.replace("{player}", target.getName()));
            SoundUtil.play(player, "error");
            return true;
        }

        TpaRequest request = new TpaRequest(player, target, TpaRequest.Type.TPAHERE);
        plugin.getRequestManager().addRequest(request);

        MessageUtil.sendChatList(player, "request_sent_tpahere",
                s -> s.replace("{player}", target.getName()));
        MessageUtil.sendActionbar(player, "request_sent_tpahere_ab",
                s -> s.replace("{player}", target.getName()));
        SoundUtil.play(player, "request_sent");

        if (plugin.getRequestManager().isTpautoEnabled(target)) {
            plugin.getWarmupManager().startWarmup(target, player, true);
            return true;
        }

        sendClickableRequest(target, player, "accept_tpahere");
        MessageUtil.sendChatList(target, "request_received_tpahere",
                s -> s.replace("{player}", player.getName()));
        SoundUtil.play(target, "request_sent");

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

    private void sendClickableRequest(Player receiver, Player requester, String key) {
        List<String> lines = plugin.getConfig().getStringList("clickable_messages." + key + ".text");
        for (String line : lines) {
            String replaced = line.replace("{player}", requester.getName());
            String colored = replaced.replaceAll("#([0-9A-Fa-f]{6})", "<color:#$1>")
                    .replace("&7", "<gray>").replace("&a", "<green>");
            Component component = MM.deserialize(colored)
                    .clickEvent(ClickEvent.runCommand("/tpaccept " + requester.getName()));
            receiver.sendMessage(component);
        }
    }
}
