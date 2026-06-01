package me.vennlmao.ariscore.shop.listeners;

import me.vennlmao.ariscore.shop.ShopModule;
import me.vennlmao.ariscore.shop.gui.GuiBuilder;
import me.vennlmao.ariscore.shop.gui.GuiSession;
import me.vennlmao.ariscore.shop.managers.ShopItem;
import me.vennlmao.ariscore.shop.utils.ColorUtil;
import me.vennlmao.ariscore.shop.utils.SoundUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShopListener implements Listener {

    private final ShopModule plugin;
    private final Map<UUID, GuiSession> sessions = new HashMap<>();

    public ShopListener(ShopModule plugin) {
        this.plugin = plugin;
    }

    public void openShop(Player player) {
        GuiSession session = new GuiSession();
        sessions.put(player.getUniqueId(), session);
        player.getScheduler().run(plugin.getPlugin(), t -> player.openInventory(GuiBuilder.buildMainShop(plugin)), null);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        GuiSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        int slot = event.getSlot();

        switch (session.getScreen()) {
            case MAIN -> handleMain(player, session, slot);
            case CATEGORY -> handleCategory(player, session, slot);
            case PURCHASE -> handlePurchase(player, session, slot);
        }
    }

    private void handleMain(Player player, GuiSession session, int slot) {
        for (String cat : plugin.getShopManager().getCategories()) {
            int catSlot = plugin.getConfig().getInt("gui.categories." + cat + ".slot", -1);
            if (catSlot == slot) {
                SoundUtil.play(player, "click");
                session.setCategory(cat);
                session.setScreen(GuiSession.Screen.CATEGORY);
                player.getScheduler().run(plugin.getPlugin(), t -> player.openInventory(GuiBuilder.buildCategory(plugin, cat)), null);
                return;
            }
        }
    }

    private void handleCategory(Player player, GuiSession session, int slot) {
        int backSlot = plugin.getConfig().getInt("gui-back.back.slot", 18);
        if (slot == backSlot) {
            SoundUtil.play(player, "click");
            session.setScreen(GuiSession.Screen.MAIN);
            player.getScheduler().run(plugin.getPlugin(), t -> player.openInventory(GuiBuilder.buildMainShop(plugin)), null);
            return;
        }

        ShopItem item = plugin.getShopManager().findItem(session.getCategory(), slot);
        if (item == null) return;

        SoundUtil.play(player, "click");
        session.setSelectedItem(item);
        session.setAmount(1);
        session.setScreen(GuiSession.Screen.PURCHASE);
        player.getScheduler().run(plugin.getPlugin(), t -> player.openInventory(GuiBuilder.buildPurchase(plugin, item, 1)), null);
    }

    private void handlePurchase(Player player, GuiSession session, int slot) {
        ShopItem item = session.getSelectedItem();
        if (item == null) return;

        int confirmSlot = plugin.getConfig().getInt("gui-purchase.confirm.slot", 23);
        int cancelSlot = plugin.getConfig().getInt("gui-purchase.cancel.slot", 21);
        int addOneSlot = plugin.getConfig().getInt("gui-purchase.add.one.slot", 15);
        int addTenSlot = plugin.getConfig().getInt("gui-purchase.add.ten.slot", 16);
        int addMaxSlot = plugin.getConfig().getInt("gui-purchase.add.max.slot", 17);
        int removeOneSlot = plugin.getConfig().getInt("gui-purchase.remove.one.slot", 11);
        int removeTenSlot = plugin.getConfig().getInt("gui-purchase.remove.ten.slot", 10);
        int removeMaxSlot = plugin.getConfig().getInt("gui-purchase.remove.max.slot", 9);

        int amount = session.getAmount();
        int maxStack = item.getStack();

        if (slot == cancelSlot) {
            SoundUtil.play(player, "click");
            session.setScreen(GuiSession.Screen.CATEGORY);
            session.setAmount(1);
            player.getScheduler().run(plugin.getPlugin(), t -> player.openInventory(GuiBuilder.buildCategory(plugin, session.getCategory())), null);
            return;
        }

        if (slot == confirmSlot) {
            processPurchase(player, item, amount, session);
            return;
        }

        if (slot == addOneSlot) amount = Math.min(amount + 1, maxStack);
        else if (slot == addTenSlot) amount = Math.min(amount + 10, maxStack);
        else if (slot == addMaxSlot) amount = maxStack;
        else if (slot == removeOneSlot) amount = Math.max(amount - 1, 1);
        else if (slot == removeTenSlot) amount = Math.max(amount - 10, 1);
        else if (slot == removeMaxSlot) amount = 1;
        else return;

        SoundUtil.play(player, "click");
        session.setAmount(amount);
        player.getScheduler().run(plugin.getPlugin(), t -> player.openInventory(GuiBuilder.buildPurchase(plugin, item, amount)), null);
    }

    private void processPurchase(Player player, ShopItem item, int amount, GuiSession session) {
        double totalCost = item.getPrice() * amount;

        if (item.isShard()) {
            double shards = plugin.getShardsManager().getShards(player);
            if (shards < totalCost) {
                SoundUtil.play(player, "no_shards");
                sendMessage(player, "messages.purchase.shards");
                sendActionbar(player, "messages.purchase.shards_ab");
                return;
            }
            if (!hasSpace(player, item)) {
                SoundUtil.play(player, "inventory_full");
                sendMessage(player, "messages.purchase.inventory_full");
                sendActionbar(player, "messages.purchase.inventory_full_ab");
                return;
            }
            plugin.getShardsManager().takeShards(player, (int) totalCost);
            executeCommands(player, item, amount);
        } else {
            if (plugin.getEconomy() == null) return;
            if (!plugin.getEconomy().has(player, totalCost)) {
                SoundUtil.play(player, "no_money");
                sendMessage(player, "messages.purchase.money");
                sendActionbar(player, "messages.purchase.money_ab");
                return;
            }
            if (!hasSpace(player, item)) {
                SoundUtil.play(player, "inventory_full");
                sendMessage(player, "messages.purchase.inventory_full");
                sendActionbar(player, "messages.purchase.inventory_full_ab");
                return;
            }
            plugin.getEconomy().withdrawPlayer(player, totalCost);
            if (item.getCommands().isEmpty()) {
                giveItems(player, item, amount);
            } else {
                executeCommands(player, item, amount);
            }
        }

        SoundUtil.play(player, "purchase_success");
        player.getScheduler().run(plugin.getPlugin(), t -> player.openInventory(GuiBuilder.buildPurchase(plugin, item, session.getAmount())), null);
    }

    private void giveItems(Player player, ShopItem item, int amount) {
        int remaining = amount;
        while (remaining > 0) {
            int give = Math.min(remaining, item.getMaterial().getMaxStackSize());
            player.getInventory().addItem(new ItemStack(item.getMaterial(), give));
            remaining -= give;
        }
    }

    private void executeCommands(Player player, ShopItem item, int amount) {
        for (String cmd : item.getCommands()) {
            String replaced = cmd
                    .replace("{player}", player.getName())
                    .replace("{amount}", String.valueOf(amount));
            plugin.getPlugin().getServer().getGlobalRegionScheduler().run(plugin.getPlugin(), t ->
                    plugin.getPlugin().getServer().dispatchCommand(plugin.getPlugin().getServer().getConsoleSender(), replaced));
        }
    }

    private boolean hasSpace(Player player, ShopItem item) {
        if (!item.getCommands().isEmpty()) return true;
        for (ItemStack i : player.getInventory().getStorageContents()) {
            if (i == null) return true;
        }
        return false;
    }

    private void sendMessage(Player player, String path) {
        String msg = plugin.getConfig().getString(path, "");
        if (!msg.isEmpty()) player.sendMessage(ColorUtil.parse(msg));
    }

    private void sendActionbar(Player player, String path) {
        String msg = plugin.getConfig().getString(path, "");
        if (!msg.isEmpty()) player.sendActionBar(ColorUtil.parse(msg));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        GuiSession session = sessions.get(player.getUniqueId());
        if (session != null && session.getScreen() == GuiSession.Screen.MAIN) {
            sessions.remove(player.getUniqueId());
        }
    }
}
