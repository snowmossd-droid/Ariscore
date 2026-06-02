package me.vennlmao.ariscore.team.gui;

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
import java.util.stream.Collectors;

public class TeamGuiBuilder {

    private final TeamModule module;
    public static final int PAGE_SIZE = 45;

    public TeamGuiBuilder(TeamModule module) { this.module = module; }

    public Inventory buildMain(Player viewer, TeamData team, int page, TeamData.SortType sort) {
        String title = module.getConfig().getString("gui.main.title", "&8ᴛᴇᴀᴍ (Page {page})")
                .replace("{page}", String.valueOf(page + 1))
                .replace("{team-name}", team.getName());
        int size = module.getConfig().getInt("gui.main.size", 54);
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.parse(title));

        List<TeamData.MemberData> sorted = sortMembers(team, sort);
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, sorted.size());

        for (int i = start; i < end; i++) {
            TeamData.MemberData md = sorted.get(i);
            inv.setItem(i - start, buildMemberHead(md, team));
        }

        int maxMembers = module.getConfig().getInt("team.max-members", 45);
        for (int i = end - start; i < Math.min(PAGE_SIZE, maxMembers); i++) {
            inv.setItem(i, buildEmptySlot());
        }

        placeControl(inv, "items.search", s -> s);
        placeControl(inv, "items.sort", s -> s);
        placeControl(inv, "items.team-info", s -> s
                .replace("{team-name}", team.getName())
                .replace("{count}", String.valueOf(team.getMemberCount()))
                .replace("{max}", String.valueOf(maxMembers)));
        placeControl(inv, "items.team-home", s -> s);
        placeControl(inv, "items.pvp", s -> {
            String status = team.isPvpEnabled()
                    ? module.getConfig().getString("items.pvp.status.enabled", "ON")
                    : module.getConfig().getString("items.pvp.status.disabled", "OFF");
            return s.replace("{pvp-status}", status);
        });

        if (page > 0) placeControl(inv, "items.back", s -> s);
        if (end < sorted.size()) placeControl(inv, "items.next", s -> s);

        return inv;
    }

    private List<TeamData.MemberData> sortMembers(TeamData team, TeamData.SortType sort) {
        List<TeamData.MemberData> list = new ArrayList<>(team.getMembers().values());
        if (sort == null) sort = TeamData.SortType.JOIN_DATE;
        switch (sort) {
            case JOIN_DATE -> list.sort(Comparator.comparingLong(m -> m.joinDate));
            case ALPHABETICALLY -> list.sort(Comparator.comparing(m -> {
                OfflinePlayer op = Bukkit.getOfflinePlayer(m.uuid);
                return op.getName() != null ? op.getName().toLowerCase() : "";
            }));
            case ONLINE_MEMBERS -> list.sort((a, b) -> {
                boolean aOnline = Bukkit.getPlayer(a.uuid) != null;
                boolean bOnline = Bukkit.getPlayer(b.uuid) != null;
                return Boolean.compare(bOnline, aOnline);
            });
        }
        list.sort((a, b) -> {
            int ra = roleOrder(a.role), rb = roleOrder(b.role);
            return Integer.compare(ra, rb);
        });
        return list;
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

        String nameColor = module.getConfig().getString("teammate-display.name-color", "&#34eb9b");
        String name = nameColor + (op.getName() != null ? op.getName() : "Unknown");
        if (md.role == TeamData.Role.OWNER) name += module.getConfig().getString("teammate-display.leader-indicator", " &6★");
        meta.displayName(ColorUtil.parse(name));

        List<Component> lore = new ArrayList<>();
        String onlineInd = module.getConfig().getString("items.player-head.online-indicator", "&a● Online");
        String offlineInd = module.getConfig().getString("items.player-head.offline-indicator", "&c● Offline");
        lore.add(ColorUtil.parse(online ? onlineInd : offlineInd));
        lore.add(ColorUtil.parse("&7" + team.getRoleString(md.uuid)));
        lore.add(ColorUtil.parse("&fClick to manage"));
        meta.lore(lore);
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack buildEmptySlot() {
        Material mat = Material.matchMaterial(module.getConfig().getString("items.empty-member.material", "GRAY_STAINED_GLASS_PANE"));
        if (mat == null) mat = Material.GRAY_STAINED_GLASS_PANE;
        String name = module.getConfig().getString("items.empty-member.display-name", "&#34eb9bɪɴᴠɪᴛᴇ");
        List<String> lore = module.getConfig().getStringList("items.empty-member.lore");
        return buildItem(mat, name, lore, s -> s);
    }

    public Inventory buildSortGui() {
        String title = module.getConfig().getString("sort-gui.title", "&#34eb9bѕᴏʀᴛ");
        int size = module.getConfig().getInt("sort-gui.size", 27);
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.parse(title));

        ConfigurationSection opts = module.getConfig().getConfigurationSection("sort-gui.options");
        if (opts != null) {
            for (String key : opts.getKeys(false)) {
                String path = "sort-gui.options." + key;
                int slot = module.getConfig().getInt(path + ".slot", 0);
                Material mat = Material.matchMaterial(module.getConfig().getString(path + ".material", "PAPER"));
                if (mat == null) mat = Material.PAPER;
                String name = module.getConfig().getString(path + ".display-name", key);
                inv.setItem(slot, buildItem(mat, name, List.of(), s -> s));
            }
        }
        return inv;
    }

    public Inventory buildPermissionGui(TeamData team, UUID targetUuid) {
        String title = module.getConfig().getString("permission-gui.title", "ᴇᴅɪᴛ ᴘᴇʀᴍɪssɪᴏɴs");
        int size = module.getConfig().getInt("permission-gui.size", 27);
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.parse(title));

        OfflinePlayer op = Bukkit.getOfflinePlayer(targetUuid);
        String playerName = op.getName() != null ? op.getName() : "Unknown";
        TeamData.MemberData md = team.getMemberData(targetUuid);
        if (md == null) return inv;

        ConfigurationSection perms = module.getConfig().getConfigurationSection("permission-gui.permissions");
        if (perms != null) {
            for (String key : perms.getKeys(false)) {
                String path = "permission-gui.permissions." + key;
                int slot = module.getConfig().getInt(path + ".slot", 0);
                Material mat = Material.matchMaterial(module.getConfig().getString(path + ".material", "PAPER"));
                if (mat == null) mat = Material.PAPER;
                String name = module.getConfig().getString(path + ".name", module.getConfig().getString(path + ".display-name", key))
                        .replace("{player-name}", playerName);
                List<String> lore = module.getConfig().getStringList(path + ".lore");

                boolean enabled = getPermValue(md, key);
                String status = enabled ? "&a&lON" : "&c&lOFF";
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
        String title = module.getConfig().getString(configKey + ".title", "Confirm");
        int size = module.getConfig().getInt(configKey + ".size", 27);
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.parse(title));

        ConfigurationSection cancelSec = module.getConfig().getConfigurationSection(configKey + ".cancel");
        if (cancelSec != null) {
            int slot = cancelSec.getInt("slot", 11);
            Material mat = Material.matchMaterial(cancelSec.getString("material", "RED_STAINED_GLASS_PANE"));
            if (mat == null) mat = Material.RED_STAINED_GLASS_PANE;
            inv.setItem(slot, buildItem(mat, cancelSec.getString("display-name", "&cCancel"), cancelSec.getStringList("lore"), s -> s));
        }

        ConfigurationSection confirmSec = module.getConfig().getConfigurationSection(configKey + ".confirm");
        if (confirmSec != null) {
            int slot = confirmSec.getInt("slot", 15);
            Material mat = Material.matchMaterial(confirmSec.getString("material", "LIME_STAINED_GLASS_PANE"));
            if (mat == null) mat = Material.LIME_STAINED_GLASS_PANE;
            inv.setItem(slot, buildItem(mat, confirmSec.getString("display-name", "&aConfirm"), confirmSec.getStringList("lore"), s -> s));
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

    private void placeControl(Inventory inv, String path, java.util.function.UnaryOperator<String> replacer) {
        ConfigurationSection sec = module.getConfig().getConfigurationSection(path);
        if (sec == null) return;
        int slot = sec.getInt("slot", 0);
        Material mat = Material.matchMaterial(sec.getString("material", "STONE"));
        if (mat == null) mat = Material.STONE;
        String name = replacer.apply(sec.getString("display-name", sec.getString("name", "")));
        inv.setItem(slot, buildItem(mat, name, sec.getStringList("lore"), replacer));
    }

    private ItemStack buildItem(Material mat, String name, List<String> lore, java.util.function.UnaryOperator<String> replacer) {
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
