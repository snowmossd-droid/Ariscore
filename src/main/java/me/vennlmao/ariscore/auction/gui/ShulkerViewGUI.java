package me.vennlmao.ariscore.auction.gui;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.auction.managers.GUIManager;
import me.vennlmao.ariscore.auction.utils.AuctionItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShulkerViewGUI implements Listener {

    private final ArisCore plugin;
    private final Map<UUID, AuctionItem> viewingAuction = new HashMap<>();

    public ShulkerViewGUI(ArisCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, AuctionItem auction) {
        ItemStack shulker = auction.getItemStack();
        if (!isShulker(shulker)) return;

        GUIManager gui = plugin.getAuctionModule().getGuiManager();
        Inventory inv = Bukkit.createInventory(null, gui.getShulkerSize(), gui.getShulkerTitle());

        BlockStateMeta bsm = (BlockStateMeta) shulker.getItemMeta();
        ShulkerBox box = (ShulkerBox) bsm.getBlockState();
        ItemStack[] contents = box.getInventory().getContents();
        for (int i = 0; i < Math.min(contents.length, 27); i++) {
            if (contents[i] != null) inv.setItem(i, contents[i]);
        }

        inv.setItem(gui.getShulkerBackSlot(), gui.getShulkerBackItem());
        viewingAuction.put(player.getUniqueId(), auction);
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (!isShulkerViewGUI(event.getInventory())) return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        GUIManager gui = plugin.getAuctionModule().getGuiManager();
        int slot = event.getRawSlot();

        if (slot == gui.getShulkerBackSlot()) {
            viewingAuction.remove(player.getUniqueId());
            plugin.getAuctionModule().getAuctionGUI().open(player);
        }
    }

    private boolean isShulkerViewGUI(Inventory inv) {
        if (inv == null || inv.getViewers().isEmpty()) return false;
        String title = inv.getViewers().get(0).getOpenInventory().getTitle();
        return title != null && title.equals(plugin.getAuctionModule().getGuiManager().getShulkerTitle());
    }

    public static boolean isShulker(ItemStack item) {
        if (item == null) return false;
        Material m = item.getType();
        return m == Material.SHULKER_BOX || m.name().endsWith("_SHULKER_BOX");
    }
}
