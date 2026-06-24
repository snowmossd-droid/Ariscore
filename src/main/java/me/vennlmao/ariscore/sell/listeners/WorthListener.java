package me.vennlmao.ariscore.sell.listeners;

import me.vennlmao.ariscore.sell.SellModule;
import me.vennlmao.ariscore.sell.menus.WorthMenu;
import me.vennlmao.ariscore.sell.utils.SoundUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.Map;

public class WorthListener implements Listener {

    private final SellModule module;
    private final Map<Integer, String> navigationSlots = new HashMap<>();

    public WorthListener(SellModule module) {
        this.module = module;
        loadSlots();
    }

    private void loadSlots() {
        FileConfiguration config = module.getGuiManager().getGuiConfig("worth");
        if (config == null) return;
        navigationSlots.clear();
        navigationSlots.put(config.getInt("worth.navigation.next-page-slot"), "next");
        navigationSlots.put(config.getInt("worth.navigation.previous-page-slot"), "prev");
        navigationSlots.put(config.getInt("worth.navigation.sort-button.slot"), "sort");
        navigationSlots.put(config.getInt("worth.navigation.close-button.slot"), "close");
        navigationSlots.put(config.getInt("worth.navigation.search-button.slot"), "search");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof WorthMenu menu)) return;
        event.setCancelled(true);
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        String action = navigationSlots.get(slot);
        if (action == null) return;

        boolean playSound = true;
        switch (action) {
            case "next": menu.nextPage(); break;
            case "prev": menu.prevPage(); break;
            case "sort": menu.cycleSort(); break;
            case "close": player.closeInventory(); break;
            case "search":
                if (event.isLeftClick()) {
                    module.getChatSignManager().requestInput(player, input -> {
                        WorthMenu newMenu = new WorthMenu(module);
                        newMenu.setFilter(input);
                        player.openInventory(newMenu.getInventory());
                    });
                } else if (event.isRightClick()) {
                    menu.clearFilter();
                } else {
                    playSound = false;
                }
                break;
            default: playSound = false; break;
        }

        if (playSound) SoundUtil.play(player, "menu-click-sound");
    }
}
