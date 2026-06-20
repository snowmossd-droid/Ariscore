package me.vennlmao.ariscore.auction.gui;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.auction.managers.AuctionConfigManager;
import me.vennlmao.ariscore.auction.managers.GUIManager;
import me.vennlmao.ariscore.auction.managers.LangManager;
import me.vennlmao.ariscore.auction.utils.AuctionItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MyAuctionsGUI implements Listener {

    private final ArisCore plugin;
    private final Map<UUID, Integer> playerPage = new HashMap<>();
    private final Map<UUID, ScheduledTask> autoUpdateTasks = new HashMap<>();

    public MyAuctionsGUI(ArisCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        GUIManager gui = plugin.getAuctionModule().getGuiManager();
        AuctionConfigManager cfg = plugin.getAuctionModule().getConfigManager();
        LangManager lang = plugin.getAuctionModule().getLangManager();

        List<AuctionItem> myAuctions = plugin.getAuctionModule().getAuctionManager().getPlayerAuctions(player.getUniqueId());
        List<Integer> auctionSlots = gui.getMyItemsAuctionSlots();
        int perPage = auctionSlots.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) myAuctions.size() / perPage));
        page = Math.max(1, Math.min(page, totalPages));
        playerPage.put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, gui.getMyItemsSize(), gui.getMyItemsTitle(page, totalPages));

        if (gui.fillerEnabled(gui.getMyItemsGui())) {
            ItemStack filler = gui.buildFiller(gui.getMyItemsGui());
            for (int slot : gui.getFillerSlots(gui.getMyItemsGui())) inv.setItem(slot, filler);
        }

        List<AuctionItem> pageItems = myAuctions.subList(Math.min((page - 1) * perPage, myAuctions.size()), Math.min(page * perPage, myAuctions.size()));
        for (int i = 0; i < pageItems.size(); i++) {
            inv.setItem(auctionSlots.get(i), gui.createAuctionDisplayItem(pageItems.get(i), cfg));
        }

        if (page > 1) inv.setItem(gui.getMyItemsPrevSlot(), gui.getMyItemsPrevItem());
        if (page < totalPages) inv.setItem(gui.getMyItemsNextSlot(), gui.getMyItemsNextItem());
        inv.setItem(gui.getMyItemsInfoSlot(), gui.getMyItemsInfoItem(player));
        inv.setItem(gui.getMyItemsTransactionsSlot(), gui.getMyItemsTransactionsItem());

        player.openInventory(inv);
        startAutoUpdate(player);
    }

    private void startAutoUpdate(Player player) {
        stopAutoUpdate(player.getUniqueId());
        int ticks = plugin.getAuctionModule().getGuiManager().getMyItemsAutoUpdate();
        ScheduledTask task = Bukkit.getGlobalRegionScheduler().runAtFixedRate((Plugin) plugin, scheduledTask -> {
            if (player.isOnline() && isMyAuctionsGUI(player.getOpenInventory().getTopInventory())) {
                open(player, playerPage.getOrDefault(player.getUniqueId(), 1));
            } else {
                stopAutoUpdate(player.getUniqueId());
            }
        }, Math.max(1L, ticks), Math.max(1L, ticks));
        autoUpdateTasks.put(player.getUniqueId(), task);
    }

    private void stopAutoUpdate(UUID uuid) {
        ScheduledTask t = autoUpdateTasks.remove(uuid);
        if (t != null) t.cancel();
    }

    private boolean isMyAuctionsGUI(Inventory inv) {
        if (inv == null || inv.getViewers().isEmpty()) return false;
        String title = inv.getViewers().get(0).getOpenInventory().getTitle();
        GUIManager gui = plugin.getAuctionModule().getGuiManager();
        return title != null && title.contains(gui.getMyItemsTitle(1, 1).replaceAll("\\(Page .*\\)", "").trim());
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        stopAutoUpdate(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (!isMyAuctionsGUI(event.getInventory())) return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        GUIManager gui = plugin.getAuctionModule().getGuiManager();
        AuctionConfigManager cfg = plugin.getAuctionModule().getConfigManager();
        LangManager lang = plugin.getAuctionModule().getLangManager();
        int slot = event.getRawSlot();
        int page = playerPage.getOrDefault(player.getUniqueId(), 1);

        cfg.playSound(player, "click");

        if (slot == gui.getMyItemsPrevSlot()) { open(player, page - 1); return; }
        if (slot == gui.getMyItemsNextSlot()) { open(player, page + 1); return; }
        if (slot == gui.getMyItemsTransactionsSlot()) {
            plugin.getAuctionModule().getTransactionsGUI().open(player, 1);
            return;
        }

        List<Integer> auctionSlots = gui.getMyItemsAuctionSlots();
        if (!auctionSlots.contains(slot)) return;

        List<AuctionItem> myAuctions = plugin.getAuctionModule().getAuctionManager().getPlayerAuctions(player.getUniqueId());
        int perPage = auctionSlots.size();
        int index = (page - 1) * perPage + auctionSlots.indexOf(slot);
        if (index >= myAuctions.size()) return;

        AuctionItem auction = myAuctions.get(index);
        boolean removed = plugin.getAuctionModule().getAuctionManager().removeAuction(auction.getAuctionId(), player);
        if (removed) {
            player.sendMessage(lang.getAuctionRemoved());
            cfg.playSound(player, "reload");
        }
        open(player, page);
    }
}
