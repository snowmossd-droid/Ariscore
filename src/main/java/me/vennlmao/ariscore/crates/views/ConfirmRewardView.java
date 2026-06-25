package me.vennlmao.ariscore.crates.views;

import me.vennlmao.ariscore.crates.CratesModule;
import me.vennlmao.ariscore.crates.models.ConfirmGuiConfig;
import me.vennlmao.ariscore.crates.models.CrateModel;
import me.vennlmao.ariscore.crates.models.GamerModel;
import me.vennlmao.ariscore.crates.models.GuiButton;
import me.vennlmao.ariscore.crates.models.RewardInfo;
import me.vennlmao.ariscore.crates.utils.FoliaUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ConfirmRewardView implements InventoryHolder {

    private final CratesModule module;
    private final CrateModel crateModel;
    private final RewardInfo rewardInfo;
    private final Inventory inventory;
    private final int cancelSlot;
    private final int confirmSlot;

    private ConfirmRewardView(CratesModule module, CrateModel crateModel, RewardInfo rewardInfo) {
        this.module = module;
        this.crateModel = crateModel;
        this.rewardInfo = rewardInfo;

        ConfirmGuiConfig cfg = crateModel.getConfirmGuiConfig();
        this.inventory = Bukkit.createInventory(this, cfg.getRows() * 9, cfg.getName());

        for (int i = 0; i < inventory.getSize(); i++) {
            if (cfg.getBackground() != null) inventory.setItem(i, cfg.getBackground().clone());
        }

        inventory.setItem(cfg.getRewardSlot(), rewardInfo.getIcon().clone());

        GuiButton cancel = cfg.getCancelButton();
        if (cancel != null) {
            inventory.setItem(cancel.getSlot(), cancel.getItem().clone());
            this.cancelSlot = cancel.getSlot();
        } else {
            this.cancelSlot = -1;
        }

        GuiButton confirm = cfg.getConfirmButton();
        if (confirm != null) {
            inventory.setItem(confirm.getSlot(), confirm.getItem().clone());
            this.confirmSlot = confirm.getSlot();
        } else {
            this.confirmSlot = -1;
        }
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public static void open(CratesModule module, Player player, CrateModel crateModel, RewardInfo rewardInfo) {
        ConfirmRewardView view = new ConfirmRewardView(module, crateModel, rewardInfo);
        FoliaUtil.runForEntity(module.getPlugin(), player, () -> player.openInventory(view.inventory));
    }

    public static class ClickListener implements Listener {

        private final CratesModule module;

        public ClickListener(CratesModule module) {
            this.module = module;
        }

        @EventHandler
        public void onClick(InventoryClickEvent event) {
            if (!(event.getInventory().getHolder() instanceof ConfirmRewardView view)) return;
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player player)) return;
            int slot = event.getRawSlot();

            if (slot == view.cancelSlot) {
                module.getMessageUtil().playSound(player, "click");
                player.closeInventory();
                return;
            }

            if (slot == view.confirmSlot) {
                module.getMessageUtil().playSound(player, "click");
                player.closeInventory();
                claimReward(player, view.crateModel, view.rewardInfo);
            }
        }

        private void claimReward(Player player, CrateModel crateModel, RewardInfo rewardInfo) {
            GamerModel gamer = module.getGamerDataManager().find(player.getUniqueId());
            if (gamer == null || gamer.getKeyAmount(crateModel.getName()) <= 0) return;

            gamer.removeKeyAmount(crateModel.getName(), 1);

            FoliaUtil.runForEntity(module.getPlugin(), player, () -> {
                giveItems(player, rewardInfo);
                module.getMessageUtil().playSound(player, "purchase_success");
            });

            FoliaUtil.runAsync(module.getPlugin(), () ->
                    module.getPlayerStorageManager().savePlayer(gamer));
        }

        private void giveItems(Player player, RewardInfo rewardInfo) {
            if (rewardInfo.getItems().isEmpty()) return;

            for (ItemStack item : rewardInfo.getItems()) {
                if (item == null) continue;
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
                if (!leftover.isEmpty()) {
                    Location loc = player.getLocation();
                    leftover.values().forEach(drop -> loc.getWorld().dropItemNaturally(loc, drop));
                }
            }
        }
    }
}
