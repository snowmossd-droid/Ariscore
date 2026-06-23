package me.vennlmao.ariscore.order.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class OrderHolder implements InventoryHolder {

    private final String guiId;
    private Inventory inventory;

    public OrderHolder(String guiId) {
        this.guiId = guiId;
    }

    public String getGuiId() {
        return guiId;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
