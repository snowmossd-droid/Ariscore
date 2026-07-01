package me.vennlmao.ariscore.afk.listeners;

import me.vennlmao.ariscore.afk.AfkModule;
import me.vennlmao.ariscore.afk.gui.AfkGuiBuilder;
import me.vennlmao.ariscore.afk.utils.ColorUtil;
import me.vennlmao.ariscore.afk.utils.MessageUtil;
import me.vennlmao.ariscore.afk.utils.SoundUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AfkGuiListener implements Listener {

    private final AfkModule module;

    public AfkGuiListener(AfkModule module) {
        this.module = module;
    }

    public void openAfksGui(Player player) {
        player.getScheduler().run(module.getPlugin(), t ->
                player.openInventory(AfkGuiBuilder.buildAfksGui(module, player)), null);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title    = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        String guiTitle = ColorUtil.strip(module.getConfig().getString("gui.title", "ᴀꜰᴋ ᴢᴏɴᴇꜱ"));

        if (!title.equals(guiTitle)) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        SoundUtil.play(player, "click");

        int clickedSlot = event.getSlot();
        int randomSlot  = module.getConfig().getInt("gui.afk-info.random-afk.slot", 22);
        int guiSize     = module.getConfig().getInt("gui.size", 27);

        Map<String, Location> zones = module.getAfkManager().getAllZones();
        List<String> names = new ArrayList<>(zones.keySet());

        if (clickedSlot == randomSlot) {
            Location random = module.getAfkManager().getRandomZone();
            if (random == null) {
                SoundUtil.play(player, "error");
                MessageUtil.sendChat(player, "no_afks");
                MessageUtil.sendActionbar(player, "no_afks_ab");
                return;
            }
            player.closeInventory();
            String randomLabel = module.getConfig().getString("gui.afk-info.random-afk.displayName",
                    module.getConfig().getString("default-name", "afk"));
            module.getWarmupManager().startWarmup(player, ColorUtil.strip(randomLabel), random);
            return;
        }

        int slot = 0;
        String targetName = null;
        for (String name : names) {
            if (slot == randomSlot) slot++;
            if (slot >= guiSize) break;
            if (slot == clickedSlot) { targetName = name; break; }
            slot++;
        }

        if (targetName == null) return;

        Location loc = module.getAfkManager().getZone(targetName);
        if (loc == null || loc.getWorld() == null) {
            SoundUtil.play(player, "error");
            MessageUtil.sendChat(player, "world_not_found");
            MessageUtil.sendActionbar(player, "world_not_found_ab");
            return;
        }

        player.closeInventory();
        module.getWarmupManager().startWarmup(player, targetName, loc);
    }
}
