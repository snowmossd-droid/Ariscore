package me.vennlmao.ariscore.sell.listeners;

import me.vennlmao.ariscore.sell.SellModule;
import me.vennlmao.ariscore.sell.managers.SearchManager;
import me.vennlmao.ariscore.sell.menus.SellHistoryMenu;
import me.vennlmao.ariscore.sell.menus.WorthMenu;
import me.vennlmao.ariscore.sell.utils.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

public class SearchListener implements Listener {

    private final SellModule module;

    public SearchListener(SellModule module) {
        this.module = module;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!module.getSearchManager().isSearching(player.getUniqueId())) return;

        event.setCancelled(true);
        String message = event.getMessage().trim();
        SearchManager.SearchTarget target = module.getSearchManager().stopSearching(player.getUniqueId());

        if (message.equalsIgnoreCase("cancel")) {
            player.sendMessage(ColorUtil.colorize(module.getConfig().getString("search-cancelled", "&cSearch cancelled.")));
            return;
        }

        player.getScheduler().run((Plugin) module.getPlugin(), task -> {
            if (target == SearchManager.SearchTarget.HISTORY) {
                SellHistoryMenu menu = new SellHistoryMenu(module, player);
                menu.setFilter(message);
                player.openInventory(menu.getInventory());
            } else if (target == SearchManager.SearchTarget.WORTH) {
                WorthMenu menu = new WorthMenu(module);
                menu.setFilter(message);
                player.openInventory(menu.getInventory());
            }
        }, null);
    }
}
