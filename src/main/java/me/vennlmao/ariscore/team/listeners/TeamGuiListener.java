package me.vennlmao.ariscore.team.listeners;

import me.vennlmao.ariscore.team.TeamModule;
import me.vennlmao.ariscore.team.gui.TeamGuiBuilder;
import me.vennlmao.ariscore.team.managers.TeamData;
import me.vennlmao.ariscore.team.utils.ColorUtil;
import me.vennlmao.ariscore.team.utils.MessageUtil;
import me.vennlmao.ariscore.team.utils.SoundUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TeamGuiListener implements Listener {

    private final TeamModule module;
    private final Map<UUID, Integer> playerPage = new HashMap<>();
    private final Map<UUID, UUID> pendingKick = new HashMap<>();

    public TeamGuiListener(TeamModule module) { this.module = module; }

    public void openMain(Player player, int page) {
        TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) return;
        int p = Math.max(0, page);
        playerPage.put(player.getUniqueId(), p);
        player.getScheduler().run(module.getPlugin(), t ->
                player.openInventory(module.getGuiBuilder().buildMain(player, team, p)), null);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        String mainBase = ColorUtil.strip(module.getConfig().getString("gui.main.title", ""))
                .replaceAll("\\([^)]*\\{[^}]*}[^)]*\\)", "")
                .replaceAll("\\{[^}]*}", "")
                .trim();
        String permTitle = ColorUtil.strip(module.getConfig().getString("permission-gui.title", ""));
        String kickTitle = ColorUtil.strip(module.getConfig().getString("kick-confirmation-gui.title", ""));
        String leaveTitle = ColorUtil.strip(module.getConfig().getString("leave-confirmation-gui.title", ""));
        String disbandTitle = ColorUtil.strip(module.getConfig().getString("disband-confirmation-gui.title", ""));

        } else if (title.equals(permTitle)) {
            event.setCancelled(true);
            handlePerm(player, event.getSlot());
        } else if (title.equals(kickTitle)) {
            event.setCancelled(true);
            handleKickConfirm(player, event.getSlot());
        } else if (title.equals(leaveTitle)) {
            event.setCancelled(true);
            handleLeaveConfirm(player, event.getSlot());
        } else if (title.equals(disbandTitle)) {
            event.setCancelled(true);
            handleDisbandConfirm(player, event.getSlot());
        } else if (title.contains(mainBase)) {
            event.setCancelled(true);
            handleMain(player, event.getSlot(), event.getCurrentItem());
        }
    }

    private void handleMain(Player player, int slot, ItemStack clicked) {
        TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) return;

        int backSlot = module.getConfig().getInt("items.back.slot");
        int nextSlot = module.getConfig().getInt("items.next.slot");
        int homeSlot = module.getConfig().getInt("items.team-home.slot");
        int pvpSlot  = module.getConfig().getInt("items.pvp.slot");

        if (slot == backSlot) {
            SoundUtil.play(player, "page_turn");
            int page = playerPage.getOrDefault(player.getUniqueId(), 0);
            if (page > 0) openMain(player, page - 1);
            return;
        }
        if (slot == nextSlot) {
            SoundUtil.play(player, "page_turn");
            int currentPage = playerPage.getOrDefault(player.getUniqueId(), 0);
            int memberCount = team.getMembers().size();
            int maxPage = (memberCount - 1) / me.vennlmao.ariscore.team.gui.TeamGuiBuilder.PAGE_SIZE;
            if (currentPage < maxPage) openMain(player, currentPage + 1);
            return;
        }
        if (slot == homeSlot) {
            player.closeInventory();
            if (team.getHome() == null) {
                MessageUtil.sendChat(player, "home_not_set");
                MessageUtil.sendActionbar(player, "home_not_set_ab");
                SoundUtil.play(player, "error");
                return;
            }
            if (!team.isOwnerOrCoOwner(player.getUniqueId())) {
                TeamData.MemberData md = team.getMemberData(player.getUniqueId());
                if (md == null || !md.permVisitHome) {
                    MessageUtil.sendChat(player, "no_permission");
                    SoundUtil.play(player, "error");
                    return;
                }
            }
            module.getWarmupManager().startWarmup(player, team.getHome());
            return;
        }
        if (slot == pvpSlot) {
            if (!team.isOwner(player.getUniqueId())) {
                TeamData.MemberData md = team.getMemberData(player.getUniqueId());
                if (md == null || !md.permPvpToggle) { SoundUtil.play(player, "error"); return; }
            }
            module.getTeamManager().setPvp(team.getName(), !team.isPvpEnabled());
            SoundUtil.play(player, "click");
            openMain(player, playerPage.getOrDefault(player.getUniqueId(), 0));
            return;
        }

        if (slot >= 0 && slot < TeamGuiBuilder.PAGE_SIZE) {
            if (clicked.getItemMeta() instanceof SkullMeta skullMeta) {
                OfflinePlayer op = skullMeta.getOwningPlayer();
                if (op == null) return;
                UUID targetUuid = op.getUniqueId();

                if (targetUuid.equals(player.getUniqueId())) {
                    if (team.isOwner(player.getUniqueId())) {
                        MessageUtil.sendChat(player, "owner_cannot_edit_self");
                        MessageUtil.sendActionbar(player, "owner_cannot_edit_self_ab");
                        SoundUtil.play(player, "error");
                    } else {
                        player.getScheduler().run(module.getPlugin(), t ->
                                player.openInventory(module.getGuiBuilder().buildLeaveConfirm()), null);
                        SoundUtil.play(player, "click");
                    }
                } else if (team.isOwnerOrCoOwner(player.getUniqueId())) {
                    pendingKick.put(player.getUniqueId(), targetUuid);
                    player.getScheduler().run(module.getPlugin(), t ->
                            player.openInventory(module.getGuiBuilder().buildPermissionGui(team, targetUuid)), null);
                    SoundUtil.play(player, "click");
                } else {
                    MessageUtil.sendChat(player, "member_cannot_edit");
                    MessageUtil.sendActionbar(player, "member_cannot_edit_ab");
                    SoundUtil.play(player, "error");
                }
            }
        }
    }


    private void handlePerm(Player player, int slot) {
        TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) return;

        int backSlot = module.getConfig().getInt("permission-gui.back-button.slot");
        if (slot == backSlot) {
            SoundUtil.play(player, "click");
            openMain(player, playerPage.getOrDefault(player.getUniqueId(), 0));
            return;
        }

        UUID targetUuid = pendingKick.get(player.getUniqueId());
        if (targetUuid == null) return;
        TeamData.MemberData md = team.getMemberData(targetUuid);
        if (md == null) return;

        ConfigurationSection permSec = module.getConfig().getConfigurationSection("permission-gui.permissions");
        if (permSec == null) return;

        for (String key : permSec.getKeys(false)) {
            if (permSec.getInt(key + ".slot") != slot) continue;
            if (key.equals("kick-player")) {
                player.getScheduler().run(module.getPlugin(), t ->
                        player.openInventory(module.getGuiBuilder().buildKickConfirm(Bukkit.getOfflinePlayer(targetUuid))), null);
                SoundUtil.play(player, "click");
                return;
            }
            if (md.role == TeamData.Role.OWNER) return;
            module.getGuiBuilder().togglePerm(md, key);
            module.getTeamManager().saveMemberPerms(md, team.getName());
            SoundUtil.play(player, "click");
            final UUID tUuid = targetUuid;
            player.getScheduler().run(module.getPlugin(), t ->
                    player.openInventory(module.getGuiBuilder().buildPermissionGui(team, tUuid)), null);
            return;
        }
    }

    private void handleKickConfirm(Player player, int slot) {
        TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) return;

        int confirmSlot = module.getConfig().getInt("kick-confirmation-gui.confirm.slot");
        int cancelSlot  = module.getConfig().getInt("kick-confirmation-gui.cancel.slot");

        if (slot == confirmSlot) {
            UUID targetUuid = pendingKick.remove(player.getUniqueId());
            if (targetUuid == null) return;
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
            String name = target.getName() != null ? target.getName() : "?";
            module.getTeamManager().removeMember(targetUuid);
            player.closeInventory();
            MessageUtil.sendChat(player, "kick", s -> s.replace("{player}", name));
            MessageUtil.sendActionbar(player, "kick_ab");
            SoundUtil.play(player, "success");
            Player online = Bukkit.getPlayer(targetUuid);
            if (online != null) {
                MessageUtil.sendChat(online, "kick_notify", s -> s.replace("{team}", team.getName()));
                SoundUtil.play(online, "error");
            }
        } else if (slot == cancelSlot) {
            SoundUtil.play(player, "click");
            UUID targetUuid = pendingKick.get(player.getUniqueId());
            if (targetUuid != null) {
                player.getScheduler().run(module.getPlugin(), t ->
                        player.openInventory(module.getGuiBuilder().buildPermissionGui(team, targetUuid)), null);
            } else {
                openMain(player, playerPage.getOrDefault(player.getUniqueId(), 0));
            }
        }
    }

    private void handleLeaveConfirm(Player player, int slot) {
        TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) { player.closeInventory(); return; }

        int confirmSlot = module.getConfig().getInt("leave-confirmation-gui.confirm.slot");
        int cancelSlot  = module.getConfig().getInt("leave-confirmation-gui.cancel.slot");

        if (slot == confirmSlot) {
            if (team.isOwner(player.getUniqueId())) return;
            String teamName = team.getName();
            module.getTeamManager().removeMember(player.getUniqueId());
            player.closeInventory();
            MessageUtil.sendChat(player, "leave", s -> s.replace("{team}", teamName));
            MessageUtil.sendActionbar(player, "leave_ab");
            SoundUtil.play(player, "click");
            for (UUID uuid : team.getMembers().keySet()) {
                Player m = Bukkit.getPlayer(uuid);
                if (m != null) MessageUtil.sendChat(m, "leave_notify", s -> s.replace("{player}", player.getName()));
            }
        } else if (slot == cancelSlot) {
            SoundUtil.play(player, "click");
            openMain(player, playerPage.getOrDefault(player.getUniqueId(), 0));
        }
    }

    private void handleDisbandConfirm(Player player, int slot) {
        TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());

        int confirmSlot = module.getConfig().getInt("disband-confirmation-gui.confirm.slot");
        int cancelSlot  = module.getConfig().getInt("disband-confirmation-gui.cancel.slot");

        if (slot == confirmSlot) {
            if (team == null || !team.isOwner(player.getUniqueId())) { player.closeInventory(); return; }
            String teamName = team.getName();
            for (UUID uuid : team.getMembers().keySet()) {
                Player m = Bukkit.getPlayer(uuid);
                if (m != null && !m.equals(player))
                    MessageUtil.sendChat(m, "disband_notify", s -> s.replace("{team}", teamName));
            }
            module.getTeamManager().disbandTeam(teamName);
            pendingKick.remove(player.getUniqueId());
            playerPage.remove(player.getUniqueId());
            player.closeInventory();
            MessageUtil.sendChat(player, "disband", s -> s.replace("{team}", teamName));
            MessageUtil.sendActionbar(player, "disband_ab");
            SoundUtil.play(player, "error");
        } else if (slot == cancelSlot) {
            SoundUtil.play(player, "click");
            if (team == null || !team.isOwner(player.getUniqueId())) { player.closeInventory(); return; }
            openMain(player, playerPage.getOrDefault(player.getUniqueId(), 0));
        }
    }
                            }
        
