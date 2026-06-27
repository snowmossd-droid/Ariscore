package me.vennlmao.ariscore.crates.models;

import org.bukkit.inventory.ItemStack;

public class ConfirmGuiConfig {

    private final String name;
    private final int rows;
    private final ItemStack background;
    private final int rewardSlot;
    private final GuiButton cancelButton;
    private final GuiButton confirmButton;

    public ConfirmGuiConfig(String name, int rows, ItemStack background,
                            int rewardSlot, GuiButton cancelButton, GuiButton confirmButton) {
        this.name = name;
        this.rows = rows;
        this.background = background;
        this.rewardSlot = rewardSlot;
        this.cancelButton = cancelButton;
        this.confirmButton = confirmButton;
    }

    public String getName() { return name; }
    public int getRows() { return rows; }
    public ItemStack getBackground() { return background; }
    public int getRewardSlot() { return rewardSlot; }
    public GuiButton getCancelButton() { return cancelButton; }
    public GuiButton getConfirmButton() { return confirmButton; }
}
