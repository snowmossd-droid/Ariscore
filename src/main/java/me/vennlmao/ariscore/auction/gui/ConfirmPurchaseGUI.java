package me.vennlmao.ariscore.auction.gui;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.auction.managers.AuctionConfigManager;
import me.vennlmao.ariscore.auction.managers.GUIManager;
import me.vennlmao.ariscore.auction.managers.LangManager;
import me.vennlmao.ariscore.auction.utils.AuctionItem;
import me.vennlmao.ariscore.auction.utils.ColorUtil;
import me.vennlmao.ariscore.auction.utils.EcoUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ConfirmPurchaseGUI implements Listener {

    private final ArisCore plugin;
    private final Map<UUID, AuctionItem> pendingPurchase = new HashMap<>();
    private final Map<UUID, Object[]> returnContext = new HashMap<>();

    public ConfirmPurchaseGUI(ArisCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, AuctionItem auction, AuctionGUI auctionGUI, int returnPage) {
        GUIManager gui = plugin.getAuctionModule().getGuiManager();
        AuctionConfigManager cfg = plugin.getAuctionModule().getConfigManager();

        pendingPurchase.put(player.getUniqueId(), auction);
        returnContext.put(player.getUniqueId(), new Object[]{auctionGUI, returnPage});

        Inventory inv = Bukkit.createInventory(null, gui.getConfirmPurchaseSize(), gui.getConfirmPurchaseTitle());

        ItemStack displayItem = auction.getItemStack().clone();
        ItemMeta meta = displayItem.getItemMeta();
        List<String> lore = new ArrayList<>();
        for (String line : gui.getConfirmPurchaseItemLore()) {
            lore.add(ColorUtil.colorize(line
                    .replace("%seller%", auction.getSellerName())
                    .replace("%price%", EcoUtil.format(auction.getPrice(), true, cfg))
                    .replace("%expires%", auction.getTimeLeft())));
        }
        meta.setLore(lore);
        displayItem.setItemMeta(meta);
        inv.setItem(gui.getConfirmPurchaseItemSlot(), displayItem);

        ItemStack confirmItem = gui.getConfirmItem();
        for (int slot : gui.getConfirmSlots()) inv.setItem(slot, confirmItem);

        ItemStack cancelItem = gui.getCancelItem();
        for (int slot : gui.getCancelSlots()) inv.setItem(slot, cancelItem);

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (!isConfirmPurchaseGUI(event.getInventory())) return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        GUIManager gui = plugin.getAuctionModule().getGuiManager();
        AuctionConfigManager cfg = plugin.getAuctionModule().getConfigManager();
        LangManager lang = plugin.getAuctionModule().getLangManager();
        int slot = event.getRawSlot();

        cfg.playSound(player, "click");

        if (gui.getConfirmSlots().contains(slot)) {
            AuctionItem auction = pendingPurchase.remove(player.getUniqueId());
            returnContext.remove(player.getUniqueId());
            if (auction == null) { player.closeInventory(); return; }
            boolean success = plugin.getAuctionModule().getAuctionManager().purchaseAuction(player, auction.getAuctionId());
            if (success) cfg.playSound(player, "item-bought");
            plugin.getAuctionModule().getAuctionGUI().open(player);
            return;
        }

        if (gui.getCancelSlots().contains(slot)) {
            pendingPurchase.remove(player.getUniqueId());
            Object[] ctx = returnContext.remove(player.getUniqueId());
            int page = ctx != null ? (int) ctx[1] : 1;
            plugin.getAuctionModule().getAuctionGUI().open(player, page, null, null);
        }
    }

    private boolean isConfirmPurchaseGUI(Inventory inv) {
        if (inv == null || inv.getViewers().isEmpty()) return false;
        String title = inv.getViewers().get(0).getOpenInventory().getTitle();
        return title != null && title.equals(plugin.getAuctionModule().getGuiManager().getConfirmPurchaseTitle());
    }
}
