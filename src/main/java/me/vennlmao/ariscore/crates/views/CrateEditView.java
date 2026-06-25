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
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player player)) return;
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= view.inventory.getSize()) return;

            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.getType() == Material.AIR) {
                module.getMessageUtil().send(player, "edit-no-item");
                module.getMessageUtil().playSound(player, "error");
                return;
            }

            RewardInfo existing = view.rewardSlots.get(slot);
            String rewardId = existing != null
                    ? module.getCrateConfigManager().findRewardId(view.crateModel.getName(), slot)
                    : "reward_" + slot;

            module.getCrateConfigManager().addItemToReward(view.crateModel.getName(), rewardId, hand.clone());
            module.getMessageUtil().send(player, "edit-item-added");
            module.getMessageUtil().playSound(player, "click");

            module.reload();
        }
    }
}
