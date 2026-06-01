package me.vennlmao.ariscore.team.commands;

import me.vennlmao.ariscore.team.TeamModule;
import me.vennlmao.ariscore.team.managers.TeamData;
import me.vennlmao.ariscore.team.utils.ColorUtil;
import me.vennlmao.ariscore.team.utils.MessageUtil;
import me.vennlmao.ariscore.team.utils.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TeamCommand implements CommandExecutor, TabCompleter {

    private final TeamModule module;

    public TeamCommand(TeamModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        if (args.length == 0) {
            TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
            if (team == null) {
                MessageUtil.sendChat(player, "no_team");
                SoundUtil.play(player, "error");
                return true;
            }
            player.getScheduler().run(module.getPlugin(), t ->
                    player.openInventory(module.getGuiBuilder().buildMain(player, team)), null);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create" -> {
                if (args.length < 2) { SoundUtil.play(player, "error"); return true; }
                String name = args[1];
                int min = module.getConfig().getInt("team.min-name-length", 3);
                int max = module.getConfig().getInt("team.max-name-length", 16);
                if (name.length() < min) { MessageUtil.sendChat(player, "name_too_short"); SoundUtil.play(player, "error"); return true; }
                if (name.length() > max) { MessageUtil.sendChat(player, "name_too_long"); SoundUtil.play(player, "error"); return true; }
                if (module.getTeamManager().hasTeam(player.getUniqueId())) { MessageUtil.sendChat(player, "already_in_team"); SoundUtil.play(player, "error"); return true; }
                if (module.getTeamManager().teamExists(name)) { MessageUtil.sendChat(player, "name_taken"); SoundUtil.play(player, "error"); return true; }
                module.getTeamManager().createTeam(player, name);
                MessageUtil.sendChat(player, "create", s -> s.replace("{team}", name));
                MessageUtil.sendActionbar(player, "create_ab");
                SoundUtil.play(player, "success");
            }
            case "disband" -> {
                TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
                if (team == null) { MessageUtil.sendChat(player, "no_team"); SoundUtil.play(player, "error"); return true; }
                if (!team.isOwner(player.getUniqueId())) { MessageUtil.sendChat(player, "not_owner"); SoundUtil.play(player, "error"); return true; }
                String teamName = team.getName();
                for (java.util.UUID uuid : team.getMembers().keySet()) {
                    Player m = Bukkit.getPlayer(uuid);
                    if (m != null && !m.equals(player)) MessageUtil.sendChat(m, "disband_notify", s -> s.replace("{team}", teamName));
                }
                module.getTeamManager().disbandTeam(teamName);
                MessageUtil.sendChat(player, "disband", s -> s.replace("{team}", teamName));
                MessageUtil.sendActionbar(player, "disband_ab");
                SoundUtil.play(player, "error");
            }
            case "invite" -> {
                if (args.length < 2) { SoundUtil.play(player, "error"); return true; }
                TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
                if (team == null) { MessageUtil.sendChat(player, "no_team"); SoundUtil.play(player, "error"); return true; }
                if (!team.isOwnerOrCoOwner(player.getUniqueId())) { MessageUtil.sendChat(player, "not_owner_or_coowner"); SoundUtil.play(player, "error"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { MessageUtil.sendChat(player, "player_not_found"); SoundUtil.play(player, "error"); return true; }
                if (team.isMember(target.getUniqueId())) { MessageUtil.sendChat(player, "already_member", s -> s.replace("{player}", target.getName())); return true; }
                if (team.getMemberCount() >= module.getConfig().getInt("team.max-members", 45)) { MessageUtil.sendChat(player, "team_full"); SoundUtil.play(player, "error"); return true; }
                module.getTeamManager().addInvite(target.getUniqueId(), player.getUniqueId());
                MessageUtil.sendChat(player, "invite_sent", s -> s.replace("{player}", target.getName()));
                MessageUtil.sendActionbar(player, "invite_sent_ab");
                SoundUtil.play(player, "invite");
                MessageUtil.sendChat(target, "invite_received", s -> s.replace("{player}", player.getName()).replace("{team}", team.getName()));
                MessageUtil.sendActionbar(target, "invite_received_ab", s -> s.replace("{player}", player.getName()));
                SoundUtil.play(target, "invite");
            }
            case "join" -> {
                if (module.getTeamManager().hasTeam(player.getUniqueId())) { MessageUtil.sendChat(player, "already_in_team"); SoundUtil.play(player, "error"); return true; }
                if (args.length < 2) { SoundUtil.play(player, "error"); return true; }
                String teamName = args[1];
                if (!module.getTeamManager().hasAnyInvite(player.getUniqueId()) || !module.getTeamManager().hasInvite(player.getUniqueId(), teamName)) {
                    MessageUtil.sendChat(player, "no_invite"); SoundUtil.play(player, "error"); return true;
                }
                TeamData team = module.getTeamManager().getTeam(teamName);
                if (team == null) { MessageUtil.sendChat(player, "team_not_found"); return true; }
                if (team.getMemberCount() >= module.getConfig().getInt("team.max-members", 45)) { MessageUtil.sendChat(player, "team_full"); SoundUtil.play(player, "error"); return true; }
                module.getTeamManager().addMember(teamName, player);
                module.getTeamManager().removeInvite(player.getUniqueId());
                MessageUtil.sendChat(player, "join", s -> s.replace("{team}", teamName));
                MessageUtil.sendActionbar(player, "join_ab");
                SoundUtil.play(player, "success");
                for (java.util.UUID uuid : team.getMembers().keySet()) {
                    Player m = Bukkit.getPlayer(uuid);
                    if (m != null && !m.equals(player)) MessageUtil.sendChat(m, "join_notify", s -> s.replace("{player}", player.getName()));
                }
            }
            case "leave" -> {
                TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
                if (team == null) { MessageUtil.sendChat(player, "no_team"); SoundUtil.play(player, "error"); return true; }
                if (team.isOwner(player.getUniqueId())) { MessageUtil.sendChat(player, "not_owner"); SoundUtil.play(player, "error"); return true; }
                String teamName = team.getName();
                module.getTeamManager().removeMember(player.getUniqueId());
                MessageUtil.sendChat(player, "leave", s -> s.replace("{team}", teamName));
                MessageUtil.sendActionbar(player, "leave_ab");
                SoundUtil.play(player, "click");
                for (java.util.UUID uuid : team.getMembers().keySet()) {
                    Player m = Bukkit.getPlayer(uuid);
                    if (m != null) MessageUtil.sendChat(m, "leave_notify", s -> s.replace("{player}", player.getName()));
                }
            }
            case "kick" -> {
                if (args.length < 2) { SoundUtil.play(player, "error"); return true; }
                TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
                if (team == null) { MessageUtil.sendChat(player, "no_team"); SoundUtil.play(player, "error"); return true; }
                if (!team.isOwnerOrCoOwner(player.getUniqueId())) { MessageUtil.sendChat(player, "not_owner_or_coowner"); SoundUtil.play(player, "error"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { MessageUtil.sendChat(player, "player_not_found"); SoundUtil.play(player, "error"); return true; }
                if (!team.isMember(target.getUniqueId())) { MessageUtil.sendChat(player, "player_not_found"); return true; }
                module.getTeamManager().removeMember(target.getUniqueId());
                MessageUtil.sendChat(player, "kick", s -> s.replace("{player}", target.getName()));
                MessageUtil.sendActionbar(player, "kick_ab");
                SoundUtil.play(player, "success");
                MessageUtil.sendChat(target, "kick_notify", s -> s.replace("{team}", team.getName()));
                SoundUtil.play(target, "error");
            }
            case "sethome" -> {
                TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
                if (team == null) { MessageUtil.sendChat(player, "no_team"); SoundUtil.play(player, "error"); return true; }
                if (!team.isOwnerOrCoOwner(player.getUniqueId())) { MessageUtil.sendChat(player, "not_owner_or_coowner"); SoundUtil.play(player, "error"); return true; }
                if (module.getConfig().getStringList("blocked_worlds").contains(player.getWorld().getName())) {
                    MessageUtil.sendChat(player, "world_blocked"); MessageUtil.sendActionbar(player, "world_blocked_ab"); SoundUtil.play(player, "error"); return true;
                }
                module.getTeamManager().setHome(team.getName(), player.getLocation());
                MessageUtil.sendChat(player, "sethome"); MessageUtil.sendActionbar(player, "sethome_ab"); SoundUtil.play(player, "success");
            }
            case "delhome" -> {
                TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
                if (team == null) { MessageUtil.sendChat(player, "no_team"); SoundUtil.play(player, "error"); return true; }
                if (!team.isOwnerOrCoOwner(player.getUniqueId())) { MessageUtil.sendChat(player, "not_owner_or_coowner"); SoundUtil.play(player, "error"); return true; }
                module.getTeamManager().deleteHome(team.getName());
                MessageUtil.sendChat(player, "delhome"); MessageUtil.sendActionbar(player, "delhome_ab"); SoundUtil.play(player, "click");
            }
            case "home" -> {
                TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
                if (team == null) { MessageUtil.sendChat(player, "no_team"); SoundUtil.play(player, "error"); return true; }
                if (team.getHome() == null) { MessageUtil.sendChat(player, "home_not_set"); MessageUtil.sendActionbar(player, "home_not_set_ab"); SoundUtil.play(player, "error"); return true; }
                module.getWarmupManager().startWarmup(player, team.getHome());
            }
            case "chat" -> {
                TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
                if (team == null) { MessageUtil.sendChat(player, "no_team"); SoundUtil.play(player, "error"); return true; }
                module.getChatListener().toggleChat(player);
                if (module.getChatListener().isChatEnabled(player.getUniqueId())) {
                    MessageUtil.sendChat(player, "chat_toggle_on"); MessageUtil.sendActionbar(player, "chat_toggle_on_ab"); SoundUtil.play(player, "success");
                } else {
                    MessageUtil.sendChat(player, "chat_toggle_off"); MessageUtil.sendActionbar(player, "chat_toggle_off_ab"); SoundUtil.play(player, "click");
                }
            }
            case "info" -> {
                TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
                if (team == null) { MessageUtil.sendChat(player, "no_team"); SoundUtil.play(player, "error"); return true; }
                player.sendMessage(ColorUtil.parse("&8--- &6Team: &f" + team.getName() + " &8---"));
                player.sendMessage(ColorUtil.parse("&7Members: &f" + team.getMemberCount() + "/" + module.getConfig().getInt("team.max-members", 45)));
                player.sendMessage(ColorUtil.parse("&7Home: &f" + (team.getHome() != null ? team.getHome().getWorld().getName() : "Not set")));
                for (java.util.Map.Entry<java.util.UUID, TeamData.Role> e : team.getMembers().entrySet()) {
                    Player m = Bukkit.getPlayer(e.getKey());
                    String name = m != null ? m.getName() : Bukkit.getOfflinePlayer(e.getKey()).getName();
                    player.sendMessage(ColorUtil.parse("&7- &f" + name + " &8[&6" + team.getRoleString(e.getKey()) + "&8]"));
                }
            }

        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "disband", "invite", "join", "leave", "kick",
                    "sethome", "delhome", "home", "chat", "info", "reload").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (List.of("invite", "kick", "join").contains(sub)) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return List.of();
    }
}
