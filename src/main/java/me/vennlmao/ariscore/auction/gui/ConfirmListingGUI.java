package me.vennlmao.ariscore.auction.gui;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.auction.managers.AuctionConfigManager;
import me.vennlmao.ariscore.auction.managers.GUIManager;
import me.vennlmao.ariscore.auction.managers.LangManager;
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

public class ConfirmListingGUI implements Listener {

    private final ArisCore plugin;
    private final Map<UUID, ItemStack> pendingItem = new HashMap<>();
    private final Map<UUID, Double> pendingPrice = new HashMap<>();

    public ConfirmListingGUI(ArisCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, ItemStack item, double price) {
        GUIManager gui = plugin.getAuctionModule().getGuiManager();
        AuctionConfigManager cfg = plugin.getAuctionModule().getConfigManager();

        pendingItem.put(player.getUniqueId(), item);
        pendingPrice.put(player.getUniqueId(), price);

        Inventory inv = Bukkit.createInventory(null, gui.getConfirmListingSize(), gui.getConfirmListingTitle());

        ItemStack displayItem = item.clone();
        ItemMeta meta = displayItem.getItemMeta();
        List<String> lore = new ArrayList<>();
        for (String line : gui.getConfirmListingItemLore()) {
            lore.add(ColorUtil.colorize(line
                    .replace("%price%", EcoUtil.format(price, true, cfg))
                    .replace("%amount%", String.valueOf(item.getAmount()))));
        }
        meta.setLore(lore);
        displayItem.setItemMeta(meta);
        inv.setItem(gui.getConfirmListingItemSlot(), displayItem);

        ItemStack confirmItem = gui.getListingConfirmItem();
        for (int slot : gui.getListingConfirmSlots()) inv.setItem(slot, confirmItem);

        ItemStack cancelItem = gui.getListingCancelItem();
        for (int slot : gui.getListingCancelSlots()) inv.setItem(slot, cancelItem);

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (!isConfirmListingGUI(event.getInventory())) return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        GUIManager gui = plugin.getAuctionModule().getGuiManager();
        AuctionConfigManager cfg = plugin.getAuctionModule().getConfigManager();
        LangManager lang = plugin.getAuctionModule().getLangManager();
        int slot = event.getRawSlot();

        cfg.playSound(player, "click");

        if (gui.getListingConfirmSlots().contains(slot)) {
            ItemStack item = pendingItem.remove(player.getUniqueId());
            Double price = pendingPrice.remove(player.getUniqueId());
            if (item == null || price == null) { player.closeInventory(); return; }

            String error = plugin.getAuctionModule().getAuctionManager().createAuction(player, item, price);
            if (error != null) {
                player.sendMessage(error);
                player.closeInventory();
                return;
            }

            player.getInventory().remove(item);
            player.sendMessage(lang.getAuctionCreated(EcoUtil.format(price, true, cfg)));
            cfg.playSound(player, "item-sold");
            plugin.getAuctionModule().getAuctionGUI().open(player);
            return;
        }

        if (gui.getListingCancelSlots().contains(slot)) {
            pendingItem.remove(player.getUniqueId());
            pendingPrice.remove(player.getUniqueId());
            plugin.getAuctionModule().getAuctionGUI().open(player);
        }
    }

    private boolean isConfirmListingGUI(Inventory inv) {
        if (inv == null || inv.getViewers().isEmpty()) return false;
        String title = inv.getViewers().get(0).getOpenInventory().getTitle();
        return title != null && title.equals(plugin.getAuctionModule().getGuiManager().getConfirmListingTitle());
    }
}
