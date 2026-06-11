package me.vennlmao.ariscore.auction.commands;

import me.vennlmao.ariscore.auction.AuctionModule;
import me.vennlmao.ariscore.auction.utils.MessageUtil;
import me.vennlmao.ariscore.auction.utils.SoundUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.List;

public class AuctionCommand implements CommandExecutor {

    private final AuctionModule module;
    private static final DecimalFormat DF = new DecimalFormat("#,###.##");

    public AuctionCommand(AuctionModule module) {
        this.module = module;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) return true;

        if (args.length == 0) {
            p.getScheduler().run(module.getPlugin(), t -> { SoundUtil.play(p,"open"); module.getGuiListener().openMain(p); }, null);
            return true;
        }

        if (args[0].equalsIgnoreCase("sell")) {
            if (args.length < 2) {
                MessageUtil.sendChat(p, "usage");
                MessageUtil.sendActionbar(p, "usage");
                return true;
            }
            try {
                double price = Double.parseDouble(args[1]);
                double min = module.getConfig().getDouble("min-price", 1.0);
                double max = module.getConfig().getDouble("max-price", 10000000.0);
                if (price < min || price > max) {
                    MessageUtil.sendChat(p, "invalid_price", s -> s.replace("{min}", DF.format(min)).replace("{max}", DF.format(max)));
                    MessageUtil.sendActionbar(p, "invalid_price");
                    SoundUtil.play(p, "error");
                    return true;
                }
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand.getType().isAir()) {
                    module.getGuiListener().setPendingPrice(p.getUniqueId(), price);
                    p.getScheduler().run(module.getPlugin(), t -> p.openInventory(me.vennlmao.ariscore.auction.gui.GuiBuilder.buildSellInput(module)), null);
                } else {
                    if (module.getConfig().getBoolean("disallow-renamed-items", true) && hand.hasItemMeta() && hand.getItemMeta().hasDisplayName()) {
                        MessageUtil.sendChat(p, "renamed_not_allowed");
                        MessageUtil.sendActionbar(p, "renamed_not_allowed");
                        SoundUtil.play(p, "error");
                        return true;
                    }
                    ItemStack toList = hand.clone();
                    p.getInventory().setItemInMainHand(null);
                    boolean listed = module.getAuctionManager().addListing(p.getUniqueId(), p.getName(), toList, price);
                    if (!listed) {
                        p.getInventory().setItemInMainHand(toList);
                        MessageUtil.sendChat(p,"max_listings"); MessageUtil.sendActionbar(p,"max_listings"); SoundUtil.play(p,"error"); return true;
                    }
                    MessageUtil.sendChat(p,"listed",s->s.replace("{item}",toList.getType().name()).replace("{price}","$"+DF.format(price)));
                    MessageUtil.sendActionbar(p,"listed",s->s.replace("{item}",toList.getType().name()).replace("{price}","$"+DF.format(price)));
                    SoundUtil.play(p,"list_success");
                }
            } catch (NumberFormatException e) {
                MessageUtil.sendChat(p, "invalid_price_format");
                MessageUtil.sendActionbar(p, "invalid_price_format");
            }
            return true;
        }

        String query = String.join(" ", args);
        p.getScheduler().run(module.getPlugin(), t -> {
            var results = module.getAuctionManager().search(query);
            List<String> sortOpts = module.getConfig().getStringList("gui.items.sort.lore");
                List<String> filterOpts = module.getConfig().getStringList("gui.items.filter.lore");
                String defSort = sortOpts.isEmpty() ? "" : sortOpts.get(sortOpts.size()-1).trim();
                String defFilter = filterOpts.isEmpty() ? "" : filterOpts.get(0).trim();
                p.openInventory(me.vennlmao.ariscore.auction.gui.GuiBuilder.buildMain(module, p, results, 0, defSort, defFilter));
        }, null);
        return true;
    }
    }
                    
