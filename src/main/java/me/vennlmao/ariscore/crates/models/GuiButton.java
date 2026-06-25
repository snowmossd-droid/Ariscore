package me.vennlmao.ariscore.crates.models;

import org.bukkit.inventory.ItemStack;

public class GuiButton {

    private final int slot;
    private final ItemStack item;

    public GuiButton(int slot, ItemStack item) {
        this.slot = slot;
        this.item = item;
    }

    public int getSlot() { return slot; }
    public ItemStack getItem() { return item; }
}
