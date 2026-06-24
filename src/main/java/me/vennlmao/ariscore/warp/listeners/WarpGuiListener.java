package me.vennlmao.ariscore.warp.listeners;

import me.vennlmao.ariscore.warp.WarpModule;
import me.vennlmao.ariscore.warp.gui.WarpGuiBuilder;
import me.vennlmao.ariscore.warp.utils.ColorUtil;
import me.vennlmao.ariscore.warp.utils.MessageUtil;
import me.vennlmao.ariscore.warp.utils.SoundUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WarpGuiListener implements Listener {

    private final WarpModule module;

    public WarpGuiListener(WarpModule module) { this.module = module; }

    public void openWarpsGui(Player player) {
        player.getScheduler().run(module.getPlugin(), t ->
                player.openInventory(WarpGuiBuilder.buildWarpsGui(module, player)), null);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title    = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        String guiTitle = ColorUtil.strip(module.getConfig().getString("gui.title", "ᴡᴀʀᴘs"));

        if (!title.equals(guiTitle)) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        SoundUtil.play(player, "click");

        int clickedSlot = event.getSlot();
        int randomSlot  = module.getConfig().getInt("gui.warp-info.random-warp.slot", 22);
        int guiSize     = module.getConfig().getInt("gui.size", 27);

        Map<String, Location> warps = module.getWarpManager().getAllWarps();
        List<String> names = new ArrayList<>(warps.keySet());

        if (clickedSlot == randomSlot) {
            Location random = module.getWarpManager().getRandomWarp();
            if (random == null) {
                SoundUtil.play(player, "error");
                MessageUtil.sendChat(player, "no_warps");
                MessageUtil.sendActionbar(player, "no_warps_ab");
                return;
            }
            player.closeInventory();
            String randomLabel = module.getConfig().getString("gui.warp-info.random-warp.displayName",
                    module.getConfig().getString("default-name", "warp"));
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

        Location loc = module.getWarpManager().getWarp(targetName);
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
