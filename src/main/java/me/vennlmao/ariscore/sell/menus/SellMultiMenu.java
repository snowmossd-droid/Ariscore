package me.vennlmao.ariscore.sell.menus;

import me.vennlmao.ariscore.sell.SellModule;
import me.vennlmao.ariscore.sell.utils.ColorUtil;
import me.vennlmao.ariscore.sell.utils.FormatUtils;
import me.vennlmao.ariscore.sell.utils.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SellMultiMenu implements InventoryHolder {

    private static final int[] PROGRESS_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final String[][] CATEGORY_KEYS = {
            {"crops", "crops"},
            {"ores", "ores"},
            {"mob-drops", "mobdrops"},
            {"natural-items", "naturalitems"},
            {"armor-tools", "armortools"},
            {"fish", "fish"},
            {"enchanted-books", "enchantedbooks"},
            {"potions", "potions"},
            {"blocks", "blocks"}
    };

    private final SellModule module;
    private final Player player;
    private final Inventory inventory;
    private final FileConfiguration config;
    private String selectedCategory = "crops";
    private boolean loaded = false;
    private ItemStack fillerItem;
    private ItemStack loadingItem;
    private ItemStack closeButton;
    private final Map<Integer, String> slotToCategory = new HashMap<>();
    private final Map<Integer, String> worthNavSlots = new HashMap<>();

    public SellMultiMenu(SellModule module, Player player) {
        this.module = module;
        this.player = player;
        this.config = module.getGuiManager().getGuiConfig("sellmulti");
        int rows = config.getInt("sellmulti.rows", 6);
        this.inventory = Bukkit.createInventory(this, rows * 9, ColorUtil.colorize(config.getString("sellmulti.title")));
        loadSlots();
        update();
    }

    private void loadSlots() {
        for (String[] entry : CATEGORY_KEYS) {
            slotToCategory.put(config.getInt("sellmulti.buttons.categories." + entry[0] + ".slot"), entry[0]);
        }
        worthNavSlots.put(config.getInt("sellmulti.category-item-worth-list.navigation.previous-page-slot"), "prev");
        worthNavSlots.put(config.getInt("sellmulti.category-item-worth-list.navigation.next-page-slot"), "next");
        worthNavSlots.put(config.getInt("sellmulti.category-item-worth-list.navigation.close-button.slot"), "close");
    }

    public void update() {
        if (!loaded) {
            inventory.clear();
            if (loadingItem == null) loadingItem = createLoadingItem();
            inventory.setItem(22, loadingItem);
            Bukkit.getAsyncScheduler().runNow((Plugin) module.getPlugin(), task -> {
                module.getDataManager().preloadData(player.getUniqueId());
                loaded = true;
                Bukkit.getGlobalRegionScheduler().run((Plugin) module.getPlugin(), t -> update());
            });
            return;
        }

        if (fillerItem == null && config.getBoolean("sellmulti.filler.enabled")) {
            fillerItem = createFiller();
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, fillerItem);
        }

        for (String[] entry : CATEGORY_KEYS) {
            setupCategory(entry[0], entry[1]);
        }
        setupProgressItems();

        if (closeButton == null) closeButton = createCloseButton();
        inventory.setItem(45, closeButton);
    }

    private void setupCategory(String configKey, String dbKey) {
        String path = "sellmulti.buttons.categories." + configKey;
        int slot = config.getInt(path + ".slot");
        Material icon = Material.valueOf(config.getString(path + ".icon"));

        PlayerStats stats = module.getDataManager().getStats(player.getUniqueId(), dbKey);
        int nextLevel = Math.min(20, stats.level() + 1);
        double amountNeeded = config.getDouble("multipliers.levels." + nextLevel + ".amountNeeded", -1.0);
        double currentMultiplier = stats.level() == 0
                ? config.getDouble("multipliers.base-multiplier", 1.0)
                : config.getDouble("multipliers.levels." + stats.level() + ".multi", 1.0);
        double targetMultiplier = config.getDouble("multipliers.levels." + nextLevel + ".multi", currentMultiplier);
        double progressPercent = amountNeeded <= 0.0 ? 100.0 : Math.min(100.0, stats.progress() / amountNeeded * 100.0);
        String progressBar = createProgressBar(progressPercent);

        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ColorUtil.colorize(config.getString(path + ".name")));

        List<String> rawLore = config.getStringList(path + ".lore");
        List<String> coloredLore = new ArrayList<>(rawLore.size());
        String targetMultStr = String.format("%.1f", targetMultiplier);
        String progressPercentStr = String.format("%.1f", progressPercent);
        for (String line : rawLore) {
            coloredLore.add(ColorUtil.colorize(line
                    .replace("{multiplier}", targetMultStr)
                    .replace("{progress}", progressBar)
                    .replace("%progress%", progressPercentStr)));
        }
        meta.setLore(coloredLore);
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }

    private void setupProgressItems() {
        String dbKey = toDbKey(selectedCategory);
        PlayerStats stats = module.getDataManager().getStats(player.getUniqueId(), dbKey);

        String path = "sellmulti.categories." + selectedCategory.replace("-", "_") + ".icon";
        Material icon = Material.valueOf(config.getString(path + ".material"));
        ItemStack header = new ItemStack(icon);
        ItemMeta headerMeta = header.getItemMeta();
        headerMeta.setDisplayName(ColorUtil.colorize(config.getString(path + ".displayname")));
        List<String> rawHeaderLore = config.getStringList(path + ".lore");
        List<String> coloredHeaderLore = new ArrayList<>(rawHeaderLore.size());
        for (String line : rawHeaderLore) coloredHeaderLore.add(ColorUtil.colorize(line));
        headerMeta.setLore(coloredHeaderLore);
        header.setItemMeta(headerMeta);
        inventory.setItem(4, header);

        for (int i = 0; i < 20; i++) {
            int level = i + 1;
            double levelCost = config.getDouble("multipliers.levels." + level + ".amountNeeded");
            double levelMulti = config.getDouble("multipliers.levels." + level + ".multi");
            String status = "incomplete";
            if (stats.level() >= level) status = "complete";
            else if (stats.level() == level - 1) status = "working";
            inventory.setItem(PROGRESS_SLOTS[i], createProgressItem(status, levelMulti, stats.progress(), levelCost));
        }
    }

    private ItemStack createProgressItem(String status, double multiplier, double have, double needed) {
        String path = "sellmulti." + status;
        Material material = Material.valueOf(config.getString(path + ".material"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ColorUtil.colorize(config.getString(path + ".displayname")));

        double progress = needed <= 0.0 ? 0.0 : Math.min(100.0, have / needed * 100.0);
        if (status.equals("complete")) progress = 100.0;

        List<String> rawLore = config.getStringList(path + ".lore");
        List<String> coloredLore = new ArrayList<>(rawLore.size());
        String progressBar = createProgressBar(progress);
        String multStr = String.format("%.1f", multiplier);
        String progressStr = String.format("%.1f", progress);
        String haveStr = FormatUtils.formatPrice(have);
        String neededStr = FormatUtils.formatPrice(needed);
        for (String line : rawLore) {
            coloredLore.add(ColorUtil.colorize(line
                    .replace("%loading-bar%", progressBar)
                    .replace("%multiplier%", multStr)
                    .replace("%progress%", progressStr)
                    .replace("%amount-have%", haveStr)
                    .replace("%amount-needed%", neededStr)));
        }
        meta.setLore(coloredLore);
        item.setItemMeta(meta);
        return item;
    }

    private String createProgressBar(double percentage) {
        int totalBars = 10;
        int completedBars = (int) (percentage / (100.0 / totalBars));
        StringBuilder bar = new StringBuilder(30);
        bar.append("&a");
        for (int i = 0; i < completedBars; i++) bar.append("■");
        bar.append("&7");
        for (int i = 0; i < totalBars - completedBars; i++) bar.append("■");
        return bar.toString();
    }

    private ItemStack createFiller() {
        Material material = Material.valueOf(config.getString("sellmulti.filler.material", "GRAY_STAINED_GLASS_PANE"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ColorUtil.colorize(config.getString("sellmulti.filler.displayname", " ")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createLoadingItem() {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ColorUtil.colorize("&eLoading..."));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCloseButton() {
        Material material = Material.valueOf(config.getString("sellmulti.back-button.material"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ColorUtil.colorize(config.getString("sellmulti.back-button.displayname")));
        List<String> rawLore = config.getStringList("sellmulti.back-button.lore");
        List<String> coloredLore = new ArrayList<>(rawLore.size());
        for (String line : rawLore) coloredLore.add(ColorUtil.colorize(line));
        meta.setLore(coloredLore);
        item.setItemMeta(meta);
        return item;
    }

    public static String toDbKey(String configKey) {
        switch (configKey) {
            case "armor-tools": return "armortools";
            case "mob-drops": return "mobdrops";
            case "natural-items": return "naturalitems";
            case "enchanted-books": return "enchantedbooks";
            default: return configKey.replace("-", "_");
        }
    }

    public void setSelectedCategory(String category) {
        this.selectedCategory = category;
        update();
    }

    public String getSelectedCategory() {
        return selectedCategory;
    }

    public Map<Integer, String> getSlotToCategory() {
        return slotToCategory;
    }

    public Map<Integer, String> getWorthNavSlots() {
        return worthNavSlots;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
