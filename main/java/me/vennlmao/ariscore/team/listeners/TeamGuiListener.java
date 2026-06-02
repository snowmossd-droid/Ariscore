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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeamGuiListener implements Listener {

    private final TeamModule module;
    private final Map<UUID, Integer> playerPage = new HashMap<>();
    private final Map<UUID, TeamData.SortType> playerSort = new HashMap<>();
    private final Map<UUID, UUID> pendingKick = new HashMap<>();

    public TeamGuiListener(TeamModule module) { this.module = module; }

    public void openMain(Player player, int page) {
        TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) return;
        int p = Math.max(0, page);
        playerPage.put(player.getUniqueId(), p);
        TeamData.SortType sort = playerSort.getOrDefault(player.getUniqueId(), TeamData.SortType.JOIN_DATE);
        player.getScheduler().run(module.getPlugin(), t ->
                player.openInventory(module.getGuiBuilder().buildMain(player, team, p, sort)), null);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        String mainTitleRaw = ColorUtil.strip(module.getConfig().getString("gui.main.title", "ᴛᴇᴀᴍ"));
        String mainBase = mainTitleRaw.replace("(Page {page})", "").replace("{team-name}", "").replace("{page}", "").trim();

        String sortTitle = ColorUtil.strip(module.getConfig().getString("sort-gui.title", "ѕᴏʀᴛ"));
        String permTitle = ColorUtil.strip(module.getConfig().getString("permission-gui.title", "ᴇᴅɪᴛ ᴘᴇʀᴍɪssɪᴏɴs"));
        String kickTitle = ColorUtil.strip(module.getConfig().getString("kick-confirmation-gui.title", "ᴄᴏɴғɪʀᴍ ᴋɪᴄᴋ"));
        String leaveTitle = ColorUtil.strip(module.getConfig().getString("leave-confirmation-gui.title", "ᴄᴏɴғɪʀᴍ ʟᴇᴀᴠɪɴɢ ᴛᴇᴀᴍ"));
        String disbandTitle = ColorUtil.strip(module.getConfig().getString("disband-confirmation-gui.title", "ᴄᴏɴғɪʀᴍ ᴅɪsʙᴀɴᴅɪɴɢ ᴛᴇᴀᴍ"));

        if (title.contains(mainBase)) {
            event.setCancelled(true);
            handleMain(player, event.getSlot(), event.getCurrentItem());
        } else if (title.equals(sortTitle)) {
            event.setCancelled(true);
            handleSort(player, event.getSlot());
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
        }
    }

    private void handleMain(Player player, int slot, ItemStack clicked) {
        TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) return;

        int backSlot = module.getConfig().getInt("items.back.slot", 48);
        int nextSlot = module.getConfig().getInt("items.next.slot", 50);
        int sortSlot = module.getConfig().getInt("items.sort.slot", 46);
        int homeSlot = module.getConfig().getInt("items.team-home.slot", 52);
        int pvpSlot  = module.getConfig().getInt("items.pvp.slot", 53);

        if (slot == backSlot) {
            SoundUtil.play(player, "click");
            int page = playerPage.getOrDefault(player.getUniqueId(), 0);
            if (page > 0) openMain(player, page - 1);
            return;
        }
        if (slot == nextSlot) {
            SoundUtil.play(player, "click");
            openMain(player, playerPage.getOrDefault(player.getUniqueId(), 0) + 1);
            return;
        }
        if (slot == sortSlot) {
            SoundUtil.play(player, "click");
            player.getScheduler().run(module.getPlugin(), t ->
                    player.openInventory(module.getGuiBuilder().buildSortGui()), null);
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
                        player.getScheduler().run(module.getPlugin(), t ->
                                player.openInventory(module.getGuiBuilder().buildDisbandConfirm()), null);
                    } else {
                        player.getScheduler().run(module.getPlugin(), t ->
                                player.openInventory(module.getGuiBuilder().buildLeaveConfirm()), null);
                    }
                    SoundUtil.play(player, "click");
                } else if (team.isOwnerOrCoOwner(player.getUniqueId())) {
                    pendingKick.put(player.getUniqueId(), targetUuid);
                    player.getScheduler().run(module.getPlugin(), t ->
                            player.openInventory(module.getGuiBuilder().buildPermissionGui(team, targetUuid)), null);
                    SoundUtil.play(player, "click");
                }
            }
        }
    }

    private void handleSort(Player player, int slot) {
        String key = null;
        for (String optKey : module.getConfig().getConfigurationSection("sort-gui.options").getKeys(false)) {
            if (module.getConfig().getInt("sort-gui.options." + optKey + ".slot") == slot) { key = optKey; break; }
        }
        if (key == null) return;
        TeamData.SortType sort = switch (key) {
            case "alphabetically" -> TeamData.SortType.ALPHABETICALLY;
            case "online-members" -> TeamData.SortType.ONLINE_MEMBERS;
            default -> TeamData.SortType.JOIN_DATE;
        };
        playerSort.put(player.getUniqueId(), sort);
        SoundUtil.play(player, "click");
        openMain(player, playerPage.getOrDefault(player.getUniqueId(), 0));
    }

    private void handlePerm(Player player, int slot) {
        TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) return;

        int backSlot = module.getConfig().getInt("permission-gui.back-button.slot", 18);
        if (slot == backSlot) {
            SoundUtil.play(player, "click");
            openMain(player, playerPage.getOrDefault(player.getUniqueId(), 0));
            return;
        }

        UUID targetUuid = pendingKick.get(player.getUniqueId());
        if (targetUuid == null) return;
        TeamData.MemberData md = team.getMemberData(targetUuid);
        if (md == null) return;

        int kickSlot = module.getConfig().getInt("permission-gui.permissions.kick-player.slot", 11);
        if (slot == kickSlot) {
            player.getScheduler().run(module.getPlugin(), t ->
                    player.openInventory(module.getGuiBuilder().buildKickConfirm(Bukkit.getOfflinePlayer(targetUuid))), null);
            SoundUtil.play(player, "click");
            return;
        }

        String permKey = null;
        for (String k : module.getConfig().getConfigurationSection("permission-gui.permissions").getKeys(false)) {
            if (module.getConfig().getInt("permission-gui.permissions." + k + ".slot") == slot) { permKey = k; break; }
        }
        if (permKey == null || permKey.equals("kick-player")) return;
        if (md.role == TeamData.Role.OWNER) return;

        module.getGuiBuilder().togglePerm(md, permKey);
        module.getTeamManager().saveMemberPerms(md, team.getName());
        SoundUtil.play(player, "click");

        final UUID tUuid = targetUuid;
        player.getScheduler().run(module.getPlugin(), t ->
                player.openInventory(module.getGuiBuilder().buildPermissionGui(team, tUuid)), null);
    }

    private void handleKickConfirm(Player player, int slot) {
        int confirmSlot = module.getConfig().getInt("kick-confirmation-gui.confirm.slot", 15);
        int cancelSlot = module.getConfig().getInt("kick-confirmation-gui.cancel.slot", 11);
        TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) return;

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
            if (online != null) { MessageUtil.sendChat(online, "kick_notify", s -> s.replace("{team}", team.getName())); SoundUtil.play(online, "error"); }
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
        int confirmSlot = module.getConfig().getInt("leave-confirmation-gui.confirm.slot", 15);
        int cancelSlot = module.getConfig().getInt("leave-confirmation-gui.cancel.slot", 11);
        TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) return;

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
        int confirmSlot = module.getConfig().getInt("disband-confirmation-gui.confirm.slot", 15);
        int cancelSlot = module.getConfig().getInt("disband-confirmation-gui.cancel.slot", 11);
        TeamData team = module.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null || !team.isOwner(player.getUniqueId())) return;

        if (slot == confirmSlot) {
            String teamName = team.getName();
            for (UUID uuid : team.getMembers().keySet()) {
                Player m = Bukkit.getPlayer(uuid);
                if (m != null && !m.equals(player)) MessageUtil.sendChat(m, "disband_notify", s -> s.replace("{team}", teamName));
            }
            module.getTeamManager().disbandTeam(teamName);
            player.closeInventory();
            MessageUtil.sendChat(player, "disband", s -> s.replace("{team}", teamName));
            MessageUtil.sendActionbar(player, "disband_ab");
            SoundUtil.play(player, "error");
        } else if (slot == cancelSlot) {
            SoundUtil.play(player, "click");
            openMain(player, playerPage.getOrDefault(player.getUniqueId(), 0));
        }
    }
}
