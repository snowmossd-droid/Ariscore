package me.vennlmao.ariscore.sell.menus;

import me.vennlmao.ariscore.sell.SellModule;
import me.vennlmao.ariscore.sell.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class SellMenu implements InventoryHolder {

    private final Inventory inventory;

    public SellMenu(SellModule module) {
        String title = ColorUtil.colorize(module.getConfig().getString("gui-title", "&8Sell Menu"));
        this.inventory = Bukkit.createInventory(this, 54, title);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
