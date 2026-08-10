package me.vennlmao.ariscore.spawners.gui;

import me.vennlmao.ariscore.spawners.managers.SpawnerData;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class SpawnerGuiHolder implements InventoryHolder {

    public enum Screen { INFO, STORAGE, CONFIRM }

    private final Screen screen;
    private final SpawnerData data;
    private final Screen returnTo;
    private int page;
    private Inventory inventory;

    public SpawnerGuiHolder(Screen screen, SpawnerData data, Screen returnTo, int page) {
        this.screen = screen;
        this.data = data;
        this.returnTo = returnTo;
        this.page = page;
    }

    public Screen getScreen() { return screen; }
    public SpawnerData getData() { return data; }
    public Screen getReturnTo() { return returnTo; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public @NotNull Inventory getInventory() { return inventory; }
}
