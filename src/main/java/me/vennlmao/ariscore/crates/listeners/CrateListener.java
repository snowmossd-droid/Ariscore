package me.vennlmao.ariscore.crates.listeners;

import me.vennlmao.ariscore.crates.CratesModule;
import me.vennlmao.ariscore.crates.models.CrateModel;
import me.vennlmao.ariscore.crates.utils.FoliaUtil;
import me.vennlmao.ariscore.crates.views.CrateRewardsView;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class CrateListener implements Listener {

    private final CratesModule module;

    public CrateListener(CratesModule module) {
        this.module = module;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        var action = event.getAction();
        if (action == org.bukkit.event.block.Action.LEFT_CLICK_AIR
                || action == org.bukkit.event.block.Action.RIGHT_CLICK_AIR) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Location blockLocation = block.getLocation();
        CrateModel crateModel = findCrateAt(blockLocation);
        if (crateModel == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (player.isSneaking()) return;

        CrateRewardsView.open(module, player, crateModel);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();

        CrateModel crateModel = findCrateAt(loc);
        if (crateModel == null) return;

        if (!player.hasPermission("ariscrates.admin")) {
            event.setCancelled(true);
            return;
        }

        crateModel.removeLocation(loc);
        FoliaUtil.runAsync(module.getPlugin(), () -> module.getCrateConfigManager().removeLocation(crateModel, loc));
    }

    private CrateModel findCrateAt(Location location) {
        for (CrateModel crate : module.getCrateRegistry().values()) {
            for (Location cLoc : crate.getLocations()) {
                if (cLoc.getBlockX() == location.getBlockX()
                        && cLoc.getBlockY() == location.getBlockY()
                        && cLoc.getBlockZ() == location.getBlockZ()
                        && cLoc.getWorld() != null
                        && cLoc.getWorld().equals(location.getWorld())) {
                    return crate;
                }
            }
        }
        return null;
    }
}
