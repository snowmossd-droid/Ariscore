package me.vennlmao.ariscore.auction.managers;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.vennlmao.ariscore.ArisCore;
import me.vennlmao.ariscore.auction.utils.AuctionItem;
import me.vennlmao.ariscore.auction.utils.EcoUtil;
import me.vennlmao.ariscore.auction.utils.ItemSerializer;
import me.vennlmao.ariscore.auction.utils.PendingPayment;
import me.vennlmao.ariscore.auction.utils.Transaction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class AuctionManager {

    private final ArisCore plugin;
    private final List<AuctionItem> activeAuctions = new ArrayList<>();
    private final List<PendingPayment> pendingPayments = new ArrayList<>();
    private AuctionDataManager dataManager;
    private ScheduledTask expireTask;

    public AuctionManager(ArisCore plugin) {
        this.plugin = plugin;
        this.dataManager = new AuctionDataManager(plugin);
        loadAuctions();
        startExpireTask();
    }

    private void loadAuctions() {
        activeAuctions.addAll(dataManager.loadAuctions());
        pendingPayments.addAll(dataManager.loadPendingPayments());
    }

    private void startExpireTask() {
        int interval = plugin.getAuctionModule().getConfigManager().getExpireCheckInterval();
        expireTask = plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(
            plugin, t -> checkExpired(), interval * 20L, interval * 20L
        );
    }

    public void stopExpireTask() {
        if (expireTask != null) expireTask.cancel();
    }

    private void checkExpired() {
        List<AuctionItem> expired = activeAuctions.stream().filter(AuctionItem::isExpired).collect(Collectors.toList());
        for (AuctionItem a : expired) {
            activeAuctions.remove(a);
            Player seller = Bukkit.getPlayer(a.getSellerUUID());
            if (seller != null && seller.isOnline()) {
                if (seller.getInventory().firstEmpty() == -1) {
                    seller.getWorld().dropItemNaturally(seller.getLocation(), a.getItemStack());
                } else {
                    seller.getInventory().addItem(a.getItemStack());
                }
                LangManager lang = plugin.getAuctionModule().getLangManager();
                seller.sendMessage(lang.getExpired(
                        String.valueOf(a.getItemStack().getAmount()),
                        lang.formatItemName(a.getItemStack()),
                        new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date(a.getExpireTime()))
                ));
            }
        }
    }

    public String createAuction(Player player, ItemStack item, double price) {
        AuctionConfigManager cfg = plugin.getAuctionModule().getConfigManager();
        LangManager lang = plugin.getAuctionModule().getLangManager();

        if (cfg.isBlacklisted(item.getType())) return lang.getBlacklistedItem(lang.formatItemName(item));
        if (price < cfg.getMinimumPrice()) return lang.getPriceTooLow(EcoUtil.format(cfg.getMinimumPrice(), false, cfg));
        if (price > cfg.getMaximumPrice()) return lang.getPriceTooHigh(EcoUtil.format(cfg.getMaximumPrice(), false, cfg));

        int max = getMaxAuctionsForPlayer(player);
        if (getPlayerAuctionCount(player.getUniqueId()) >= max) {
            return lang.getLimitReached(String.valueOf(getPlayerAuctionCount(player.getUniqueId())), String.valueOf(max));
        }

        long now = System.currentTimeMillis();
        long expire = now + (cfg.getAuctionDurationHours() * 3600000L);
        AuctionItem auction = new AuctionItem(UUID.randomUUID(), player.getUniqueId(), player.getName(), item.clone(), price, now, expire);
        activeAuctions.add(auction);

        String broadcast = lang.getChatBroadcast(
                player.getName(),
                String.valueOf(item.getAmount()),
                lang.formatItemName(item),
                EcoUtil.format(price, true, cfg)
        );
        if (broadcast != null) Bukkit.broadcastMessage(broadcast);

        return null;
    }

    public boolean purchaseAuction(Player buyer, UUID auctionId) {
        AuctionItem auction = getAuctionById(auctionId);
        if (auction == null) return false;
        if (auction.isExpired()) {
            activeAuctions.remove(auction);
            return false;
        }

        AuctionConfigManager cfg = plugin.getAuctionModule().getConfigManager();
        LangManager lang = plugin.getAuctionModule().getLangManager();

        if (!plugin.getAuctionModule().getEconomy().has(buyer, auction.getPrice())) {
            buyer.sendMessage(lang.getNotEnoughMoney());
            return false;
        }
        if (buyer.getInventory().firstEmpty() == -1) {
            buyer.sendMessage(lang.getInventoryFull());
            return false;
        }

        plugin.getAuctionModule().getEconomy().withdrawPlayer(buyer, auction.getPrice());

        double fee = auction.getPrice() * (cfg.getFeePercentage() / 100.0);
        double sellerAmount = auction.getPrice() - fee;

        Player seller = Bukkit.getPlayer(auction.getSellerUUID());
        if (seller != null && seller.isOnline()) {
            plugin.getAuctionModule().getEconomy().depositPlayer(seller, sellerAmount);
            seller.sendMessage(lang.getItemSold(
                    buyer.getName(),
                    String.valueOf(auction.getItemStack().getAmount()),
                    lang.formatItemName(auction.getItemStack()),
                    EcoUtil.format(sellerAmount, true, cfg)
            ));
        } else {
            pendingPayments.add(new PendingPayment(
                    auction.getSellerUUID(), auction.getSellerName(),
                    sellerAmount, lang.formatItemName(auction.getItemStack()),
                    auction.getItemStack().getAmount()
            ));
            dataManager.savePendingPayments(pendingPayments);
        }

        buyer.getInventory().addItem(auction.getItemStack());
        activeAuctions.remove(auction);

        Transaction t = new Transaction(
                UUID.randomUUID(), buyer.getUniqueId(), buyer.getName(),
                auction.getSellerUUID(), auction.getSellerName(), auction.getAuctionId(),
                ItemSerializer.toBase64(auction.getItemStack()), lang.formatItemName(auction.getItemStack()),
                auction.getItemStack().getAmount(), auction.getPrice(), fee,
                System.currentTimeMillis(), Transaction.Type.PURCHASE
        );
        dataManager.saveTransaction(t);

        Transaction tSale = new Transaction(
                UUID.randomUUID(), buyer.getUniqueId(), buyer.getName(),
                auction.getSellerUUID(), auction.getSellerName(), auction.getAuctionId(),
                ItemSerializer.toBase64(auction.getItemStack()), lang.formatItemName(auction.getItemStack()),
                auction.getItemStack().getAmount(), sellerAmount, fee,
                System.currentTimeMillis(), Transaction.Type.SALE
        );
        dataManager.saveTransaction(tSale);

        buyer.sendMessage(lang.getItemBought(
                String.valueOf(auction.getItemStack().getAmount()),
                lang.formatItemName(auction.getItemStack()),
                EcoUtil.format(auction.getPrice(), true, cfg)
        ));

        return true;
    }

    public boolean removeAuction(UUID auctionId, Player requester) {
        AuctionItem auction = getAuctionById(auctionId);
        if (auction == null) return false;

        activeAuctions.remove(auction);

        if (requester.getInventory().firstEmpty() == -1) {
            requester.getWorld().dropItemNaturally(requester.getLocation(), auction.getItemStack());
        } else {
            requester.getInventory().addItem(auction.getItemStack());
        }
        return true;
    }

    public void processPendingPayments(Player player) {
        List<PendingPayment> toProcess = pendingPayments.stream()
                .filter(p -> p.getSellerUUID().equals(player.getUniqueId()))
                .collect(Collectors.toList());

        LangManager lang = plugin.getAuctionModule().getLangManager();
        AuctionConfigManager cfg = plugin.getAuctionModule().getConfigManager();

        for (PendingPayment p : toProcess) {
            plugin.getAuctionModule().getEconomy().depositPlayer(player, p.getAmount());
            player.sendMessage(lang.getOfflineAuctionSold(
                    String.valueOf(p.getItemAmount()), p.getItemName(),
                    EcoUtil.format(p.getAmount(), true, cfg)
            ));
            pendingPayments.remove(p);
        }
        if (!toProcess.isEmpty()) dataManager.savePendingPayments(pendingPayments);
    }

    public void saveAllAuctions() {
        Map<UUID, List<AuctionItem>> playerMap = new HashMap<>();
        for (AuctionItem a : activeAuctions) {
            playerMap.computeIfAbsent(a.getSellerUUID(), k -> new ArrayList<>()).add(a);
        }
        dataManager.saveAuctions(activeAuctions, playerMap);
        dataManager.savePendingPayments(pendingPayments);
    }

    public List<AuctionItem> getActiveAuctions() { return new ArrayList<>(activeAuctions); }

    public List<AuctionItem> getPlayerAuctions(UUID playerUUID) {
        return activeAuctions.stream().filter(a -> a.getSellerUUID().equals(playerUUID)).collect(Collectors.toList());
    }

    public AuctionItem getAuctionById(UUID id) {
        return activeAuctions.stream().filter(a -> a.getAuctionId().equals(id)).findFirst().orElse(null);
    }

    public int getPlayerAuctionCount(UUID playerUUID) {
        return (int) activeAuctions.stream().filter(a -> a.getSellerUUID().equals(playerUUID)).count();
    }

    public int getMaxAuctionsForPlayer(Player player) {
        int max = 0;
        for (int i = 99; i >= 1; i--) {
            if (player.hasPermission("ariscore.auction.limit." + i)) { max = i; break; }
        }
        if (max == 0) max = 10;
        return max;
    }

    public AuctionDataManager getDataManager() { return dataManager; }
        }
