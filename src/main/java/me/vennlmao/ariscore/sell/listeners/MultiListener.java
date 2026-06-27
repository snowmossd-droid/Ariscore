package me.vennlmao.ariscore.sell.listeners;

import me.vennlmao.ariscore.sell.SellModule;
import me.vennlmao.ariscore.sell.menus.CategoryWorthMenu;
import me.vennlmao.ariscore.sell.menus.SellMultiMenu;
import me.vennlmao.ariscore.sell.utils.SoundUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class MultiListener implements Listener {

    private final SellModule module;

    public MultiListener(SellModule module) {
        this.module = module;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof SellMultiMenu menu) {
            handleMultiMenu(event, menu);
        } else if (event.getInventory().getHolder() instanceof CategoryWorthMenu menu) {
            handleWorthMenu(event, menu);
        }
    }

    private void handleMultiMenu(InventoryClickEvent event, SellMultiMenu menu) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (menu.getSlotToCategory().containsKey(slot)) {
            menu.setSelectedCategory(menu.getSlotToCategory().get(slot));
            SoundUtil.play(player, "menu-click-sound");
            return;
        }

        if (slot == 4) {
            String category = SellMultiMenu.toDbKey(menu.getSelectedCategory());
            player.openInventory(new CategoryWorthMenu(module, player, category).getInventory());
            SoundUtil.play(player, "menu-click-sound");
            return;
        }

        if (slot == 45) {
            player.closeInventory();
            SoundUtil.play(player, "menu-click-sound");
        }
    }

    private void handleWorthMenu(InventoryClickEvent event, CategoryWorthMenu menu) {
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        String action = menu.getWorthNavSlot(slot);
        if (action == null) return;

        switch (action) {
            case "prev": menu.prevPage(); SoundUtil.play(player, "menu-click-sound"); break;
            case "next": menu.nextPage(); SoundUtil.play(player, "menu-click-sound"); break;
            case "close":
                player.openInventory(new SellMultiMenu(module, player).getInventory());
                SoundUtil.play(player, "menu-click-sound");
                break;
            default: break;
        }
    }
}
