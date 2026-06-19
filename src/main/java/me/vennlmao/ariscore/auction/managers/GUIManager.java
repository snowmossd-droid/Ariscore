package me.vennlmao.ariscore.auction.managers;

import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.auction.utils.AuctionItem;
import me.vennlmao.ariscore.auction.utils.ColorUtil;
import me.vennlmao.ariscore.auction.utils.EcoUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class GUIManager {

    public enum SortType { NEWEST, OLDEST, CHEAPEST, PRICIEST }

    private final ArisCore plugin;
    private FileConfiguration mainGui, myItemsGui, confirmPurchaseGui, confirmListingGui, transactionsGui, shulkerViewGui;

    public GUIManager(ArisCore plugin) {
        this.plugin = plugin;
        loadAll();
    }

    public void reload() {
        loadAll();
    }

    private void loadAll() {
        File guiFolder = new File(plugin.getDataFolder(), "auction/gui");
        if (!guiFolder.exists()) guiFolder.mkdirs();

        mainGui = load("auction/gui/main.yml", new File(guiFolder, "main.yml"));
        myItemsGui = load("auction/gui/my-items.yml", new File(guiFolder, "my-items.yml"));
        confirmPurchaseGui = load("auction/gui/confirm-purchase.yml", new File(guiFolder, "confirm-purchase.yml"));
        confirmListingGui = load("auction/gui/confirm-listing.yml", new File(guiFolder, "confirm-listing.yml"));
        transactionsGui = load("auction/gui/transactions.yml", new File(guiFolder, "transactions.yml"));
        shulkerViewGui = load("auction/gui/shulker-view.yml", new File(guiFolder, "shulker-view.yml"));
    }

    private FileConfiguration load(String resource, File target) {
        if (!target.exists()) {
            try {
                InputStream in = plugin.getResource(resource);
                if (in != null) Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) { e.printStackTrace(); }
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(target);
        InputStream defStream = plugin.getResource(resource);
        if (defStream != null) cfg.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defStream)));
        return cfg;
    }

    private ItemStack buildItem(FileConfiguration cfg, String path) {
        String matName = cfg.getString(path + ".material", "STONE");
        Material mat = Material.getMaterial(matName);
        if (mat == null) mat = Material.STONE;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        String name = cfg.getString(path + ".name");
        if (name != null) meta.setDisplayName(ColorUtil.colorize(name));
        List<String> lore = cfg.getStringList(path + ".lore");
        if (!lore.isEmpty()) {
            List<String> colored = new ArrayList<>();
            for (String l : lore) colored.add(ColorUtil.colorize(l));
            meta.setLore(colored);
        }
        item.setItemMeta(meta);
        return item;
    }

    private List<Integer> parseSlots(FileConfiguration cfg, String path) {
        List<Integer> slots = new ArrayList<>();
        List<?> raw = cfg.getList(path);
        if (raw == null) return slots;
        for (Object obj : raw) {
            String s = obj.toString().trim();
            if (s.contains("-")) {
                String[] parts = s.split("-");
                try {
                    int from = Integer.parseInt(parts[0].trim());
                    int to = Integer.parseInt(parts[1].trim());
                    for (int i = from; i <= to; i++) slots.add(i);
                } catch (NumberFormatException ignored) {}
            } else {
                try { slots.add(Integer.parseInt(s)); } catch (NumberFormatException ignored) {}
            }
        }
        return slots;
    }

    public FileConfiguration getMainGui() { return mainGui; }
    public FileConfiguration getMyItemsGui() { return myItemsGui; }
    public FileConfiguration getConfirmPurchaseGui() { return confirmPurchaseGui; }
    public FileConfiguration getConfirmListingGui() { return confirmListingGui; }
    public FileConfiguration getTransactionsGui() { return transactionsGui; }
    public FileConfiguration getShulkerViewGui() { return shulkerViewGui; }

    public String getMainTitle(int page) {
        return ColorUtil.colorize(mainGui.getString("title", "").replace("%page%", String.valueOf(page)));
    }
    public int getMainSize() { return mainGui.getInt("size"); }
    public int getMainAutoUpdate() { return mainGui.getInt("auto-update-ticks"); }
    public List<Integer> getMainAuctionSlots() { return parseSlots(mainGui, "auction-slots"); }
    public int getPrevSlot() { return mainGui.getInt("previous-page.slot"); }
    public int getNextSlot() { return mainGui.getInt("next-page.slot"); }
    public int getRefreshSlot() { return mainGui.getInt("refresh.slot"); }
    public int getSortSlot() { return mainGui.getInt("sort.slot"); }
    public int getFilterSlot() { return mainGui.getInt("filter.slot"); }
    public int getSearchSlot() { return mainGui.getInt("search.slot"); }
    public int getMyItemsSlot() { return mainGui.getInt("my-items.slot"); }
    public List<String> getFilterOptions() { return mainGui.getStringList("filter.options"); }
    public ItemStack getPrevItem() { return buildItem(mainGui, "previous-page"); }
    public ItemStack getNextItem() { return buildItem(mainGui, "next-page"); }
    public ItemStack getSearchItem() { return buildItem(mainGui, "search"); }
    public ItemStack getMyItemsItem() { return buildItem(mainGui, "my-items"); }

    public ItemStack getRefreshItem(Player player) {
        ArisCore plugin = this.plugin;
        ItemStack item = buildItem(mainGui, "refresh");
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        if (lore != null) {
            int count = plugin.getAuctionModule().getAuctionManager().getPlayerAuctionCount(player.getUniqueId());
            int max = plugin.getAuctionModule().getAuctionManager().getMaxAuctionsForPlayer(player);
            List<String> formatted = new ArrayList<>();
            for (String l : lore) formatted.add(l.replace("%sell_count%", String.valueOf(count)).replace("%sell_limit%", String.valueOf(max)));
            meta.setLore(formatted);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack getSortItem(SortType current) {
        ItemStack item = buildItem(mainGui, "sort");
        ItemMeta meta = item.getItemMeta();
        List<String> options = mainGui.getStringList("sort.options");
        List<String> keys = mainGui.getStringList("sort.keys");
        String activePrefix = ColorUtil.colorize(mainGui.getString("sort.active-prefix", ""));
        String inactivePrefix = ColorUtil.colorize(mainGui.getString("sort.inactive-prefix", ""));
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        int currentIndex = 0;
        for (int i = 0; i < keys.size(); i++) {
            if (keys.get(i).equalsIgnoreCase(current.name())) { currentIndex = i; break; }
        }
        for (int i = 0; i < options.size(); i++) {
            String opt = ColorUtil.colorize(options.get(i));
            lore.add(i == currentIndex ? activePrefix + opt : inactivePrefix + opt);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack getFilterItem(String currentFilter) {
        ItemStack item = buildItem(mainGui, "filter");
        ItemMeta meta = item.getItemMeta();
        List<String> options = mainGui.getStringList("filter.options");
        String activePrefix = ColorUtil.colorize(mainGui.getString("filter.active-prefix", ""));
        String inactivePrefix = ColorUtil.colorize(mainGui.getString("filter.inactive-prefix", ""));
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        for (String opt : options) {
            String colored = ColorUtil.colorize(opt);
            lore.add(opt.equalsIgnoreCase(currentFilter) ? activePrefix + colored : inactivePrefix + colored);
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createAuctionDisplayItem(AuctionItem auction, AuctionConfigManager cfg) {
        ItemStack item = auction.getItemStack().clone();
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();
        for (String line : mainGui.getStringList("auction-item-lore")) {
            lore.add(ColorUtil.colorize(line
                    .replace("%seller%", auction.getSellerName())
                    .replace("%price%", EcoUtil.format(auction.getPrice(), true, cfg))
                    .replace("%expires%", auction.getTimeLeft())));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public String getMyItemsTitle(int page, int total) {
        return ColorUtil.colorize(myItemsGui.getString("title", "")
                .replace("%page%", String.valueOf(page)).replace("%total%", String.valueOf(total)));
    }
    public int getMyItemsSize() { return myItemsGui.getInt("size"); }
    public List<Integer> getMyItemsAuctionSlots() { return parseSlots(myItemsGui, "auction-slots"); }
    public int getMyItemsPrevSlot() { return myItemsGui.getInt("previous-page.slot"); }
    public int getMyItemsNextSlot() { return myItemsGui.getInt("next-page.slot"); }
    public int getMyItemsInfoSlot() { return myItemsGui.getInt("info.slot"); }
    public int getMyItemsTransactionsSlot() { return myItemsGui.getInt("transactions.slot"); }
    public int getMyItemsAutoUpdate() { return myItemsGui.getInt("auto-update-ticks"); }
    public ItemStack getMyItemsPrevItem() { return buildItem(myItemsGui, "previous-page"); }
    public ItemStack getMyItemsNextItem() { return buildItem(myItemsGui, "next-page"); }
    public ItemStack getMyItemsTransactionsItem() { return buildItem(myItemsGui, "transactions"); }

    public ItemStack getMyItemsInfoItem(Player player) {
        ItemStack item = buildItem(myItemsGui, "info");
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        if (lore != null) {
            int count = plugin.getAuctionModule().getAuctionManager().getPlayerAuctionCount(player.getUniqueId());
            int max = plugin.getAuctionModule().getAuctionManager().getMaxAuctionsForPlayer(player);
            List<String> formatted = new ArrayList<>();
            for (String l : lore) formatted.add(l.replace("%sell_count%", String.valueOf(count)).replace("%sell_limit%", String.valueOf(max)));
            meta.setLore(formatted);
            item.setItemMeta(meta);
        }
        return item;
    }

    public String getConfirmPurchaseTitle() { return ColorUtil.colorize(confirmPurchaseGui.getString("title", "")); }
    public int getConfirmPurchaseSize() { return confirmPurchaseGui.getInt("size"); }
    public int getConfirmPurchaseItemSlot() { return confirmPurchaseGui.getInt("item-slot"); }
    public List<Integer> getConfirmSlots() { return confirmPurchaseGui.getIntegerList("confirm.slots"); }
    public List<Integer> getCancelSlots() { return confirmPurchaseGui.getIntegerList("cancel.slots"); }
    public ItemStack getConfirmItem() { return buildItem(confirmPurchaseGui, "confirm"); }
    public ItemStack getCancelItem() { return buildItem(confirmPurchaseGui, "cancel"); }
    public List<String> getConfirmPurchaseItemLore() { return confirmPurchaseGui.getStringList("item-lore"); }

    public String getConfirmListingTitle() { return ColorUtil.colorize(confirmListingGui.getString("title", "")); }
    public int getConfirmListingSize() { return confirmListingGui.getInt("size"); }
    public int getConfirmListingItemSlot() { return confirmListingGui.getInt("item-slot"); }
    public List<Integer> getListingConfirmSlots() { return confirmListingGui.getIntegerList("confirm.slots"); }
    public List<Integer> getListingCancelSlots() { return confirmListingGui.getIntegerList("cancel.slots"); }
    public ItemStack getListingConfirmItem() { return buildItem(confirmListingGui, "confirm"); }
    public ItemStack getListingCancelItem() { return buildItem(confirmListingGui, "cancel"); }
    public List<String> getConfirmListingItemLore() { return confirmListingGui.getStringList("item-lore"); }

    public String getTransactionsTitle(int page) {
        return ColorUtil.colorize(transactionsGui.getString("title", "").replace("%page%", String.valueOf(page)));
    }
    public int getTransactionsSize() { return transactionsGui.getInt("size"); }
    public List<Integer> getTransactionSlots() { return parseSlots(transactionsGui, "transaction-slots"); }
    public int getTransactionsPrevSlot() { return transactionsGui.getInt("previous-page.slot"); }
    public int getTransactionsNextSlot() { return transactionsGui.getInt("next-page.slot"); }
    public int getTransactionsStatsSlot() { return transactionsGui.getInt("stats.slot"); }
    public int getTransactionsRefreshSlot() { return transactionsGui.getInt("refresh.slot"); }
    public int getTransactionsSearchSlot() { return transactionsGui.getInt("search.slot"); }
    public ItemStack getTransactionsPrevItem() { return buildItem(transactionsGui, "previous-page"); }
    public ItemStack getTransactionsNextItem() { return buildItem(transactionsGui, "next-page"); }
    public ItemStack getTransactionsRefreshItem() { return buildItem(transactionsGui, "refresh"); }
    public ItemStack getTransactionsSearchItem() { return buildItem(transactionsGui, "search"); }
    public int getTransactionsAutoUpdate() { return transactionsGui.getInt("auto-update-ticks"); }
    public List<String> getTransactionItemLore() { return transactionsGui.getStringList("transaction-item.lore"); }

    public ItemStack getTransactionsStatsItem(double spent, double made, AuctionConfigManager cfg) {
        ItemStack item = buildItem(transactionsGui, "stats");
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.getLore();
        if (lore != null) {
            List<String> formatted = new ArrayList<>();
            for (String l : lore) formatted.add(l
                    .replace("%total_spent%", EcoUtil.format(spent, true, cfg))
                    .replace("%total_made%", EcoUtil.format(made, true, cfg)));
            meta.setLore(formatted);
            item.setItemMeta(meta);
        }
        return item;
    }

    public String getShulkerTitle() { return ColorUtil.colorize(shulkerViewGui.getString("title", "")); }
    public int getShulkerSize() { return shulkerViewGui.getInt("size"); }
    public int getShulkerBackSlot() { return shulkerViewGui.getInt("back-button.slot"); }
    public ItemStack getShulkerBackItem() { return buildItem(shulkerViewGui, "back-button"); }

    public ItemStack buildFiller(FileConfiguration cfg) {
        String mat = cfg.getString("filler.material", "GRAY_STAINED_GLASS_PANE");
        Material m = Material.getMaterial(mat);
        if (m == null) m = Material.GRAY_STAINED_GLASS_PANE;
        ItemStack item = new ItemStack(m);
        ItemMeta meta = item.getItemMeta();
        String name = cfg.getString("filler.name", " ");
        meta.setDisplayName(ColorUtil.colorize(name));
        item.setItemMeta(meta);
        return item;
    }

    public List<Integer> getFillerSlots(FileConfiguration cfg) {
        return cfg.getIntegerList("filler.slots");
    }

    public boolean fillerEnabled(FileConfiguration cfg) {
        return cfg.getBoolean("filler.enabled");
    }
}
