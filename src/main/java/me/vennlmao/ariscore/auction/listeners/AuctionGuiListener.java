package me.vennlmao.ariscore.auction.listeners;

import me.vennlmao.ariscore.auction.AuctionModule;
import me.vennlmao.ariscore.auction.data.AuctionListing;
import me.vennlmao.ariscore.auction.data.Transaction;
import me.vennlmao.ariscore.auction.gui.GuiBuilder;
import me.vennlmao.ariscore.auction.utils.ColorUtil;
import me.vennlmao.ariscore.auction.utils.MessageUtil;
import me.vennlmao.ariscore.auction.utils.SignEditorUtil;
import me.vennlmao.ariscore.auction.utils.SoundUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.util.*;
import java.util.HashMap;

public class AuctionGuiListener implements Listener {

    private final AuctionModule module;
    private final Map<UUID, String> sortModes = new HashMap<>();
    private final Map<UUID, String> filterModes = new HashMap<>();
    private final Map<UUID, Integer> mainPages = new HashMap<>();
    private final Map<UUID, Integer> yourPages = new HashMap<>();
    private final Map<UUID, Integer> txPages = new HashMap<>();
    private final Map<UUID, Double> pendingPrice = new HashMap<>();
    private final Map<UUID, UUID> pendingBuy = new HashMap<>();
    private final Map<UUID, Long> lastClick = new HashMap<>();
    private final Map<UUID, Deque<String>> backStack = new HashMap<>();
    private final Map<UUID, Boolean> skipClose = new HashMap<>();
    private static final DecimalFormat DF = new DecimalFormat("#,###.##");

    public AuctionGuiListener(AuctionModule module) {
        this.module = module;
    }

    private String getDefaultSort() {
        List<String> opts = module.getConfig().getStringList("gui.items.sort.lore");
        return opts.isEmpty() ? "" : opts.get(opts.size() - 1).trim();
    }

    private String getDefaultFilter() {
        List<String> opts = module.getConfig().getStringList("gui.items.filter.lore");
        return opts.isEmpty() ? "" : opts.get(0).trim();
    }

    private boolean cooldown(Player p) {
        long ms = module.getConfig().getLong("click-cooldown-ms", 50);
        long now = System.currentTimeMillis();
        if (now - lastClick.getOrDefault(p.getUniqueId(), 0L) < ms) return true;
        lastClick.put(p.getUniqueId(), now);
        return false;
    }

    private void pushBack(UUID uuid, String gui) {
        backStack.computeIfAbsent(uuid, k -> new ArrayDeque<>()).push(gui);
    }

    private String popBack(UUID uuid) {
        Deque<String> stack = backStack.get(uuid);
        if (stack == null || stack.isEmpty()) return null;
        return stack.pop();
    }

    private void openSkip(Player p, Runnable open) {
        skipClose.put(p.getUniqueId(), true);
        p.getScheduler().run(module.getPlugin(), t -> open.run(), null);
    }

    public void openMain(Player p) {
        String sort = sortModes.getOrDefault(p.getUniqueId(), getDefaultSort());
        String filter = filterModes.getOrDefault(p.getUniqueId(), getDefaultFilter());
        int page = mainPages.getOrDefault(p.getUniqueId(), 0);
        List<AuctionListing> listings = module.getAuctionManager().sort(module.getAuctionManager().filter(filter), sort);
        p.openInventory(GuiBuilder.buildMain(module, p, listings, page, sort, filter));
    }

    private void openYour(Player p) {
        p.openInventory(GuiBuilder.buildYourListings(module, p, yourPages.getOrDefault(p.getUniqueId(), 0)));
    }

    private void openTx(Player p) {
        p.openInventory(GuiBuilder.buildTransactions(module, p, txPages.getOrDefault(p.getUniqueId(), 0)));
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() == null) return;

        String title = ColorUtil.strip(PlainTextComponentSerializer.plainText().serialize(e.getView().title()));
        String mainTitle = ColorUtil.strip(module.getConfig().getString("gui.title","").replace("{page}","").replace("{pages}","").trim());
        String yourTitle = ColorUtil.strip(module.getConfig().getString("your-listings-menu.title",""));
        String sellInputTitle = ColorUtil.strip(module.getConfig().getString("sell-item-input-menu.title",""));
        String sellConfirmTitle = ColorUtil.strip(module.getConfig().getString("sell-confirm-menu.title",""));
        String buyConfirmTitle = ColorUtil.strip(module.getConfig().getString("buy-confirm-menu.title",""));
        String txTitle = ColorUtil.strip(module.getConfig().getString("transactions-menu.title","").replace("{page}","").replace("{pages}","").trim());

