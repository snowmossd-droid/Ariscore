package me.vennlmao.ariscore.sell.menus;

import me.vennlmao.ariscore.sell.SellModule;
import me.vennlmao.ariscore.sell.utils.ColorUtil;
import me.vennlmao.ariscore.sell.utils.FormatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryWorthMenu implements InventoryHolder {

    private final SellModule module;
    private final Player player;
    private final Inventory inventory;
    private final FileConfiguration config;
    private final String category;
    private int page = 0;
    private List<Material> cachedItems;
    private ItemStack fillerItem;
    private final Map<String, ItemStack> controlItemCache = new HashMap<>();
    private final Map<Integer, String> worthNavSlots = new HashMap<>();

    public CategoryWorthMenu(SellModule module, Player player, String category) {
        this.module = module;
        this.player = player;
        this.category = category;
        this.config = module.getGuiManager().getGuiConfig("sellmulti");

        String title = config.getString("sellmulti.category-item-worth-list.titles." + category.replace("_", "-"));
        if (title == null) title = config.getString("sellmulti.category-item-worth-list.titles.crops", "&8Item Worth");

        this.inventory = Bukkit.createInventory(this, 54, ColorUtil.colorize(title));
        loadNavSlots();
        update();
    }

    private void loadNavSlots() {
        worthNavSlots.put(config.getInt("sellmulti.category-item-worth-list.navigation.previous-page-slot"), "prev");
        worthNavSlots.put(config.getInt("sellmulti.category-item-worth-list.navigation.next-page-slot"), "next");
        worthNavSlots.put(config.getInt("sellmulti.category-item-worth-list.navigation.close-button.slot"), "close");
    }

    public String getWorthNavSlot(int slot) {
        return worthNavSlots.get(slot);
    }

    public void update() {
        if (fillerItem == null && config.getBoolean("sellmulti.filler.enabled")) {
            fillerItem = createFiller();
        }
        for (int i = 0; i < 45; i++) {
            inventory.setItem(i, fillerItem);
        }

        if (cachedItems == null) {
            cachedItems = new ArrayList<>();
            for (Map.Entry<Material, Double> entry : module.getPriceManager().getPrices().entrySet()) {
                if (category.equalsIgnoreCase(module.getPriceManager().getCategory(entry.getKey()))) {
                    cachedItems.add(entry.getKey());
                }
            }
        }

        double multiplier = module.getDataManager().getMultiplier(player.getUniqueId(), category);
        int start = page * 45;
        int end = Math.min(start + 45, cachedItems.size());
        for (int j = start; j < end; j++) {
            Material material = cachedItems.get(j);
            double basePrice = module.getPriceManager().getPrice(material);
            inventory.setItem(j - start, createItem(material, basePrice, multiplier));
        }

        setupControls(cachedItems.size());
    }

    private ItemStack createItem(Material material, double basePrice, double multiplier) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize("&a" + FormatUtils.formatItemName(material.name())));
            List<String> lore = new ArrayList<>(3);
            lore.add(ColorUtil.colorize("&7Base price: &f" + FormatUtils.formatPrice(basePrice)));
            lore.add(ColorUtil.colorize("&7Multiplier: &a" + String.format("%.1f", multiplier)));
            lore.add(ColorUtil.colorize("&7Total price: &6" + FormatUtils.formatPrice(basePrice * multiplier)));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void setupControls(int totalItems) {
        int prevSlot = config.getInt("sellmulti.category-item-worth-list.navigation.previous-page-slot");
        int nextSlot = config.getInt("sellmulti.category-item-worth-list.navigation.next-page-slot");
        int closeSlot = config.getInt("sellmulti.category-item-worth-list.navigation.close-button.slot");

        inventory.setItem(prevSlot, fillerItem);
        inventory.setItem(nextSlot, fillerItem);
        inventory.setItem(closeSlot, fillerItem);

        if (page > 0) inventory.setItem(prevSlot, getControlItem("previous-page"));
        if (totalItems > (page + 1) * 45) inventory.setItem(nextSlot, getControlItem("next-page"));
        inventory.setItem(closeSlot, getControlItem("close-button"));
    }

    private ItemStack getControlItem(String key) {
        return controlItemCache.computeIfAbsent(key, this::createControlItem);
    }

    private ItemStack createControlItem(String key) {
        String path = "sellmulti.category-item-worth-list.navigation." + key;
        Material material = Material.valueOf(config.getString(path + ".material"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ColorUtil.colorize(config.getString(path + ".displayname")));
        List<String> rawLore = config.getStringList(path + ".lore");
        List<String> coloredLore = new ArrayList<>(rawLore.size());
        for (String line : rawLore) coloredLore.add(ColorUtil.colorize(line));
        meta.setLore(coloredLore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFiller() {
        Material material = Material.valueOf(config.getString("sellmulti.filler.material", "GRAY_STAINED_GLASS_PANE"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ColorUtil.colorize(config.getString("sellmulti.filler.displayname", " ")));
        item.setItemMeta(meta);
        return item;
    }

    public void nextPage() {
        if (cachedItems == null) update();
        if ((page + 1) * 45 < cachedItems.size()) {
            page++;
            update();
        }
    }

    public void prevPage() {
        if (page > 0) {
            page--;
            update();
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
