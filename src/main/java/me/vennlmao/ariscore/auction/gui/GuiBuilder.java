package me.vennlmao.ariscore.auction.gui;

import me.vennlmao.ariscore.auction.AuctionModule;
import me.vennlmao.ariscore.auction.data.AuctionListing;
import me.vennlmao.ariscore.auction.data.Transaction;
import me.vennlmao.ariscore.auction.utils.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GuiBuilder {

    public static Inventory buildMain(AuctionModule m, Player p, List<AuctionListing> listings, int page, String sort, String filter) {
        int listingSlots = m.getConfig().getInt("gui.listing-slots", 45);
        String rawTitle = m.getConfig().getString("gui.title", "ᴀᴜᴄᴛɪᴏɴ (Page {page})");
        int totalPages = Math.max(1, (int) Math.ceil((double) listings.size() / listingSlots));
        String title = rawTitle.replace("{page}", String.valueOf(page + 1)).replace("{pages}", String.valueOf(totalPages));
        Inventory inv = Bukkit.createInventory(null, m.getConfig().getInt("gui.size", 54), ColorUtil.parse(title));

        int start = page * listingSlots;
        for (int i = 0; i < listingSlots && start + i < listings.size(); i++) {
            AuctionListing listing = listings.get(start + i);
            ItemStack display = listing.getItem().clone();
            ItemMeta meta = display.hasItemMeta() ? display.getItemMeta() : Bukkit.getItemFactory().getItemMeta(display.getType());
            if (meta == null) continue;
            List<Component> lore = new ArrayList<>();
            if (meta.hasLore()) lore.addAll(meta.lore());
            lore.add(Component.empty());
            for (String l : m.getConfig().getStringList("listing-display.lore")) {
                lore.add(ColorUtil.parse(l.replace("{price}", m.getAuctionManager().format(listing.getPrice()))));
            }
            boolean own = listing.getSellerUuid().equals(p.getUniqueId());
            String action = own ? m.getConfig().getString("listing-display.own-action","&eClick to collect")
                    : m.getConfig().getString("listing-display.buy-action","&aClick to buy");
            lore.add(ColorUtil.parse(action));
            if (display.getType().name().contains("SHULKER")) {
                lore.add(ColorUtil.parse(m.getConfig().getString("listing-display.shulker-hint","&7Click to preview")));
            }
            meta.lore(lore);
            display.setItemMeta(meta);
            inv.setItem(i, display);
        }

        placeNavItem(m, inv, "gui.items.back", page > 0);
        placeNavItem(m, inv, "gui.items.next", start + listingSlots < listings.size());
        placeNavItemAlways(m, inv, "gui.items.sort", buildSortLore(m, sort));
        placeNavItemAlways(m, inv, "gui.items.filter", buildFilterLore(m, filter));
        placeNavItemAlways(m, inv, "gui.items.refresh", null);
        placeNavItemAlways(m, inv, "gui.items.search", null);
        placeNavItemAlways(m, inv, "gui.items.your_listings", null);

        return inv;
    }

    public static Inventory buildYourListings(AuctionModule m, Player p, int page) {
        String title = m.getConfig().getString("your-listings-menu.title", "ᴀᴜᴄᴛɪᴏɴ -> ʏᴏᴜʀ ɪᴛᴇᴍѕ");
        int size = m.getConfig().getInt("your-listings-menu.size", 27);
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.parse(title));

        List<AuctionListing> all = m.getAuctionManager().getSellerListings(p.getUniqueId());
        int navSlots = 2;
        int perPage = size - 9 - navSlots;
        int start = page * perPage;

        for (int i = 0; i < perPage && start + i < all.size(); i++) {
            AuctionListing listing = all.get(start + i);
            ItemStack display = listing.getItem().clone();
            ItemMeta meta = display.hasItemMeta() ? display.getItemMeta() : Bukkit.getItemFactory().getItemMeta(display.getType());
            if (meta == null) continue;
            List<Component> lore = new ArrayList<>();
            if (meta.hasLore()) lore.addAll(meta.lore());
            lore.add(Component.empty());
            for (String l : m.getConfig().getStringList("your-listings-menu.listing-lore")) {
                lore.add(ColorUtil.parse(l.replace("{price}", m.getAuctionManager().format(listing.getPrice()))
                        .replace("{expires}", listing.getTimeLeftFormatted())));
            }
            meta.lore(lore);
            display.setItemMeta(meta);
            inv.setItem(i, display);
        }

        placeNavItemAlways(m, inv, "your-listings-menu.items.sell", null);
        placeNavItemAlways(m, inv, "your-listings-menu.items.transactions", null);
        placeNavItem(m, inv, "your-listings-menu.items.back_page", page > 0);
        placeNavItem(m, inv, "your-listings-menu.items.next_page", start + perPage < all.size());

        return inv;
    }

    public static Inventory buildSellInput(AuctionModule m) {
        String title = m.getConfig().getString("sell-item-input-menu.title", "ɪɴѕᴇʀᴛ ᴀ ɪᴛᴇᴍ");
        Inventory inv = Bukkit.createInventory(null, 9, ColorUtil.parse(title));
        Material filler = Material.matchMaterial(m.getConfig().getString("sell-item-input-menu.filler-material","GRAY_STAINED_GLASS_PANE"));
        if (filler == null) filler = Material.GRAY_STAINED_GLASS_PANE;
        ItemStack fill = makeItem(filler, Component.empty(), List.of());
        for (int i = 0; i < 9; i++) inv.setItem(i, fill);
        inv.setItem(m.getConfig().getInt("sell-item-input-menu.items.cancel.slot",3), makeFromConfig(m,"sell-item-input-menu.items.cancel"));
        inv.setItem(m.getConfig().getInt("sell-item-input-menu.items.confirm.slot",5), makeFromConfig(m,"sell-item-input-menu.items.confirm"));
        inv.setItem(4, null);
        return inv;
    }

    public static Inventory buildSellConfirm(AuctionModule m, AuctionListing listing) {
        String title = m.getConfig().getString("sell-confirm-menu.title","ᴄᴏɴꜰɪʀᴍ ʟɪѕᴛɪɴɢ");
        int size = m.getConfig().getInt("sell-confirm-menu.size",27);
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.parse(title));
        inv.setItem(m.getConfig().getInt("sell-confirm-menu.items.cancel.slot",11), makeFromConfig(m,"sell-confirm-menu.items.cancel"));
        inv.setItem(m.getConfig().getInt("sell-confirm-menu.items.confirm.slot",15), makeFromConfig(m,"sell-confirm-menu.items.confirm"));
        ItemStack info = listing.getItem().clone();
        ItemMeta meta = info.hasItemMeta() ? info.getItemMeta() : Bukkit.getItemFactory().getItemMeta(info.getType());
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            if (meta.hasLore()) lore.addAll(meta.lore());
            lore.add(Component.empty());
            for (String l : m.getConfig().getStringList("sell-confirm-menu.info-lore")) {
                lore.add(ColorUtil.parse(l.replace("{price}", m.getAuctionManager().format(listing.getPrice()))));
            }
            meta.lore(lore);
            info.setItemMeta(meta);
        }
        inv.setItem(13, info);
        return inv;
    }

    public static Inventory buildBuyConfirm(AuctionModule m, AuctionListing listing) {
        String title = m.getConfig().getString("buy-confirm-menu.title","ᴄᴏɴꜰɪʀᴍ ᴘᴜʀᴄʜᴀѕᴇ");
        int size = m.getConfig().getInt("buy-confirm-menu.size",27);
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.parse(title));
        inv.setItem(m.getConfig().getInt("buy-confirm-menu.items.cancel.slot",11), makeFromConfig(m,"buy-confirm-menu.items.cancel"));
        inv.setItem(m.getConfig().getInt("buy-confirm-menu.items.confirm.slot",15), makeFromConfig(m,"buy-confirm-menu.items.confirm"));
        ItemStack info = listing.getItem().clone();
        ItemMeta meta = info.hasItemMeta() ? info.getItemMeta() : Bukkit.getItemFactory().getItemMeta(info.getType());
        if (meta != null) {
            List<Component> lore = new ArrayList<>();
            if (meta.hasLore()) lore.addAll(meta.lore());
            lore.add(Component.empty());
            for (String l : m.getConfig().getStringList("buy-confirm-menu.info-lore")) {
                lore.add(ColorUtil.parse(l.replace("{price}", m.getAuctionManager().format(listing.getPrice()))
                        .replace("{seller}", listing.getSellerName())
                        .replace("{time_left}", listing.getTimeLeftFormatted())));
            }
            meta.lore(lore);
            info.setItemMeta(meta);
        }
        inv.setItem(13, info);
        return inv;
    }

    public static Inventory buildTransactions(AuctionModule m, Player p, int page) {
        String rawTitle = m.getConfig().getString("transactions-menu.title","ᴛʀᴀɴѕᴀᴄᴛɪᴏɴѕ (Page {page})");
        int size = m.getConfig().getInt("transactions-menu.size",54);
        List<Transaction> all = m.getAuctionManager().getTransactions(p.getUniqueId());
        int listSlots = size - 9;
        int total = Math.max(1,(int)Math.ceil((double)all.size()/listSlots));
        String title = rawTitle.replace("{page}",String.valueOf(page+1)).replace("{pages}",String.valueOf(total));
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.parse(title));

        int start = page * listSlots;
        for (int i = 0; i < listSlots && start+i < all.size(); i++) {
            Transaction tx = all.get(all.size()-1-(start+i));
            ItemStack display = tx.getItem().clone();
            ItemMeta meta = display.hasItemMeta() ? display.getItemMeta() : Bukkit.getItemFactory().getItemMeta(display.getType());
            if (meta == null) continue;
            List<Component> lore = new ArrayList<>();
            String itemName = display.getType().name().toLowerCase().replace("_"," ");
            List<String> loreCfg = tx.getType() == Transaction.Type.BOUGHT
                    ? m.getConfig().getStringList("transactions-menu.bought-lore")
                    : m.getConfig().getStringList("transactions-menu.sold-lore");
            for (String l : loreCfg) {
                lore.add(ColorUtil.parse(l.replace("{other}", tx.getOtherName())
                        .replace("{item}", itemName)
                        .replace("{price}", m.getAuctionManager().format(tx.getAmount()))
                        .replace("{time_ago}", tx.getTimeAgo())));
            }
            meta.lore(lore);
            display.setItemMeta(meta);
            inv.setItem(i, display);
        }

        double made = m.getAuctionManager().getTotalMade(p.getUniqueId());
        double spent = m.getAuctionManager().getTotalSpent(p.getUniqueId());
        int statsSlot = m.getConfig().getInt("transactions-menu.items.stats.slot",47);
        Material statsMat = Material.matchMaterial(m.getConfig().getString("transactions-menu.items.stats.material","PAPER"));
        if (statsMat == null) statsMat = Material.PAPER;
        List<Component> statsLore = new ArrayList<>();
        for (String l : m.getConfig().getStringList("transactions-menu.items.stats.lore")) {
            statsLore.add(ColorUtil.parse(l.replace("{spent}", m.getAuctionManager().format(spent))
                    .replace("{earned}", m.getAuctionManager().format(made))));
        }
        inv.setItem(statsSlot, makeItem(statsMat, ColorUtil.parse(m.getConfig().getString("transactions-menu.items.stats.name","&aѕᴛᴀᴛѕ")), statsLore));
        placeNavItemAlways(m, inv, "transactions-menu.items.refresh", null);
        placeNavItemAlways(m, inv, "transactions-menu.items.search", null);
        return inv;
    }

    private static List<Component> buildSortLore(AuctionModule m, String current) {
        String sel = m.getConfig().getString("selection-format.selected-prefix","&a• ");
        String unsel = m.getConfig().getString("selection-format.unselected-prefix","&f• ");
        List<Component> lore = new ArrayList<>();
        for (String opt : m.getConfig().getStringList("gui.items.sort.lore")) {
            boolean active = opt.trim().equalsIgnoreCase(current.replace("_"," "));
            lore.add(ColorUtil.parse((active ? sel : unsel) + opt.trim()));
        }
        return lore;
    }

    private static List<Component> buildFilterLore(AuctionModule m, String current) {
        String sel = m.getConfig().getString("selection-format.selected-prefix","&a• ");
        String unsel = m.getConfig().getString("selection-format.unselected-prefix","&f• ");
        List<Component> lore = new ArrayList<>();
        for (String opt : m.getConfig().getStringList("gui.items.filter.lore")) {
            boolean active = opt.trim().equalsIgnoreCase(current);
            lore.add(ColorUtil.parse((active ? sel : unsel) + opt.trim()));
        }
        return lore;
    }

    private static void placeNavItem(AuctionModule m, Inventory inv, String path, boolean show) {
        if (!show) return;
        placeNavItemAlways(m, inv, path, null);
    }

    private static void placeNavItemAlways(AuctionModule m, Inventory inv, String path, List<Component> overrideLore) {
        int slot = m.getConfig().getInt(path + ".slot", -1);
        if (slot < 0 || slot >= inv.getSize()) return;
        String matStr = m.getConfig().getString(path + ".material", "STONE");
        Material mat = Material.matchMaterial(matStr);
        if (mat == null) mat = Material.STONE;
        Component name = ColorUtil.parse(m.getConfig().getString(path + ".name", ""));
        List<Component> lore = overrideLore != null ? overrideLore : new ArrayList<>();
        if (overrideLore == null) {
            for (String l : m.getConfig().getStringList(path + ".lore")) {
                lore.add(ColorUtil.parse(l));
            }
        }
        inv.setItem(slot, makeItem(mat, name, lore));
    }

    public static ItemStack makeFromConfig(AuctionModule m, String path) {
        String matStr = m.getConfig().getString(path + ".material", "STONE");
        Material mat = Material.matchMaterial(matStr);
        if (mat == null) mat = Material.STONE;
        Component name = ColorUtil.parse(m.getConfig().getString(path + ".name", ""));
        List<Component> lore = new ArrayList<>();
        for (String l : m.getConfig().getStringList(path + ".lore")) lore.add(ColorUtil.parse(l));
        return makeItem(mat, name, lore);
    }

    public static ItemStack makeItem(Material mat, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(name);
        if (!lore.isEmpty()) meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
