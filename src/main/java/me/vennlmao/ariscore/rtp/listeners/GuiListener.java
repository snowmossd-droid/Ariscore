package me.vennlmao.ariscore.rtp.listeners;

import me.vennlmao.ariscore.rtp.RtpModule;
import me.vennlmao.ariscore.rtp.managers.LocationFinder;
import me.vennlmao.ariscore.rtp.utils.GuiUtil;
import me.vennlmao.ariscore.rtp.utils.MessageUtil;
import me.vennlmao.ariscore.rtp.utils.SoundUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GuiListener implements Listener {

    private final RtpModule plugin;

    public GuiListener(RtpModule plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String clickedTitle = PlainTextComponentSerializer.plainText()
                .serialize(event.getView().title());

        String mainTitle = GuiUtil.stripColor(
                plugin.getConfig().getString("gui.main.title", ""));
        String subTitle = GuiUtil.stripColor(
                plugin.getConfig().getString("gui.world_select.title", ""));

        boolean isMain = clickedTitle.equals(mainTitle);
        boolean isSub = clickedTitle.equals(subTitle);

        if (!isMain && !isSub) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.displayName() == null) return;

        SoundUtil.play(player, "click");

        int slot = event.getSlot();

        if (isMain) {
            handleMainClick(player, slot);
        } else {
            handleSubClick(player, slot);
        }
    }

    private void handleMainClick(Player player, int slot) {
        ConfigurationSection worlds = plugin.getConfig().getConfigurationSection("worlds");
        if (worlds == null) return;

        for (String key : worlds.getKeys(false)) {
            ConfigurationSection sec = worlds.getConfigurationSection(key);
            if (sec == null) continue;
            if (sec.getInt("slot", -1) != slot) continue;

            boolean hasSub = sec.getBoolean("has_sub_worlds", false);
            if (hasSub) {
                player.getScheduler().run(plugin.getPlugin(), t -> {
                    player.openInventory(GuiUtil.buildSubWorldGui(plugin, key, player));
                    SoundUtil.play(player, "click");
                }, null);
            } else {
                player.closeInventory();
                startRtp(player, sec, plugin.getConfig().getString("worlds." + key + ".world", ""));
            }
            return;
        }
    }

    private void handleSubClick(Player player, int slot) {
        ConfigurationSection worlds = plugin.getConfig().getConfigurationSection("worlds");
        if (worlds == null) return;

        for (String worldKey : worlds.getKeys(false)) {
            ConfigurationSection sec = worlds.getConfigurationSection(worldKey);
            if (sec == null || !sec.getBoolean("has_sub_worlds", false)) continue;

            ConfigurationSection subWorlds = sec.getConfigurationSection("sub_worlds");
            if (subWorlds == null) continue;

            for (String subKey : subWorlds.getKeys(false)) {
                ConfigurationSection subSec = subWorlds.getConfigurationSection(subKey);
                if (subSec == null) continue;
                if (subSec.getInt("slot", -1) != slot) continue;

                player.closeInventory();
                startRtp(player, subSec, subSec.getString("world", ""));
                return;
            }
        }
    }

    private void startRtp(Player player, ConfigurationSection sec, String worldName) {
        if (worldName == null || worldName.isEmpty()) {
            MessageUtil.sendChatList(player, "world_disabled");
            SoundUtil.play(player, "error");
            return;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            MessageUtil.sendChatList(player, "world_disabled");
            SoundUtil.play(player, "error");
            return;
        }

        MessageUtil.sendChatList(player, "searching_location");
        MessageUtil.sendActionbar(player, "searching_location_ab");
        SoundUtil.play(player, "searching");

        LocationFinder.findSafe(world, sec).thenAccept(location -> {
            if (location == null) {
                player.getScheduler().run(plugin.getPlugin(), t -> {
                    MessageUtil.sendChatList(player, "no_safe_location");
                    MessageUtil.sendActionbar(player, "no_safe_location_ab");
                    SoundUtil.play(player, "error");
                }, null);
                return;
            }

            player.getScheduler().run(plugin.getPlugin(), t -> {
                plugin.getWarmupManager().startWarmup(player, location, worldName);
            }, null);
        });
    }
            }
