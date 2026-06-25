package me.vennlmao.ariscore.crates.views;

import me.vennlmao.ariscore.crates.CratesModule;
import me.vennlmao.ariscore.crates.models.CrateModel;
import me.vennlmao.ariscore.crates.models.GamerModel;
import me.vennlmao.ariscore.crates.models.GuiButton;
import me.vennlmao.ariscore.crates.models.RewardInfo;
import me.vennlmao.ariscore.crates.models.RewardsGuiConfig;
import me.vennlmao.ariscore.crates.utils.FoliaUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

public class CrateRewardsView implements InventoryHolder {

    private final CratesModule module;
    private final CrateModel crateModel;
    private final Inventory inventory;
    private final Map<Integer, RewardInfo> rewardSlots = new HashMap<>();
    private int cancelSlot = -1;

    private CrateRewardsView(CratesModule module, CrateModel crateModel) {
        this.module = module;
        this.crateModel = crateModel;

        RewardsGuiConfig cfg = crateModel.getRewardsGuiConfig();
        this.inventory = Bukkit.createInventory(this, cfg.getRows() * 9, cfg.getName());

        for (int i = 0; i < inventory.getSize(); i++) {
            if (cfg.getBackground() != null) inventory.setItem(i, cfg.getBackground().clone());
        }

        for (RewardInfo reward : crateModel.getRewards()) {
            inventory.setItem(reward.getSlot(), reward.getIcon().clone());
            rewardSlots.put(reward.getSlot(), reward);
        }

        GuiButton cancel = cfg.getCancelButton();
        if (cancel != null) {
            inventory.setItem(cancel.getSlot(), cancel.getItem().clone());
            cancelSlot = cancel.getSlot();
        }
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public static void open(CratesModule module, Player player, CrateModel crateModel) {
        GamerModel gamer = module.getGamerDataManager().find(player.getUniqueId());
        if (gamer == null || gamer.getKeyAmount(crateModel.getName()) <= 0) {
            module.getMessageUtil().send(player, "no-keys");
            return;
        }
        CrateRewardsView view = new CrateRewardsView(module, crateModel);
        FoliaUtil.runForEntity(module.getPlugin(), player, () -> player.openInventory(view.inventory));
    }

    public static class ClickListener implements Listener {

        private final CratesModule module;

        public ClickListener(CratesModule module) {
            this.module = module;
        }

        @EventHandler
        public void onClick(InventoryClickEvent event) {
            if (!(event.getInventory().getHolder() instanceof CrateRewardsView view)) return;
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player player)) return;
            int slot = event.getRawSlot();

            if (slot == view.cancelSlot) {
                player.closeInventory();
                return;
            }

            RewardInfo reward = view.rewardSlots.get(slot);
            if (reward == null) return;

            GamerModel gamer = module.getGamerDataManager().find(player.getUniqueId());
            if (gamer == null || gamer.getKeyAmount(view.crateModel.getName()) <= 0) {
                player.closeInventory();
                return;
            }

            player.closeInventory();
            ConfirmRewardView.open(module, player, view.crateModel, reward);
        }
    }
}
