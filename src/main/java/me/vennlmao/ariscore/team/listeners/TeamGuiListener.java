package me.vennlmao.ariscore.team.listeners;

import me.vennlmao.ariscore.team.TeamModule;
import me.vennlmao.ariscore.team.managers.TeamData;
import me.vennlmao.ariscore.team.utils.ColorUtil;
import me.vennlmao.ariscore.team.utils.MessageUtil;
import me.vennlmao.ariscore.team.utils.SoundUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeamGuiListener implements Listener {

    private final TeamModule module;
    private final Map<UUID, UUID> pendingKick = new HashMap<>();

    public TeamGuiListener(TeamModule module) {
        this.module = module;
    }

    public void setPendingKick(UUID viewer, UUID target) {
        pendingKick.put(viewer, target);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        String mainTitle = ColorUtil.strip(module.getConfig().getString("gui.main.title", "Team"));
        String editTitle = ColorUtil.strip(module.getConfig().getString("gui.edit.title", "Edit"));
        String disbandTitle = ColorUtil.strip(module.getConfig().getString("gui.confirm_disband.title", "Confirm Disband"));
        String kickTitle = ColorUtil.strip(module.getConfig().getString("gui.confirm_kick.title", "Confirm Kick"));

        if (title.contains(mainTitle.replace("{team}", ""))) {
            event.setCancelled(true);
            handleMain(player, event.getSlot(), event.getCurrentItem());
        } else if (title.contains(editTitle.replace("{team}", ""))) {
            event.setCancelled(true);
            handleEdit(player, event.getSlot());
        } else if (title.equals(disbandTitle)) {
            event.setCancelled(true);
            handleDisband(player, event.getSlot());
        } else if (title.equals(kickTitle)) {
            event.setCancelled(true);
            handleKick(player, event.getSlot());
        }
    }

    private void handleMain(Player player, int slot, ItemStack clicked) {
        TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) return;

        int homeSlot = module.getConfig().getInt("gui.main.items.team_home.slot", 8);
        int backSlot = module.getConfig().getInt("gui.main.items.back.slot", 45);

        if (slot == homeSlot) {
            player.closeInventory();
            if (team.getHome() == null) {
                MessageUtil.sendChat(player, "home_not_set");
                MessageUtil.sendActionbar(player, "home_not_set_ab");
                SoundUtil.play(player, "error");
                return;
            }
            module.getWarmupManager().startWarmup(player, team.getHome());
            return;
        }

        if (slot == backSlot && team.isOwnerOrCoOwner(player.getUniqueId())) {
            SoundUtil.play(player, "click");
            player.getScheduler().run(module.getPlugin(), t ->
                    player.openInventory(module.getGuiBuilder().buildEdit(team)), null);
            return;
        }

        if (clicked != null && clicked.getItemMeta() instanceof SkullMeta skullMeta) {
            OfflinePlayer target = skullMeta.getOwningPlayer();
            if (target == null || target.getUniqueId().equals(player.getUniqueId())) return;
            if (!team.isOwnerOrCoOwner(player.getUniqueId())) return;
            if (!team.isMember(target.getUniqueId())) return;

            SoundUtil.play(player, "click");
            pendingKick.put(player.getUniqueId(), target.getUniqueId());
            player.getScheduler().run(module.getPlugin(), t ->
                    player.openInventory(module.getGuiBuilder().buildConfirmKick(target, team.getRoleString(target.getUniqueId()))), null);
        }
    }

    private void handleEdit(Player player, int slot) {
        TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) return;

        int inviteSlot = module.getConfig().getInt("gui.edit.items.invite.slot", 10);
        int kickSlot = module.getConfig().getInt("gui.edit.items.kick.slot", 12);
        int sethomeSlot = module.getConfig().getInt("gui.edit.items.sethome.slot", 14);
        int delhomeSlot = module.getConfig().getInt("gui.edit.items.delhome.slot", 16);
        int disbandSlot = module.getConfig().getInt("gui.edit.items.disband.slot", 22);

        if (slot == sethomeSlot) {
            SoundUtil.play(player, "success");
            player.closeInventory();
            String blocked = String.join(",", module.getConfig().getStringList("blocked_worlds"));
            if (blocked.contains(player.getWorld().getName())) {
                MessageUtil.sendChat(player, "world_blocked");
                MessageUtil.sendActionbar(player, "world_blocked_ab");
                SoundUtil.play(player, "error");
                return;
            }
            module.getTeamManager().setHome(team.getName(), player.getLocation());
            MessageUtil.sendChat(player, "sethome");
            MessageUtil.sendActionbar(player, "sethome_ab");
        } else if (slot == delhomeSlot) {
            SoundUtil.play(player, "click");
            player.closeInventory();
            module.getTeamManager().deleteHome(team.getName());
            MessageUtil.sendChat(player, "delhome");
            MessageUtil.sendActionbar(player, "delhome_ab");
        } else if (slot == disbandSlot && team.isOwner(player.getUniqueId())) {
            SoundUtil.play(player, "click");
            player.getScheduler().run(module.getPlugin(), t ->
                    player.openInventory(module.getGuiBuilder().buildConfirmDisband(team)), null);
        } else if (slot == inviteSlot || slot == kickSlot) {
            SoundUtil.play(player, "click");
            player.closeInventory();
            player.sendMessage(ColorUtil.parse("&eUse &f/team invite <player> &eor &f/team kick <player>"));
        }
    }

    private void handleDisband(Player player, int slot) {
        TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null || !team.isOwner(player.getUniqueId())) return;

        int confirmSlot = module.getConfig().getInt("gui.confirm_disband.items.confirm.slot", 11);
        int cancelSlot = module.getConfig().getInt("gui.confirm_disband.items.cancel.slot", 15);

        if (slot == confirmSlot) {
            SoundUtil.play(player, "error");
            String teamName = team.getName();
            for (UUID uuid : team.getMembers().keySet()) {
                Player member = Bukkit.getPlayer(uuid);
                if (member != null && !member.equals(player)) {
                    MessageUtil.sendChat(member, "disband_notify", s -> s.replace("{team}", teamName));
                }
            }
            module.getTeamManager().disbandTeam(teamName);
            player.closeInventory();
            MessageUtil.sendChat(player, "disband", s -> s.replace("{team}", teamName));
            MessageUtil.sendActionbar(player, "disband_ab");
        } else if (slot == cancelSlot) {
            SoundUtil.play(player, "click");
            player.closeInventory();
        }
    }

    private void handleKick(Player player, int slot) {
        TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) return;

        int confirmSlot = module.getConfig().getInt("gui.confirm_kick.items.confirm.slot", 11);
        int cancelSlot = module.getConfig().getInt("gui.confirm_kick.items.cancel.slot", 15);

        if (slot == confirmSlot) {
            UUID targetUuid = pendingKick.remove(player.getUniqueId());
            if (targetUuid == null) return;
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
            String targetName = target.getName() != null ? target.getName() : "?";

            module.getTeamManager().removeMember(targetUuid);
            player.closeInventory();
            MessageUtil.sendChat(player, "kick", s -> s.replace("{player}", targetName));
            MessageUtil.sendActionbar(player, "kick_ab");
            SoundUtil.play(player, "success");

            Player onlineTarget = Bukkit.getPlayer(targetUuid);
            if (onlineTarget != null) {
                MessageUtil.sendChat(onlineTarget, "kick_notify", s -> s.replace("{team}", team.getName()));
                SoundUtil.play(onlineTarget, "error");
            }
        } else if (slot == cancelSlot) {
            SoundUtil.play(player, "click");
            pendingKick.remove(player.getUniqueId());
            player.closeInventory();
        }
    }
}
