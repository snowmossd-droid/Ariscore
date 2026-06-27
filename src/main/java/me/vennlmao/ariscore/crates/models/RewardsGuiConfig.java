package me.vennlmao.ariscore.crates.models;

import org.bukkit.inventory.ItemStack;

public class RewardsGuiConfig {

    private final String name;
    private final int rows;
    private final ItemStack background;
    private final GuiButton cancelButton;

    public RewardsGuiConfig(String name, int rows, ItemStack background, GuiButton cancelButton) {
        this.name = name;
        this.rows = rows;
        this.background = background;
        this.cancelButton = cancelButton;
    }

    public String getName() { return name; }
    public int getRows() { return rows; }
    public ItemStack getBackground() { return background; }
    public GuiButton getCancelButton() { return cancelButton; }
}
