package me.vennlmao.ariscore.auction.listeners;

import me.vennlmao.ariscore.auction.AuctionModule;
import me.vennlmao.ariscore.auction.data.AuctionListing;
import me.vennlmao.ariscore.auction.gui.GuiBuilder;
import me.vennlmao.ariscore.auction.utils.ColorUtil;
import me.vennlmao.ariscore.auction.utils.MessageUtil;
import me.vennlmao.ariscore.auction.utils.SoundUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.text.DecimalFormat;
import java.util.List;
import java.util.UUID;

public class AuctionChatListener implements Listener {

    private final AuctionModule module;
    private static final DecimalFormat DF = new DecimalFormat("#,###.##");

    public AuctionChatListener(AuctionModule module) {
        this.module = module;
    
    private String getDefaultSort() {
        List<String> opts = module.getConfig().getStringList("gui.items.sort.lore");
        return opts.isEmpty() ? "" : opts.get(opts.size() - 1).trim();
    }

    private String getDefaultFilter() {
        List<String> opts = module.getConfig().getStringList("gui.items.filter.lore");
        return opts.isEmpty() ? "" : opts.get(0).trim();
    }
}

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player p = event.getPlayer();
        UUID uuid = p.getUniqueId();
        String mode = module.getSearching().get(uuid);
        if (mode == null) return;

        event.setCancelled(true);
        module.getSearching().remove(uuid);
        String msg = event.getMessage().trim();

        switch (mode) {
            case "MAIN_SEARCH" -> {
                if (msg.equalsIgnoreCase("cancel")) {
                    p.getScheduler().run(module.getPlugin(), t -> module.getGuiListener().openMain(p), null);
                    return;
                }
                List<AuctionListing> results = module.getAuctionManager().search(msg);
                p.getScheduler().run(module.getPlugin(), t ->
                        p.openInventory(GuiBuilder.buildMain(module, p, results, 0, getDefaultSort(), getDefaultFilter())), null);
            }
            case "SELL_PRICE" -> {
                if (msg.equalsIgnoreCase("cancel")) {
                    p.getScheduler().run(module.getPlugin(), t -> module.getGuiListener().openMain(p), null);
                    return;
                }
                try {
                    double price = Double.parseDouble(msg);
                    double min = module.getConfig().getDouble("min-price", 1.0);
                    double max = module.getConfig().getDouble("max-price", 10000000.0);
                    if (price < min || price > max) {
                        MessageUtil.sendChat(p, "invalid_price", s -> s.replace("{min}", DF.format(min)).replace("{max}", DF.format(max)));
                        MessageUtil.sendActionbar(p, "invalid_price");
                        SoundUtil.play(p, "error");
                        return;
                    }
                    module.getGuiListener().setPendingPrice(uuid, price);
                    p.getScheduler().run(module.getPlugin(), t -> p.openInventory(GuiBuilder.buildSellInput(module)), null);
                } catch (NumberFormatException e) {
                    MessageUtil.sendChat(p, "invalid_price_format");
                    MessageUtil.sendActionbar(p, "invalid_price_format");
                    SoundUtil.play(p, "error");
                }
            }
            case "TX_SEARCH" -> {
                List<me.vennlmao.ariscore.auction.data.Transaction> txList = module.getAuctionManager().getTransactions(uuid)
                        .stream().filter(t -> t.getOtherName().toLowerCase().contains(msg.toLowerCase())).toList();
                MessageUtil.sendChat(p, "tx_search_result", s -> s.replace("{player}", msg).replace("{count}", String.valueOf(txList.size())));
                MessageUtil.sendActionbar(p, "tx_search_result", s -> s.replace("{player}", msg).replace("{count}", String.valueOf(txList.size())));
                p.getScheduler().run(module.getPlugin(), t -> p.openInventory(GuiBuilder.buildTransactions(module, p, 0)), null);
            }
        }
    }
}
