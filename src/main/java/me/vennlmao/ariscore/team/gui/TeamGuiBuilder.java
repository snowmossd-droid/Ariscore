package me.vennlmao.ariscore.team.gui;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.team.TeamModule;
import me.vennlmao.ariscore.team.managers.TeamData;
import me.vennlmao.ariscore.team.utils.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.function.UnaryOperator;

public class TeamGuiBuilder {

    private final TeamModule module;
    public static final int PAGE_SIZE = 45;

    public TeamGuiBuilder(TeamModule module) { this.module = module; }

    public Inventory buildMain(Player viewer, TeamData team, int page) {
        String title = module.getConfig().getString("gui.main.title", "")
                .replace("{page}", String.valueOf(page + 1))
                .replace("{team-name}", team.getName());
        int size = module.getConfig().getInt("gui.main.size");
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.parse(title));

        List<TeamData.MemberData> sorted = new ArrayList<>(team.getMembers().values());
        sorted.sort(Comparator.comparingLong(m -> m.joinDate));
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, sorted.size());

        for (int i = start; i < end; i++) {
            inv.setItem(i - start, buildMemberHead(sorted.get(i), team));
        }

        int maxMembers = module.getConfig().getInt("team.max-members");
        for (int i = end - start; i < Math.min(PAGE_SIZE, maxMembers); i++) {
            inv.setItem(i, buildEmptySlot());
        }

        placeControl(inv, "items.team-info", s -> s
                .replace("{team-name}", team.getName())
                .replace("{count}", String.valueOf(team.getMemberCount()))
                .replace("{max}", String.valueOf(maxMembers)));
        placeControl(inv, "items.team-home", s -> s);
        placeControl(inv, "items.pvp", s -> {
            String status = team.isPvpEnabled()
                    ? module.getConfig().getString("items.pvp.status.enabled", "")
                    : module.getConfig().getString("items.pvp.status.disabled", "");
            return s.replace("{pvp-status}", status);
        });

        placeControl(inv, "items.back", s -> s);
        placeControl(inv, "items.next", s -> s);

        return inv;
    }


    private int countPerms(TeamData.MemberData md) {
        int count = 0;
        if (md.permEditHome) count++;
        if (md.permKick) count++;
        if (md.permManageTeammates) count++;
        if (md.permPvpToggle) count++;
        if (md.permVisitHome) count++;
        if (md.permTeamChat) count++;
        if (md.permInvite) count++;
        return count;
    }

    private double getBalance(UUID uuid) {
        try {
            net.milkbowl.vault.economy.Economy eco = ArisCore.getInstance().getShopModule().getEconomy();
            if (eco != null) return eco.getBalance(Bukkit.getOfflinePlayer(uuid));
        } catch (Exception ignored) {}
        return 0;
    }

    private int roleOrder(TeamData.Role r) {
        return switch (r) { case OWNER -> 0; case CO_OWNER -> 1; case MEMBER -> 2; };
    }

    private ItemStack buildMemberHead(TeamData.MemberData md, TeamData team) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(md.uuid);
        boolean online = Bukkit.getPlayer(md.uuid) != null;

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;
        meta.setOwningPlayer(op);

        String nameColor = module.getConfig().getString("teammate-display.name-color", "");
        String name = nameColor + (op.getName() != null ? op.getName() : "Unknown");
        if (md.role == TeamData.Role.OWNER) name += module.getConfig().getString("teammate-display.leader-indicator", "");
        meta.displayName(ColorUtil.parse(name));

        List<Component> lore = new ArrayList<>();
        String onlineInd = module.getConfig().getString("items.player-head.online-indicator", "");
        String offlineInd = module.getConfig().getString("items.player-head.offline-indicator", "");
        lore.add(ColorUtil.parse(online ? onlineInd : offlineInd));
        String roleColor = module.getConfig().getString("teammate-display.role-color", "");
        lore.add(ColorUtil.parse(roleColor + team.getRoleString(md.uuid)));
        String clickLore = module.getConfig().getString("items.player-head.click-lore", "");
        lore.add(ColorUtil.parse(clickLore));
        meta.lore(lore);
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack buildEmptySlot() {
        Material mat = Material.matchMaterial(module.getConfig().getString("items.empty-member.material", "STONE"));
        if (mat == null) mat = Material.GRAY_STAINED_GLASS_PANE;
        String name = module.getConfig().getString("items.empty-member.display-name", "");
        List<String> lore = module.getConfig().getStringList("items.empty-member.lore");
        return buildItem(mat, name, lore, s -> s);
    }

    public Inventory buildPermissionGui(TeamData team, UUID targetUuid) {
        String title = module.getConfig().getString("permission-gui.title", "");
        int size = module.getConfig().getInt("permission-gui.size");
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.parse(title));

        OfflinePlayer op = Bukkit.getOfflinePlayer(targetUuid);
        String playerName = op.getName() != null ? op.getName() : "Unknown";
        TeamData.MemberData md = team.getMemberData(targetUuid);
        if (md == null) return inv;

        ConfigurationSection perms = module.getConfig().getConfigurationSection("permission-gui.permissions");
        if (perms != null) {
            for (String key : perms.getKeys(false)) {
                String path = "permission-gui.permissions." + key;
                int slot = module.getConfig().getInt(path + ".slot");
                Material mat = Material.matchMaterial(module.getConfig().getString(path + ".material", "STONE"));
                if (mat == null) mat = Material.PAPER;
                String name = module.getConfig().getString(path + ".name", module.getConfig().getString(path + ".display-name", key))
                        .replace("{player-name}", playerName);
                List<String> lore = module.getConfig().getStringList(path + ".lore");

                boolean enabled = getPermValue(md, key);
                String status = enabled
                        ? module.getConfig().getString("permission-gui.status.enabled", "")
                        : module.getConfig().getString("permission-gui.status.disabled", "");
                final String pName = playerName;
                inv.setItem(slot, buildItem(mat, name, lore, s -> s.replace("{player-name}", pName).replace("{status}", status)));
            }
        }

        placeControl(inv, "permission-gui.back-button", s -> s);
        return inv;
    }

    public Inventory buildKickConfirm(OfflinePlayer target) {
        return buildConfirmGui("kick-confirmation-gui", target.getName());
    }

    public Inventory buildLeaveConfirm() {
        return buildConfirmGui("leave-confirmation-gui", null);
    }

    public Inventory buildDisbandConfirm() {
        return buildConfirmGui("disband-confirmation-gui", null);
    }

    private Inventory buildConfirmGui(String configKey, String playerName) {
        String title = module.getConfig().getString(configKey + ".title", "");
        int size = module.getConfig().getInt(configKey + ".size");
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.parse(title));

        ConfigurationSection cancelSec = module.getConfig().getConfigurationSection(configKey + ".cancel");
        if (cancelSec != null) {
            int slot = cancelSec.getInt("slot");
            Material mat = Material.matchMaterial(cancelSec.getString("material", "STONE"));
            if (mat == null) mat = Material.RED_STAINED_GLASS_PANE;
            inv.setItem(slot, buildItem(mat, cancelSec.getString("display-name", ""), cancelSec.getStringList("lore"), s -> s));
        }

        ConfigurationSection confirmSec = module.getConfig().getConfigurationSection(configKey + ".confirm");
        if (confirmSec != null) {
            int slot = confirmSec.getInt("slot");
            Material mat = Material.matchMaterial(confirmSec.getString("material", "STONE"));
            if (mat == null) mat = Material.LIME_STAINED_GLASS_PANE;
            inv.setItem(slot, buildItem(mat, confirmSec.getString("display-name", ""), confirmSec.getStringList("lore"), s -> s));
        }

        return inv;
    }

    public boolean getPermValue(TeamData.MemberData md, String key) {
        return switch (key) {
            case "edit-home" -> md.permEditHome;
            case "kick-player" -> md.permKick;
            case "manage-teammates" -> md.permManageTeammates;
            case "pvp-toggle" -> md.permPvpToggle;
            case "visit-home" -> md.permVisitHome;
            case "team-chat" -> md.permTeamChat;
            case "invite-players" -> md.permInvite;
            default -> false;
        };
    }

    public void togglePerm(TeamData.MemberData md, String key) {
        switch (key) {
            case "edit-home" -> md.permEditHome = !md.permEditHome;
            case "kick-player" -> md.permKick = !md.permKick;
            case "manage-teammates" -> md.permManageTeammates = !md.permManageTeammates;
            case "pvp-toggle" -> md.permPvpToggle = !md.permPvpToggle;
            case "visit-home" -> md.permVisitHome = !md.permVisitHome;
            case "team-chat" -> md.permTeamChat = !md.permTeamChat;
            case "invite-players" -> md.permInvite = !md.permInvite;
        }
    }

    private void placeControl(Inventory inv, String path, UnaryOperator<String> replacer) {
        ConfigurationSection sec = module.getConfig().getConfigurationSection(path);
        if (sec == null) return;
        int slot = sec.getInt("slot");
        Material mat = Material.matchMaterial(sec.getString("material", "STONE"));
        if (mat == null) mat = Material.STONE;
        String name = replacer.apply(sec.getString("display-name", sec.getString("name", "")));
        inv.setItem(slot, buildItem(mat, name, sec.getStringList("lore"), replacer));
    }

    private ItemStack buildItem(Material mat, String name, List<String> lore, UnaryOperator<String> replacer) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(ColorUtil.parse(name));
        List<Component> loreComp = new ArrayList<>();
        for (String l : lore) loreComp.add(ColorUtil.parse(replacer.apply(l)));
        meta.lore(loreComp);
        item.setItemMeta(meta);
        return item;
    }
}
