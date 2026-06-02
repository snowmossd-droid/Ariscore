package me.vennlmao.ariscore.home.listeners;

import me.vennlmao.ariscore.home.HomeModule;
import me.vennlmao.ariscore.home.gui.GuiBuilder;
import me.vennlmao.ariscore.home.utils.ColorUtil;
import me.vennlmao.ariscore.home.utils.MessageUtil;
import me.vennlmao.ariscore.home.utils.SoundUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HomesListener implements Listener {

    private final HomeModule plugin;
    private final Map<UUID, String> pendingDelete = new HashMap<>();

    public HomesListener(HomeModule plugin) {
        this.plugin = plugin;
    }

    public void openHomesGui(Player player) {
        player.getScheduler().run(plugin.getPlugin(), t ->
                player.openInventory(GuiBuilder.buildHomesGui(plugin, player)), null);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        String homesTitle = ColorUtil.strip(plugin.getConfig().getString("gui.title", "ʜᴏᴍᴇꜱ"));
        String confirmTitle = ColorUtil.strip(plugin.getConfig().getString("gui-confirm-delete.title", "Confirm Delete"));

        if (title.equals(homesTitle)) {
            event.setCancelled(true);
            handleHomesClick(player, event.getSlot());
        } else if (title.equals(confirmTitle)) {
            event.setCancelled(true);
            handleConfirmClick(player, event.getSlot());
        }
    }

    private void handleHomesClick(Player player, int slot) {
        List<Integer> bedSlots = GuiBuilder.parseSlots(plugin.getConfig().getString("gui.slots.beds", "11,12,13,14,15"));
        List<Integer> dyeSlots = GuiBuilder.parseSlots(plugin.getConfig().getString("gui.slots.dyes", "20,21,22,23,24"));

        List<String> homes = plugin.getHomeManager().getHomes(player);
        int maxHomes = plugin.getHomeManager().getMaxHomes(player);

        if (bedSlots.contains(slot)) {
            int index = bedSlots.indexOf(slot);
            if (index >= maxHomes) return;

            if (index < homes.size()) {
                String homeName = homes.get(index);
                SoundUtil.play(player, "click");
                player.closeInventory();
                plugin.getWarmupManager().startWarmup(player, homeName);
            } else {
                List<String> blockedWorlds = plugin.getConfig().getStringList("blocked_worlds");
                if (blockedWorlds.contains(player.getWorld().getName())) {
                    SoundUtil.play(player, "error");
                    MessageUtil.sendChat(player, "world_blocked");
                    MessageUtil.sendActionbar(player, "world_blocked_ab");
                    return;
                }
                if (homes.size() >= maxHomes) {
                    SoundUtil.play(player, "error");
                    MessageUtil.sendChat(player, "max_homes_reached");
                    MessageUtil.sendActionbar(player, "max_homes_reached_ab");
                    return;
                }
                String homeName = "home" + (index + 1);
                plugin.getHomeManager().setHome(player, homeName);
                SoundUtil.play(player, "sethome");
                MessageUtil.sendChat(player, "sethome", s -> s.replace("%home%", homeName));
                MessageUtil.sendActionbar(player, "sethome_ab", s -> s.replace("%home%", homeName));
                player.closeInventory();
                player.getScheduler().run(plugin.getPlugin(), t -> openHomesGui(player), null);
            }
        } else if (dyeSlots.contains(slot)) {
            int index = dyeSlots.indexOf(slot);
            if (index >= maxHomes) return;
            if (index >= homes.size()) return;

            String homeName = homes.get(index);
            SoundUtil.play(player, "click");
            pendingDelete.put(player.getUniqueId(), homeName);
            player.getScheduler().run(plugin.getPlugin(), t ->
                    player.openInventory(GuiBuilder.buildConfirmDelete(plugin, homeName)), null);
        }
    }

    private void handleConfirmClick(Player player, int slot) {
        int confirmSlot = plugin.getConfig().getInt("gui-confirm-delete.items.confirm.slot", 15);
        int cancelSlot = plugin.getConfig().getInt("gui-confirm-delete.items.cancel.slot", 11);

        if (slot == confirmSlot) {
            String homeName = pendingDelete.remove(player.getUniqueId());
            if (homeName == null) return;
            SoundUtil.play(player, "deletehome");
            plugin.getHomeManager().deleteHome(player, homeName);
            MessageUtil.sendChat(player, "deletehome", s -> s.replace("%home%", homeName));
            MessageUtil.sendActionbar(player, "deletehome_ab", s -> s.replace("%home%", homeName));
            player.closeInventory();
            player.getScheduler().run(plugin.getPlugin(), t -> openHomesGui(player), null);
        } else if (slot == cancelSlot) {
            SoundUtil.play(player, "click");
            pendingDelete.remove(player.getUniqueId());
            player.closeInventory();
            player.getScheduler().run(plugin.getPlugin(), t -> openHomesGui(player), null);
        }
    }
}
