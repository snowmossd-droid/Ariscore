package me.vennlmao.ariscore.sell.listeners;

import me.vennlmao.ariscore.sell.SellModule;
import me.vennlmao.ariscore.sell.menus.SellHistoryMenu;
import me.vennlmao.ariscore.sell.utils.SoundUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class HistoryListener implements Listener {

    private final SellModule module;

    public HistoryListener(SellModule module) {
        this.module = module;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SellHistoryMenu menu)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        FileConfiguration config = module.getGuiManager().getGuiConfig("sellhistory");
        int slot = event.getRawSlot();
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        if (slot == config.getInt("sellhistory.next-page-slot")) {
            menu.nextPage();
            SoundUtil.play(player, "menu-click-sound");
        } else if (slot == config.getInt("sellhistory.previous-page-slot")) {
            menu.prevPage();
            SoundUtil.play(player, "menu-click-sound");
        } else if (slot == config.getInt("sellhistory.sort-button.slot")) {
            menu.cycleSort();
            SoundUtil.play(player, "menu-click-sound");
        } else if (slot == config.getInt("sellhistory.back-button.slot")) {
            player.closeInventory();
            SoundUtil.play(player, "menu-click-sound");
        } else if (slot == config.getInt("sellhistory.search-button.slot")) {
            if (event.isLeftClick()) {
                module.getChatSignManager().requestInput(player, input -> {
                    SellHistoryMenu newMenu = new SellHistoryMenu(module, player);
                    newMenu.setFilter(input);
                    player.openInventory(newMenu.getInventory());
                });
                SoundUtil.play(player, "menu-click-sound");
            } else if (event.isRightClick()) {
                menu.clearFilter();
                SoundUtil.play(player, "menu-click-sound");
            }
        }
    }
}
