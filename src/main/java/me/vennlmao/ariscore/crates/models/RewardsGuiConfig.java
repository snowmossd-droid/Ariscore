package me.vennlmao.ariscore.crates.models;

import org.bukkit.inventory.ItemStack;

public class RewardsGuiConfig {

    private final String name;
    private final int size;
    private final ItemStack background;
    private final GuiButton cancelButton;

    public RewardsGuiConfig(String name, int size, ItemStack background, GuiButton cancelButton) {
        this.name = name;
        this.size = size;
        this.background = background;
        this.cancelButton = cancelButton;
    }

    public String getName() { return name; }
    public int getSize() { return size; }
    public ItemStack getBackground() { return background; }
    public GuiButton getCancelButton() { return cancelButton; }
}
