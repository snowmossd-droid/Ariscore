package me.vennlmao.ariscore.spawn.listeners;

import me.vennlmao.ariscore.spawn.SpawnModule;
import me.vennlmao.ariscore.spawn.gui.SpawnGuiBuilder;
import me.vennlmao.ariscore.spawn.utils.ColorUtil;
import me.vennlmao.ariscore.spawn.utils.MessageUtil;
import me.vennlmao.ariscore.spawn.utils.SoundUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SpawnsGuiListener implements Listener {

    private final SpawnModule module;

    public SpawnsGuiListener(SpawnModule module) {
        this.module = module;
    }

    public void openSpawnsGui(Player player) {
        player.getScheduler().run(module.getPlugin(), t ->
                player.openInventory(SpawnGuiBuilder.buildSpawnsGui(module, player)), null);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title    = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        String guiTitle = ColorUtil.strip(module.getConfig().getString("gui.title", "ꜱᴘᴀᴡɴs"));

        if (!title.equals(guiTitle)) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        SoundUtil.play(player, "click");

        int clickedSlot = event.getSlot();
        int randomSlot  = module.getConfig().getInt("gui.spawn-info.random-spawn.slot", 22);
        int guiSize     = module.getConfig().getInt("gui.size", 27);

        Map<String, Location> spawns = module.getSpawnManager().getAllSpawns();
        List<String> names = new ArrayList<>(spawns.keySet());

        if (clickedSlot == randomSlot) {
            Location random = module.getSpawnManager().getRandomSpawn();
            if (random == null) {
                SoundUtil.play(player, "error");
                MessageUtil.sendChat(player, "no_spawns");
                MessageUtil.sendActionbar(player, "no_spawns_ab");
                return;
            }
            player.closeInventory();
            String randomLabel = module.getConfig().getString("gui.spawn-info.random-spawn.displayName",
                    module.getConfig().getString("default-name", "spawn"));
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

        Location loc = module.getSpawnManager().getSpawn(targetName);
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
