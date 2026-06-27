package me.vennlmao.ariscore.crates.models;

import org.bukkit.inventory.ItemStack;

public class ConfirmGuiConfig {

    private final String name;
    private final int size;
    private final ItemStack background;
    private final int rewardSlot;
    private final GuiButton cancelButton;
    private final GuiButton confirmButton;

    public ConfirmGuiConfig(String name, int size, ItemStack background,
                            int rewardSlot, GuiButton cancelButton, GuiButton confirmButton) {
        this.name = name;
        this.size = size;
        this.background = background;
        this.rewardSlot = rewardSlot;
        this.cancelButton = cancelButton;
        this.confirmButton = confirmButton;
    }

    public String getName() { return name; }
    public int getSize() { return size; }
    public ItemStack getBackground() { return background; }
    public int getRewardSlot() { return rewardSlot; }
    public GuiButton getCancelButton() { return cancelButton; }
    public GuiButton getConfirmButton() { return confirmButton; }
}
