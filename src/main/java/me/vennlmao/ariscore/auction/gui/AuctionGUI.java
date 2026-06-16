package me.vennlmao.ariscore.auction.gui;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.auction.managers.AuctionConfigManager;
import me.vennlmao.ariscore.auction.managers.AuctionManager;
import me.vennlmao.ariscore.auction.managers.GUIManager;
import me.vennlmao.ariscore.auction.managers.LangManager;
import me.vennlmao.ariscore.auction.utils.AuctionItem;
import me.vennlmao.ariscore.auction.utils.EcoUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class AuctionGUI implements Listener {

    private final ArisCore plugin;
    private final Map<UUID, Integer> playerPage = new HashMap<>();
    private final Map<UUID, GUIManager.SortType> playerSort = new HashMap<>();
    private final Map<UUID, String> playerFilter = new HashMap<>();
    private final Map<UUID, String> playerSearch = new HashMap<>();
    private final Map<UUID, ScheduledTask> autoUpdateTasks = new HashMap<>();

    public AuctionGUI(ArisCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        open(player, 1, null, null);
    }

    public void open(Player player, int page, String search, String filter) {
        GUIManager gui = plugin.getAuctionModule().getGuiManager();
        AuctionConfigManager cfg = plugin.getAuctionModule().getConfigManager();
        AuctionManager mgr = plugin.getAuctionModule().getAuctionManager();
        LangManager lang = plugin.getAuctionModule().getLangManager();

        if (search != null) playerSearch.put(player.getUniqueId(), search);
        else playerSearch.remove(player.getUniqueId());
        if (filter != null) playerFilter.put(player.getUniqueId(), filter);

        GUIManager.SortType sort = playerSort.getOrDefault(player.getUniqueId(), GUIManager.SortType.NEWEST);
        String currentFilter = playerFilter.getOrDefault(player.getUniqueId(), gui.getFilterOptions().isEmpty() ? "All" : gui.getFilterOptions().get(0));

        List<AuctionItem> auctions = mgr.getActiveAuctions();
        String currentSearch = playerSearch.get(player.getUniqueId());
        if (currentSearch != null && !currentSearch.isEmpty()) {
            String lower = currentSearch.toLowerCase();
            auctions = auctions.stream().filter(a -> lang.formatItemName(a.getItemStack()).toLowerCase().contains(lower)).collect(Collectors.toList());
        }
        auctions = sortAuctions(auctions, sort);

        List<Integer> auctionSlots = gui.getMainAuctionSlots();
        int perPage = auctionSlots.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) auctions.size() / perPage));
        page = Math.max(1, Math.min(page, totalPages));
        playerPage.put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, gui.getMainSize(), gui.getMainTitle(page));

        if (gui.fillerEnabled(gui.getMainGui())) {
            ItemStack filler = gui.buildFiller(gui.getMainGui());
            for (int slot : gui.getFillerSlots(gui.getMainGui())) inv.setItem(slot, filler);
        }

        List<AuctionItem> pageItems = auctions.subList(Math.min((page - 1) * perPage, auctions.size()), Math.min(page * perPage, auctions.size()));
        for (int i = 0; i < pageItems.size(); i++) {
            inv.setItem(auctionSlots.get(i), gui.createAuctionDisplayItem(pageItems.get(i), cfg));
        }

        if (page > 1) inv.setItem(gui.getPrevSlot(), gui.getPrevItem());
        if (page < totalPages) inv.setItem(gui.getNextSlot(), gui.getNextItem());
        inv.setItem(gui.getRefreshSlot(), gui.getRefreshItem(player));
        inv.setItem(gui.getSortSlot(), gui.getSortItem(sort));
        inv.setItem(gui.getFilterSlot(), gui.getFilterItem(currentFilter));
        inv.setItem(gui.getSearchSlot(), gui.getSearchItem());
        inv.setItem(gui.getMyItemsSlot(), gui.getMyItemsItem());

        player.openInventory(inv);
        startAutoUpdate(player);
    }

    private List<AuctionItem> sortAuctions(List<AuctionItem> list, GUIManager.SortType sort) {
        switch (sort) {
            case OLDEST: list.sort(Comparator.comparingLong(AuctionItem::getCreateTime)); break;
            case CHEAPEST: list.sort(Comparator.comparingDouble(AuctionItem::getPrice)); break;
            case PRICIEST: list.sort(Comparator.comparingDouble(AuctionItem::getPrice).reversed()); break;
            default: list.sort(Comparator.comparingLong(AuctionItem::getCreateTime).reversed()); break;
        }
        return list;
    }

    private void startAutoUpdate(Player player) {
        stopAutoUpdate(player.getUniqueId());
        int ticks = plugin.getAuctionModule().getGuiManager().getMainAutoUpdate();
        ScheduledTask task = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, t -> {
            if (player.isOnline() && isAuctionGUI(player.getOpenInventory().getTopInventory())) {
                int page = playerPage.getOrDefault(player.getUniqueId(), 1);
                open(player, page, playerSearch.get(player.getUniqueId()), null);
            } else {
                stopAutoUpdate(player.getUniqueId());
            }
        }, ticks, ticks);
        autoUpdateTasks.put(player.getUniqueId(), task);
    }

    private void stopAutoUpdate(UUID uuid) {
        ScheduledTask t = autoUpdateTasks.remove(uuid);
        if (t != null) t.cancel();
    }

    private boolean isAuctionGUI(Inventory inv) {
        if (inv == null) return false;
        GUIManager gui = plugin.getAuctionModule().getGuiManager();
        String title = inv.getViewers().isEmpty() ? "" : inv.getViewers().get(0).getOpenInventory().getTitle();
        return title != null && title.contains(gui.getMainTitle(1).replaceAll("\\(Page .*\\)", "").trim());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        stopAutoUpdate(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getInventory();
        if (!isAuctionGUI(inv)) return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        GUIManager gui = plugin.getAuctionModule().getGuiManager();
        AuctionConfigManager cfg = plugin.getAuctionModule().getConfigManager();
        int slot = event.getRawSlot();
        int page = playerPage.getOrDefault(player.getUniqueId(), 1);

        cfg.playSound(player, "click");

        if (slot == gui.getPrevSlot()) { open(player, page - 1, playerSearch.get(player.getUniqueId()), null); return; }
        if (slot == gui.getNextSlot()) { open(player, page + 1, playerSearch.get(player.getUniqueId()), null); return; }
        if (slot == gui.getRefreshSlot()) { open(player, page, playerSearch.get(player.getUniqueId()), null); return; }

        if (slot == gui.getSortSlot()) {
            GUIManager.SortType current = playerSort.getOrDefault(player.getUniqueId(), GUIManager.SortType.NEWEST);
            GUIManager.SortType[] values = GUIManager.SortType.values();
            GUIManager.SortType next = values[(current.ordinal() + 1) % values.length];
            playerSort.put(player.getUniqueId(), next);
            open(player, 1, playerSearch.get(player.getUniqueId()), null);
            return;
        }

        if (slot == gui.getFilterSlot()) {
            List<String> options = gui.getFilterOptions();
            String current = playerFilter.getOrDefault(player.getUniqueId(), options.isEmpty() ? "" : options.get(0));
            int idx = options.indexOf(current);
            String next = options.get((idx + 1) % options.size());
            open(player, 1, playerSearch.get(player.getUniqueId()), next);
            return;
        }

        if (slot == gui.getSearchSlot()) {
            plugin.getAuctionModule().getChatSignManager().requestInput(player, input -> open(player, 1, input, null));
            return;
        }

        if (slot == gui.getMyItemsSlot()) {
            plugin.getAuctionModule().getMyAuctionsGUI().open(player, 1);
            return;
        }

        List<Integer> auctionSlots = gui.getMainAuctionSlots();
        if (!auctionSlots.contains(slot)) return;

        List<AuctionItem> auctions = sortAuctions(plugin.getAuctionModule().getAuctionManager().getActiveAuctions(),
                playerSort.getOrDefault(player.getUniqueId(), GUIManager.SortType.NEWEST));
        String currentSearch = playerSearch.get(player.getUniqueId());
        if (currentSearch != null && !currentSearch.isEmpty()) {
            LangManager lang = plugin.getAuctionModule().getLangManager();
            String lower = currentSearch.toLowerCase();
            auctions = auctions.stream().filter(a -> lang.formatItemName(a.getItemStack()).toLowerCase().contains(lower)).collect(Collectors.toList());
        }

        int perPage = auctionSlots.size();
        int auctionIndex = (page - 1) * perPage + auctionSlots.indexOf(slot);
        if (auctionIndex >= auctions.size()) return;
        AuctionItem auction = auctions.get(auctionIndex);

        if (auction.getSellerUUID().equals(player.getUniqueId())) {
            player.sendMessage(plugin.getAuctionModule().getLangManager().getOwnAuction());
            return;
        }

        boolean isFastBuy = plugin.getAuctionModule().getPlayerDataManager().getFastBuy(player.getUniqueId());
        if (isFastBuy) {
            plugin.getAuctionModule().getAuctionManager().purchaseAuction(player, auction.getAuctionId());
            open(player, page, currentSearch, null);
        } else {
            plugin.getAuctionModule().getConfirmPurchaseGUI().open(player, auction, this, page);
        }
    }

    private me.vennlmao.ariscore.auction.gui.MyAuctionsGUI myAuctionsGUI;

    public void setMyAuctionsGUI(me.vennlmao.ariscore.auction.gui.MyAuctionsGUI gui) { this.myAuctionsGUI = gui; }
    }
