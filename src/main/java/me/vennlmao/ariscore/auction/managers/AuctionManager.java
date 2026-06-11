package me.vennlmao.ariscore.auction.managers;

import me.vennlmao.ariscore.auction.AuctionModule;
import me.vennlmao.ariscore.auction.managers.AuctionDatabaseManager;
import me.vennlmao.ariscore.auction.data.AuctionListing;
import me.vennlmao.ariscore.auction.data.Transaction;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public class AuctionManager {

    private final AuctionModule module;
    private final List<AuctionListing> listings = new ArrayList<>();
    private final Map<UUID, List<Transaction>> transactions = new HashMap<>();
    private AuctionDatabaseManager db;
    private static final DecimalFormat DF = new DecimalFormat("#,###.##");

    public AuctionManager(AuctionModule module) {
        this.module = module;
    }

    public void initDatabase(AuctionDatabaseManager db) {
        this.db = db;
        listings.addAll(db.loadListings());
    }

    public boolean addListing(UUID sellerUuid, String sellerName, ItemStack item, double price) {
        int max = module.getConfig().getInt("max-listings-per-player", 25);
        long count = listings.stream().filter(l -> l.getSellerUuid().equals(sellerUuid) && !l.isExpired()).count();
        if (count >= max) return false;
        long dur = module.getConfig().getLong("expire-after-seconds", 86400) * 1000L;
        long now = System.currentTimeMillis();
        AuctionListing listing = new AuctionListing(UUID.randomUUID(), sellerUuid, sellerName, item.clone(), price, now, now + dur);
        listings.add(listing);
        if (db != null) module.getPlugin().getServer().getScheduler().runTaskAsynchronously(
                module.getPlugin(), () -> db.saveListings(new java.util.ArrayList<>(listings)));
        return true;
    }

    public boolean removeListing(UUID id) {
        boolean removed = listings.removeIf(l -> l.getId().equals(id));
        if (removed && db != null) module.getPlugin().getServer().getScheduler().runTaskAsynchronously(
                module.getPlugin(), () -> db.saveListings(new java.util.ArrayList<>(listings)));
        return removed;
    }

    public AuctionListing getListing(UUID id) {
        return listings.stream().filter(l -> l.getId().equals(id)).findFirst().orElse(null);
    }

    public List<AuctionListing> getActiveListings() {
        listings.removeIf(AuctionListing::isExpired);
        return new ArrayList<>(listings);
    }

    public List<AuctionListing> getSellerListings(UUID uuid) {
        return listings.stream().filter(l -> l.getSellerUuid().equals(uuid)).collect(Collectors.toList());
    }

    public List<AuctionListing> search(String query) {
        String q = query.toLowerCase();
        return getActiveListings().stream().filter(l -> {
            String type = l.getItem().getType().name().toLowerCase().replace("_", " ");
            if (type.contains(q)) return true;
            if (l.getItem().hasItemMeta() && l.getItem().getItemMeta().hasDisplayName()) {
                String dn = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                        .serialize(l.getItem().getItemMeta().displayName());
                if (dn.toLowerCase().contains(q)) return true;
            }
            return l.getSellerName().toLowerCase().contains(q);
        }).collect(Collectors.toList());
    }

    public List<AuctionListing> filter(String category) {
        if (category == null || category.equalsIgnoreCase("all")) return getActiveListings();
        return getActiveListings().stream().filter(l -> getCategoryOf(l).equalsIgnoreCase(category)).collect(Collectors.toList());
    }

    public List<AuctionListing> sort(List<AuctionListing> list, String mode) {
        List<AuctionListing> sorted = new ArrayList<>(list);
        switch (mode.toUpperCase().replace(" ", "_")) {
            case "HIGHEST_PRICE" -> sorted.sort((a, b) -> Double.compare(b.getPrice(), a.getPrice()));
            case "LOWEST_PRICE" -> sorted.sort(Comparator.comparingDouble(AuctionListing::getPrice));
            case "RECENTLY_LISTED" -> sorted.sort((a, b) -> Long.compare(b.getListedAt(), a.getListedAt()));
        }
        return sorted;
    }

    public void addTransaction(UUID uuid, String otherName, ItemStack item, double amount, Transaction.Type type) {
        Transaction tx = new Transaction(uuid, otherName, item, amount, type, System.currentTimeMillis());
        transactions.computeIfAbsent(uuid, k -> new ArrayList<>()).add(tx);
        if (db != null) module.getPlugin().getServer().getScheduler().runTaskAsynchronously(
                module.getPlugin(), () -> db.saveTransaction(tx));
    }

    public List<Transaction> getTransactions(UUID uuid) {
        if (!transactions.containsKey(uuid) && db != null) {
            List<Transaction> loaded = db.loadTransactions(uuid);
            if (!loaded.isEmpty()) transactions.put(uuid, loaded);
        }
        return transactions.getOrDefault(uuid, new ArrayList<>());
    }

    public double getTotalMade(UUID uuid) {
        return getTransactions(uuid).stream().filter(t -> t.getType() == Transaction.Type.SOLD).mapToDouble(Transaction::getAmount).sum();
    }

    public double getTotalSpent(UUID uuid) {
        return getTransactions(uuid).stream().filter(t -> t.getType() == Transaction.Type.BOUGHT).mapToDouble(Transaction::getAmount).sum();
    }

    public String format(double v) { return "$" + DF.format(v); }

    private String getCategoryOf(AuctionListing l) {
        String t = l.getItem().getType().name();
        if (t.contains("SWORD")||t.contains("BOW")||t.contains("CROSSBOW")||t.contains("TRIDENT")||
            t.contains("HELMET")||t.contains("CHESTPLATE")||t.contains("LEGGINGS")||t.contains("BOOTS")) return "COMBAT";
        if (t.contains("PICKAXE")||t.contains("SHOVEL")||t.contains("HOE")||t.contains("AXE")||
            t.contains("SHEARS")||t.contains("FISHING_ROD")||t.contains("FLINT_AND_STEEL")) return "TOOLS";
        if (t.contains("BEEF")||t.contains("PORK")||t.contains("CHICKEN")||t.contains("BREAD")||
            t.contains("APPLE")||t.contains("CARROT")||t.contains("POTATO")||t.contains("MELON")||
            t.contains("BERRY")||t.contains("MUSHROOM_STEW")||t.contains("RABBIT_STEW")) return "FOOD";
        if (t.contains("POTION")||t.contains("ARROW")) return "POTIONS";
        if (t.contains("BOOK")) return "BOOKS";
        if (t.contains("SEEDS")||t.contains("WHEAT")||t.contains("BAMBOO")||t.contains("CACTUS")||
            t.contains("SUGAR_CANE")||t.contains("KELP")||t.contains("NETHER_WART")) return "INGREDIENTS";
        if (t.endsWith("_LOG")||t.endsWith("_PLANKS")||t.contains("STONE")||t.contains("SAND")||
            t.contains("DIRT")||t.contains("GRAVEL")||t.endsWith("_BLOCK")) return "BLOCKS";
        return "UTILITIES";
    }
            }
                                     
