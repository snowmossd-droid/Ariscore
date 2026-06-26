package me.vennlmao.ariscore.crates.views;

import me.vennlmao.ariscore.crates.CratesModule;
import me.vennlmao.ariscore.crates.models.CrateModel;
import me.vennlmao.ariscore.crates.models.RewardInfo;
import me.vennlmao.ariscore.crates.models.RewardsGuiConfig;
import me.vennlmao.ariscore.crates.utils.FoliaUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class CrateEditView implements InventoryHolder {

    private final CratesModule module;
    private final CrateModel crateModel;
    private final Inventory inventory;
    private final Map<Integer, RewardInfo> rewardSlots = new HashMap<>();
    private boolean dirty = false;

    private CrateEditView(CratesModule module, CrateModel crateModel) {
        this.module = module;
        this.crateModel = crateModel;

        RewardsGuiConfig cfg = crateModel.getRewardsGuiConfig();
        this.inventory = Bukkit.createInventory(this, cfg.getRows() * 9, "Edit: " + crateModel.getName());

        for (RewardInfo reward : crateModel.getRewards()) {
            inventory.setItem(reward.getSlot(), reward.getIcon().clone());
            rewardSlots.put(reward.getSlot(), reward);
        }
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public static void open(CratesModule module, Player player, CrateModel crateModel) {
        CrateEditView view = new CrateEditView(module, crateModel);
        FoliaUtil.runForEntity(module.getPlugin(), player, () -> player.openInventory(view.inventory));
    }

    public static class ClickListener implements Listener {

        private final CratesModule module;

        public ClickListener(CratesModule module) {
            this.module = module;
        }

        @EventHandler
        public void onClick(InventoryClickEvent event) {
            if (!(event.getInventory().getHolder() instanceof CrateEditView view)) return;

            int rawSlot = event.getRawSlot();
            boolean clickedTopInventory = rawSlot >= 0 && rawSlot < view.inventory.getSize();
            if (!clickedTopInventory) return;

            if (!(event.getWhoClicked() instanceof Player player)) return;

            event.setCancelled(false);
            module.getMessageUtil().playSound(player, "click");

            FoliaUtil.runForEntity(module.getPlugin(), player, () ->
                    view.markSlotDirty(player, rawSlot));
        }

        @EventHandler
        public void onClose(InventoryCloseEvent event) {
            if (!(event.getInventory().getHolder() instanceof CrateEditView view)) return;
            if (!view.dirty) return;

            module.reload();
        }
    }

    private void markSlotDirty(Player player, int slot) {
        dirty = true;

        ItemStack current = inventory.getItem(slot);
        RewardInfo existing = rewardSlots.get(slot);
        String rewardId = existing != null
                ? module.getCrateConfigManager().findRewardId(crateModel.getName(), slot)
                : "reward_" + slot;

        if (current == null || current.getType() == Material.AIR) {
            module.getCrateConfigManager().clearReward(crateModel.getName(), rewardId);
            rewardSlots.remove(slot);
            module.getMessageUtil().send(player, "edit-item-removed");
        } else {
            module.getCrateConfigManager().setRewardItem(crateModel.getName(), rewardId, slot, current.clone());
            module.getMessageUtil().send(player, "edit-item-added");
        }
    }
                }
