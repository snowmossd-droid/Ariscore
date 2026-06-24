package me.vennlmao.ariscore.shop.managers;

import org.bukkit.Material;

import java.util.List;

public class ShopItem {
    private final String key;
    private final int slot;
    private final Material material;
    private final String name;
    private final List<String> lore;
    private final int stack;
    private final double price;
    private final boolean isShard;
    private final List<String> commands;
    private final String category;

    public ShopItem(String key, int slot, Material material, String name,
                    List<String> lore, int stack, double price,
                    boolean isShard, List<String> commands, String category) {
        this.key = key;
        this.slot = slot;
        this.material = material;
        this.name = name;
        this.lore = lore;
        this.stack = stack;
        this.price = price;
        this.isShard = isShard;
        this.commands = commands;
        this.category = category;
    }

    public String getKey() { return key; }
    public int getSlot() { return slot; }
    public Material getMaterial() { return material; }
    public String getName() { return name; }
    public List<String> getLore() { return lore; }
    public int getStack() { return stack; }
    public double getPrice() { return price; }
    public boolean isShard() { return isShard; }
    public List<String> getCommands() { return commands; }
    public String getCategory() { return category; }
}
