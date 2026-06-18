package me.vennlmao.ariscore.auction.gui;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.auction.managers.AuctionConfigManager;
import me.vennlmao.ariscore.auction.managers.AuctionDataManager;
import me.vennlmao.ariscore.auction.managers.GUIManager;
import me.vennlmao.ariscore.auction.managers.LangManager;
import me.vennlmao.ariscore.auction.utils.ColorUtil;
import me.vennlmao.ariscore.auction.utils.EcoUtil;
import me.vennlmao.ariscore.auction.utils.ItemSerializer;
import me.vennlmao.ariscore.auction.utils.Transaction;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.plugin.Plugin;
import java.util.concurrent.TimeUnit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TransactionsGUI implements Listener {

    private final ArisCore plugin;
    private final Map<UUID, Integer> playerPage = new HashMap<>();
    private final Map<UUID, String> playerSearch = new HashMap<>();
    private final Map<UUID, ScheduledTask> autoUpdateTasks = new HashMap<>();
    private static final int PER_PAGE = 45;

    public TransactionsGUI(ArisCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        open(player, page, null);
    }

    public void open(Player player, int page, String search) {
        GUIManager gui = plugin.getAuctionModule().getGuiManager();
        AuctionConfigManager cfg = plugin.getAuctionModule().getConfigManager();
        AuctionDataManager data = plugin.getAuctionModule().getAuctionManager().getDataManager();
        LangManager lang = plugin.getAuctionModule().getLangManager();

        if (search != null) playerSearch.put(player.getUniqueId(), search);
        String currentSearch = playerSearch.get(player.getUniqueId());

        List<Integer> txSlots = gui.getTransactionSlots();
        int perPage = txSlots.size();

        List<Transaction> transactions = currentSearch != null && !currentSearch.isEmpty()
                ? data.searchPlayerTransactions(player.getUniqueId(), currentSearch, page, perPage)
                : data.getPlayerTransactions(player.getUniqueId(), page, perPage);

        int totalCount = data.getPlayerTransactionCount(player.getUniqueId());
        int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / perPage));
        page = Math.max(1, Math.min(page, totalPages));
        playerPage.put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, gui.getTransactionsSize(), gui.getTransactionsTitle(page));

        if (gui.fillerEnabled(gui.getTransactionsGui())) {
            ItemStack filler = gui.buildFiller(gui.getTransactionsGui());
            for (int slot : gui.getFillerSlots(gui.getTransactionsGui())) inv.setItem(slot, filler);
        }

        for (int i = 0; i < transactions.size() && i < txSlots.size(); i++) {
            inv.setItem(txSlots.get(i), buildTransactionItem(transactions.get(i), cfg, lang, gui, player));
        }

        if (page > 1) inv.setItem(gui.getTransactionsPrevSlot(), gui.getTransactionsPrevItem());
        if (page < totalPages) inv.setItem(gui.getTransactionsNextSlot(), gui.getTransactionsNextItem());

        double spent = data.getTotalSpent(player.getUniqueId());
        double made = data.getTotalMade(player.getUniqueId());
        inv.setItem(gui.getTransactionsStatsSlot(), gui.getTransactionsStatsItem(spent, made, cfg));
        inv.setItem(gui.getTransactionsRefreshSlot(), gui.getTransactionsRefreshItem());
        inv.setItem(gui.getTransactionsSearchSlot(), gui.getTransactionsSearchItem());

        player.openInventory(inv);
        startAutoUpdate(player);
    }

    private ItemStack buildTransactionItem(Transaction t, AuctionConfigManager cfg, LangManager lang, GUIManager gui, Player viewer) {
        ItemStack base = ItemSerializer.fromBase64(t.getItemBase64());
        if (base == null) base = new ItemStack(Material.PAPER);
        ItemStack display = base.clone();
        ItemMeta meta = display.getItemMeta();

        String action = t.getType() == Transaction.Type.PURCHASE
                ? (t.getBuyerUUID().equals(viewer.getUniqueId()) ? "Bought from" : "Sold to")
                : (t.getSellerUUID().equals(viewer.getUniqueId()) ? "Sold to" : "Bought from");
        String other = t.getType() == Transaction.Type.PURCHASE
                ? (t.getBuyerUUID().equals(viewer.getUniqueId()) ? t.getSellerName() : t.getBuyerName())
                : (t.getSellerUUID().equals(viewer.getUniqueId()) ? t.getBuyerName() : t.getSellerName());

        List<String> lore = new ArrayList<>();
        for (String line : gui.getTransactionItemLore()) {
            lore.add(ColorUtil.colorize(line
                    .replace("%action%", action)
                    .replace("%other_player%", other)
                    .replace("%amount%", String.valueOf(t.getItemAmount()))
                    .replace("%item_name%", t.getItemName())
                    .replace("%price%", EcoUtil.format(t.getPrice(), true, cfg))
                    .replace("%time_ago%", t.getTimeAgo())));
        }
        meta.setLore(lore);
        display.setItemMeta(meta);
        return display;
    }

    private void startAutoUpdate(Player player) {
        stopAutoUpdate(player.getUniqueId());
        int ticks = plugin.getAuctionModule().getGuiManager().getTransactionsAutoUpdate();
        ScheduledTask task = Bukkit.getGlobalRegionScheduler().runAtFixedRate((Plugin) plugin, scheduledTask -> {
            if (player.isOnline() && isTransactionsGUI(player.getOpenInventory().getTopInventory())) {
                open(player, playerPage.getOrDefault(player.getUniqueId(), 1), playerSearch.get(player.getUniqueId()));
            } else {
                stopAutoUpdate(player.getUniqueId());
            }
        }, 1L, Math.max(1L, ticks / 20L), TimeUnit.SECONDS);
        autoUpdateTasks.put(player.getUniqueId(), task);
    }

    private void stopAutoUpdate(UUID uuid) {
        ScheduledTask t = autoUpdateTasks.remove(uuid);
        if (t != null) t.cancel();
    }

    private boolean isTransactionsGUI(Inventory inv) {
        if (inv == null || inv.getViewers().isEmpty()) return false;
        String title = inv.getViewers().get(0).getOpenInventory().getTitle();
        GUIManager gui = plugin.getAuctionModule().getGuiManager();
        return title != null && title.contains(gui.getTransactionsTitle(1).replaceAll("\\(Page .*\\)", "").trim());
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        stopAutoUpdate(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (!isTransactionsGUI(event.getInventory())) return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        GUIManager gui = plugin.getAuctionModule().getGuiManager();
        AuctionConfigManager cfg = plugin.getAuctionModule().getConfigManager();
        int slot = event.getRawSlot();
        int page = playerPage.getOrDefault(player.getUniqueId(), 1);

        cfg.playSound(player, "click");

        if (slot == gui.getTransactionsPrevSlot()) { open(player, page - 1, playerSearch.get(player.getUniqueId())); return; }
        if (slot == gui.getTransactionsNextSlot()) { open(player, page + 1, playerSearch.get(player.getUniqueId())); return; }
        if (slot == gui.getTransactionsRefreshSlot()) { open(player, page, playerSearch.get(player.getUniqueId())); return; }
        if (slot == gui.getTransactionsSearchSlot()) {
            plugin.getAuctionModule().getChatSignManager().requestInput(player, input -> open(player, 1, input));
        }
    }

    public void closeDatabase() {}
}
