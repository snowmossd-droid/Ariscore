package me.vennlmao.ariscore.crates.models;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public class RewardInfo {

    private final int slot;
    private final ItemStack icon;
    private final List<ItemStack> items;

    public RewardInfo(int slot, ItemStack icon, List<ItemStack> items) {
        this.slot = slot;
        this.icon = icon;
        this.items = items;
    }

    public int getSlot() { return slot; }
    public ItemStack getIcon() { return icon; }
    public List<ItemStack> getItems() { return items; }
}
