package acore.aurora.features.modules.player;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import acore.aurora.core.Managers;
import acore.aurora.features.modules.Module;
import acore.aurora.gui.notification.Notification;
import acore.aurora.setting.Setting;
import acore.aurora.utility.player.InventoryUtility;
import acore.aurora.utility.player.SearchInvResult;

import static acore.aurora.features.modules.render.ClientSettings.isRu;

public class ElytraReplace extends Module {
    public ElytraReplace() {
        super("ElytraReplace", Category.PLAYER);
    }

    private final Setting<Integer> durability = new Setting<>("Durability", 5, 0, 100);

    @Override
    public void onUpdate() {
        ItemStack is = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        if(is.isOf(Items.ELYTRA) && ((100f - ((float) is.getDamage() / (float) is.getMaxDamage()) * 100f) <= durability.getValue())){

            SearchInvResult result = InventoryUtility.findInInventory(stack -> {
                if (stack.getItem() instanceof ElytraItem)
                    return (100f - ((float) stack.getDamage() / (float) stack.getMaxDamage()) * 100f) > durability.getValue();
                return false;
            });

            if (result.found()) {
                clickSlot(result.slot());
                clickSlot(6);
                clickSlot(result.slot());
                sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
                Managers.NOTIFICATION.publicity("ElytraReplace", isRu() ? "Меняем элитру на новую!" : "Swapping the old elytra for a new one!",2, Notification.Type.SUCCESS);
                sendMessage(isRu() ? "Меняем элитру на новую!" : "Swapping the old elytra for a new one!");
            }
        }
    }
}