        if (title.startsWith(mainTitle)) { e.setCancelled(true); if (!cooldown(p)) handleMain(e, p); }
        else if (title.equals(yourTitle)) { e.setCancelled(true); if (!cooldown(p)) handleYour(e, p); }
        else if (title.equals(sellInputTitle)) { handleSellInput(e, p); }
        else if (title.equals(sellConfirmTitle)) { e.setCancelled(true); if (!cooldown(p)) handleSellConfirm(e, p); }
        else if (title.equals(buyConfirmTitle)) { e.setCancelled(true); if (!cooldown(p)) handleBuyConfirm(e, p); }
        else if (title.startsWith(txTitle)) { e.setCancelled(true); if (!cooldown(p)) handleTx(e, p); }
    }

    private void handleMain(InventoryClickEvent e, Player p) {
        UUID uuid = p.getUniqueId();
        int slot = e.getRawSlot();
        String sort = sortModes.getOrDefault(uuid, getDefaultSort());
        String filter = filterModes.getOrDefault(uuid, getDefaultFilter());

        int sortSlot = module.getConfig().getInt("gui.items.sort.slot", 47);
        int filterSlot = module.getConfig().getInt("gui.items.filter.slot", 48);
        int refreshSlot = module.getConfig().getInt("gui.items.refresh.slot", 49);
        int searchSlot = module.getConfig().getInt("gui.items.search.slot", 50);
        int yourSlot = module.getConfig().getInt("gui.items.your_listings.slot", 51);
        int backSlot = module.getConfig().getInt("gui.items.back.slot", 45);
        int nextSlot = module.getConfig().getInt("gui.items.next.slot", 53);
        int listingSlots = module.getConfig().getInt("gui.listing-slots", 45);

        if (slot == sortSlot) {
            List<String> opts = module.getConfig().getStringList("gui.items.sort.lore");
            int idx = opts.indexOf(sort); if (idx < 0) idx = 0;
            sortModes.put(uuid, opts.get((idx + 1) % opts.size()).trim());
            SoundUtil.play(p, "click"); openSkip(p, () -> openMain(p)); return;
        }
        if (slot == filterSlot) {
            List<String> opts = module.getConfig().getStringList("gui.items.filter.lore");
            int idx = opts.indexOf(filter); if (idx < 0) idx = 0;
            filterModes.put(uuid, opts.get((idx + 1) % opts.size()).trim());
            mainPages.put(uuid, 0); SoundUtil.play(p, "click"); openSkip(p, () -> openMain(p)); return;
        }
        if (slot == refreshSlot) { SoundUtil.play(p, "click"); openSkip(p, () -> openMain(p)); return; }
        if (slot == searchSlot) {
            SoundUtil.play(p, "click");
            openSearchSign(p, "MAIN");
            return;
        }
        if (slot == yourSlot) {
            SoundUtil.play(p, "click");
            yourPages.put(uuid, 0);
            pushBack(uuid, "MAIN");
            openSkip(p, () -> openYour(p));
            return;
        }
        if (slot == backSlot) {
            int pg = Math.max(0, mainPages.getOrDefault(uuid, 0) - 1);
            mainPages.put(uuid, pg); SoundUtil.play(p, "page"); openSkip(p, () -> openMain(p)); return;
        }
        if (slot == nextSlot) {
            int pg = mainPages.getOrDefault(uuid, 0) + 1;
            mainPages.put(uuid, pg); SoundUtil.play(p, "page"); openSkip(p, () -> openMain(p)); return;
        }

        if (slot < listingSlots && e.getClickedInventory() == e.getView().getTopInventory()) {
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;
            List<AuctionListing> listings = module.getAuctionManager().sort(module.getAuctionManager().filter(filter), sort);
            int idx = mainPages.getOrDefault(uuid, 0) * listingSlots + slot;
            if (idx >= listings.size()) return;
            AuctionListing listing = listings.get(idx);
            SoundUtil.play(p, "click");
            if (listing.getSellerUuid().equals(uuid)) {
                ItemStack collectedItem = listing.getItem();
                module.getAuctionManager().removeListing(listing.getId());
                HashMap<Integer, ItemStack> lo1 = p.getInventory().addItem(collectedItem);
                lo1.values().forEach(i -> p.getWorld().dropItem(p.getLocation(), i));
                MessageUtil.sendChat(p, "collected", s -> s.replace("{item}", collectedItem.getType().name()));
                MessageUtil.sendActionbar(p, "collected", s -> s.replace("{item}", collectedItem.getType().name()));
                openSkip(p, () -> openMain(p));
            } else {
                pendingBuy.put(uuid, listing.getId());
                pushBack(uuid, "MAIN");
                openSkip(p, () -> p.openInventory(GuiBuilder.buildBuyConfirm(module, listing)));
            }
        }
    }

    private void handleYour(InventoryClickEvent e, Player p) {
        UUID uuid = p.getUniqueId();
        int slot = e.getRawSlot();
        int sellSlot = module.getConfig().getInt("your-listings-menu.items.sell.slot", 20);
        int txSlot = module.getConfig().getInt("your-listings-menu.items.transactions.slot", 22);
        int backSlot = module.getConfig().getInt("your-listings-menu.items.back_page.slot", 9);
        int nextSlot = module.getConfig().getInt("your-listings-menu.items.next_page.slot", 17);
        int size = module.getConfig().getInt("your-listings-menu.size", 27);
        int perPage = size - 9 - 2;

        if (slot == sellSlot) {
            SoundUtil.play(p, "click");
            openPriceSign(p, "YOUR");
            return;
        }
        if (slot == txSlot) {
            SoundUtil.play(p, "click");
            txPages.put(uuid, 0);
            pushBack(uuid, "YOUR");
            openSkip(p, () -> openTx(p));
            return;
        }
        if (slot == backSlot) {
            int pg = Math.max(0, yourPages.getOrDefault(uuid, 0) - 1);
            yourPages.put(uuid, pg); SoundUtil.play(p, "page"); openSkip(p, () -> openYour(p)); return;
        }
        if (slot == nextSlot) {
            int pg = yourPages.getOrDefault(uuid, 0) + 1;
            yourPages.put(uuid, pg); SoundUtil.play(p, "page"); openSkip(p, () -> openYour(p)); return;
        }

        if (e.getClickedInventory() != e.getView().getTopInventory()) return;
        List<AuctionListing> all = module.getAuctionManager().getSellerListings(uuid);
        int idx = yourPages.getOrDefault(uuid, 0) * perPage + slot;
        if (idx < 0 || idx >= all.size()) return;
        SoundUtil.play(p, "click");
        AuctionListing listing = all.get(idx);
        ItemStack collectedItem = listing.getItem();
        module.getAuctionManager().removeListing(listing.getId());
        HashMap<Integer, ItemStack> lo2 = p.getInventory().addItem(collectedItem);
        lo2.values().forEach(i -> p.getWorld().dropItem(p.getLocation(), i));
        MessageUtil.sendChat(p, "collected", s -> s.replace("{item}", collectedItem.getType().name()));
        MessageUtil.sendActionbar(p, "collected", s -> s.replace("{item}", collectedItem.getType().name()));
        openSkip(p, () -> openYour(p));
    }

    private void handleSellInput(InventoryClickEvent e, Player p) {
        UUID uuid = p.getUniqueId();
        int cancelSlot = module.getConfig().getInt("sell-item-input-menu.items.cancel.slot", 3);
        int confirmSlot = module.getConfig().getInt("sell-item-input-menu.items.confirm.slot", 5);
        int itemSlot = 4;
        int slot = e.getRawSlot();

        if (slot == cancelSlot) {
            e.setCancelled(true); SoundUtil.play(p, "click");
            ItemStack held = e.getView().getTopInventory().getItem(itemSlot);
            if (held != null && !held.getType().isAir()) { p.getInventory().addItem(held.clone()); e.getView().getTopInventory().setItem(itemSlot, null); }
            pendingPrice.remove(uuid);
            openSkip(p, () -> openYour(p));
            return;
        }
        if (slot == confirmSlot) {
            e.setCancelled(true); SoundUtil.play(p, "click");
            ItemStack item = e.getView().getTopInventory().getItem(itemSlot);
            if (item == null || item.getType().isAir()) { MessageUtil.sendChat(p, "no_item"); MessageUtil.sendActionbar(p, "no_item"); return; }
            Double price = pendingPrice.remove(uuid);
            if (price == null) { openSkip(p, () -> openYour(p)); return; }
            e.getView().getTopInventory().setItem(itemSlot, null);
            boolean listed = module.getAuctionManager().addListing(uuid, p.getName(), item, price);
            if (!listed) {
                p.getInventory().addItem(item);
                MessageUtil.sendChat(p, "max_listings"); MessageUtil.sendActionbar(p, "max_listings");
                SoundUtil.play(p, "error"); openSkip(p, () -> openYour(p)); return;
            }
            MessageUtil.sendChat(p, "listed", s -> s.replace("{item}", item.getType().name()).replace("{price}", module.getAuctionManager().format(price)));
            MessageUtil.sendActionbar(p, "listed", s -> s.replace("{item}", item.getType().name()).replace("{price}", module.getAuctionManager().format(price)));
            SoundUtil.play(p, "list_success");
            openSkip(p, () -> openYour(p));
            return;
        }

        if (slot >= 9 && e.getClickedInventory() == p.getInventory()) {
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;
            ItemStack existing = e.getView().getTopInventory().getItem(itemSlot);
            if (existing != null && !existing.getType().isAir()) { e.setCancelled(true); return; }
            if (module.getConfig().getBoolean("disallow-renamed-items", true) && clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
                e.setCancelled(true); MessageUtil.sendChat(p, "renamed_not_allowed"); MessageUtil.sendActionbar(p, "renamed_not_allowed"); SoundUtil.play(p, "error"); return;
            }
            e.setCancelled(true);
            ItemStack toList = clicked.clone(); toList.setAmount(1);
            e.getView().getTopInventory().setItem(itemSlot, toList);
            int newAmt = clicked.getAmount() - 1;
            if (newAmt <= 0) p.getInventory().setItem(e.getSlot(), null);
            else clicked.setAmount(newAmt);
        } else if (slot != itemSlot && e.getClickedInventory() == e.getView().getTopInventory()) {
            e.setCancelled(true);
        }
    }

    private void handleSellConfirm(InventoryClickEvent e, Player p) {
        int cancelSlot = module.getConfig().getInt("sell-confirm-menu.items.cancel.slot", 11);
        int confirmSlot = module.getConfig().getInt("sell-confirm-menu.items.confirm.slot", 15);
        if (e.getRawSlot() == cancelSlot) { SoundUtil.play(p, "click"); openSkip(p, () -> openMain(p)); }
        if (e.getRawSlot() == confirmSlot) { SoundUtil.play(p, "list_success"); openSkip(p, () -> openMain(p)); }
    }

    private void handleBuyConfirm(InventoryClickEvent e, Player p) {
        UUID uuid = p.getUniqueId();
        int cancelSlot = module.getConfig().getInt("buy-confirm-menu.items.cancel.slot", 11);
        int confirmSlot = module.getConfig().getInt("buy-confirm-menu.items.confirm.slot", 15);

        if (e.getRawSlot() == cancelSlot) {
            SoundUtil.play(p, "click");
            pendingBuy.remove(uuid);
            openSkip(p, () -> openMain(p));
            return;
        }
        if (e.getRawSlot() != confirmSlot) return;

        UUID listingId = pendingBuy.remove(uuid);
        if (listingId == null) { openSkip(p, () -> openMain(p)); return; }
        AuctionListing listing = module.getAuctionManager().getListing(listingId);
        if (listing == null || listing.isExpired()) {
            MessageUtil.sendChat(p, "listing_expired"); MessageUtil.sendActionbar(p, "listing_expired");
            SoundUtil.play(p, "error"); openSkip(p, () -> openMain(p)); return;
        }
        if (module.getEconomy().getBalance(p) < listing.getPrice()) {
            MessageUtil.sendChat(p, "no_money"); MessageUtil.sendActionbar(p, "no_money");
            SoundUtil.play(p, "error"); openSkip(p, () -> openMain(p)); return;
        }
        if (p.getInventory().firstEmpty() == -1) {
            MessageUtil.sendChat(p, "inventory_full"); MessageUtil.sendActionbar(p, "inventory_full");
            SoundUtil.play(p, "error"); return;
        }

        module.getEconomy().withdrawPlayer(p, listing.getPrice());
        module.getEconomy().depositPlayer(p.getServer().getOfflinePlayer(listing.getSellerUuid()), listing.getPrice());
        ItemStack boughtItem = listing.getItem();
        module.getAuctionManager().removeListing(listingId);
        module.getAuctionManager().addTransaction(uuid, listing.getSellerName(), listing.getItem(), listing.getPrice(), Transaction.Type.BOUGHT);
        module.getAuctionManager().addTransaction(listing.getSellerUuid(), p.getName(), listing.getItem(), listing.getPrice(), Transaction.Type.SOLD);

        HashMap<Integer, ItemStack> lo3 = p.getInventory().addItem(boughtItem);
        lo3.values().forEach(i -> p.getWorld().dropItem(p.getLocation(), i));
        String item = listing.getItem().getType().name();
        String price = module.getAuctionManager().format(listing.getPrice());
        MessageUtil.sendChat(p, "bought", s -> s.replace("{item}", item).replace("{price}", price));
        MessageUtil.sendActionbar(p, "bought", s -> s.replace("{item}", item).replace("{price}", price));
        SoundUtil.play(p, "buy_success");

        Player seller = p.getServer().getPlayer(listing.getSellerUuid());
        if (seller != null) {
            MessageUtil.sendChat(seller, "item_sold", s -> s.replace("{item}", item).replace("{price}", price).replace("{buyer}", p.getName()));
            MessageUtil.sendActionbar(seller, "item_sold", s -> s.replace("{item}", item).replace("{price}", price).replace("{buyer}", p.getName()));
            SoundUtil.play(seller, "buy_success");
        }
        openSkip(p, () -> openMain(p));
    }

    private void handleTx(InventoryClickEvent e, Player p) {
        UUID uuid = p.getUniqueId();
        int refreshSlot = module.getConfig().getInt("transactions-menu.items.refresh.slot", 49);
        int searchSlot = module.getConfig().getInt("transactions-menu.items.search.slot", 50);

        if (e.getClickedInventory() != e.getView().getTopInventory()) return;
        if (e.getRawSlot() == refreshSlot) {
            SoundUtil.play(p, "click");
            openSkip(p, () -> openTx(p));
        }
        if (e.getRawSlot() == searchSlot) {
            SoundUtil.play(p, "click");
            openSearchSign(p, "TX");
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        UUID uuid = p.getUniqueId();

        if (skipClose.remove(uuid) != null) return;

        String title = ColorUtil.strip(PlainTextComponentSerializer.plainText().serialize(e.getView().title()));
        String sellInputTitle = ColorUtil.strip(module.getConfig().getString("sell-item-input-menu.title", ""));

        if (title.equals(sellInputTitle)) {
            ItemStack item = e.getView().getTopInventory().getItem(4);
            if (item != null && !item.getType().isAir()) {
                pendingPrice.remove(uuid);
                if (p.getInventory().firstEmpty() != -1) p.getInventory().addItem(item.clone());
                else p.getWorld().dropItem(p.getLocation(), item.clone());
            }
        }

        String prev = popBack(uuid);
        if (prev == null) return;

        p.getScheduler().runDelayed(module.getPlugin(), t -> {
            switch (prev) {
                case "MAIN" -> openMain(p);
                case "YOUR" -> openYour(p);
                case "TX" -> openTx(p);
            }
        }, null, 1L);
    }

    private void openSearchSign(Player p, String context) {
        SignEditorUtil.openSign(p, "search-sign", lines -> {
            String query = lines[0].trim();
            if (query.isEmpty()) {
                switch (context) {
                    case "TX" -> openTx(p);
                    default -> openMain(p);
                }
                return;
            }
            if (context.equals("TX")) {
                openTx(p);
                return;
            }
            List<AuctionListing> results = module.getAuctionManager().search(query);
            openSkip(p, () -> p.openInventory(GuiBuilder.buildMain(module, p, results, 0, getDefaultSort(), getDefaultFilter())));
        });
    }

    private void openPriceSign(Player p, String returnContext) {
        SignEditorUtil.openSign(p, "price-sign", lines -> {
            String input = lines[0].trim();
            if (input.isEmpty()) {
                switch (returnContext) {
                    case "YOUR" -> openSkip(p, () -> openYour(p));
                    default -> openSkip(p, () -> openMain(p));
                }
                return;
            }
            try {
                double price = Double.parseDouble(input);
                double min = module.getConfig().getDouble("min-price", 1.0);
                double max = module.getConfig().getDouble("max-price", 10000000.0);
                if (price < min || price > max) {
                    MessageUtil.sendChat(p, "invalid_price", s -> s.replace("{min}", DF.format(min)).replace("{max}", DF.format(max)));
                    MessageUtil.sendActionbar(p, "invalid_price", s -> s.replace("{min}", DF.format(min)).replace("{max}", DF.format(max)));
                    SoundUtil.play(p, "error");
                    openSkip(p, () -> openYour(p));
                    return;
                }
                pendingPrice.put(p.getUniqueId(), price);
                openSkip(p, () -> p.openInventory(GuiBuilder.buildSellInput(module)));
            } catch (NumberFormatException ex) {
                MessageUtil.sendChat(p, "invalid_price_format");
                MessageUtil.sendActionbar(p, "invalid_price_format");
                SoundUtil.play(p, "error");
                openSkip(p, () -> openYour(p));
            }
        });
    }

    public void setPendingPrice(UUID uuid, double price) { pendingPrice.put(uuid, price); }
    public Map<UUID, Double> getPendingPrice() { return pendingPrice; }
    public Map<UUID, Integer> getTxPages() { return txPages; }
}
