package me.vennlmao.ariscore.shop.gui;

import me.vennlmao.ariscore.shop.managers.ShopItem;

public class GuiSession {

    public enum Screen { MAIN, CATEGORY, PURCHASE }

    private Screen screen;
    private String category;
    private ShopItem selectedItem;
    private int amount;

    public GuiSession() {
        this.screen = Screen.MAIN;
        this.amount = 1;
    }

    public Screen getScreen() { return screen; }
    public void setScreen(Screen screen) { this.screen = screen; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public ShopItem getSelectedItem() { return selectedItem; }
    public void setSelectedItem(ShopItem item) { this.selectedItem = item; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }
}
