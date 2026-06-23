package me.vennlmao.ariscore.tpa.listeners;

import me.vennlmao.ariscore.tpa.TpaModule;
import me.vennlmao.ariscore.tpa.managers.TpaRequest;
import me.vennlmao.ariscore.tpa.utils.GuiUtil;
import me.vennlmao.ariscore.tpa.utils.MessageUtil;
import me.vennlmao.ariscore.tpa.utils.SoundUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GuiListener implements Listener {

    private final TpaModule plugin;

    public GuiListener(TpaModule plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        String senderTitle = GuiUtil.stripColor(plugin.getConfig().getString("gui.sender.title", "ᴄᴏɴғɪʀᴍ ʀᴇǫᴜᴇsᴛ"));
        String acceptTitle = GuiUtil.stripColor(plugin.getConfig().getString("gui.accept.title", "ᴀᴄᴄᴇᴘᴛ ʀᴇǫᴜᴇsᴛ"));

        boolean isSenderGui = title.equals(senderTitle);
        boolean isAcceptGui = title.equals(acceptTitle);

        if (!isSenderGui && !isAcceptGui) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        SoundUtil.play(player, "gui_click");

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.displayName() == null) return;

        String itemName = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        String guiPath = isSenderGui ? "gui.sender" : "gui.accept";

        String confirmName = GuiUtil.stripColor(plugin.getConfig().getString(guiPath + ".icons.confirm.display-name", "ᴄᴏɴғɪʀᴍ"));
        String cancelName  = GuiUtil.stripColor(plugin.getConfig().getString(guiPath + ".icons.cancel.display-name", "ᴄᴀɴᴄᴇʟ"));

        if (itemName.equalsIgnoreCase(confirmName)) {
            player.closeInventory();

            if (isSenderGui) {
                // /tpa <player> → GUI sender → Confirm = gửi request TPA
                handleSendTpa(player);
            } else {
                // click accept_tpahere → GUI accept → Confirm = teleport
                handleAcceptTpahere(player);
            }

        } else if (itemName.equalsIgnoreCase(cancelName)) {
            player.closeInventory();

            if (isSenderGui) {
                // Hủy trước khi gửi
                plugin.getRequestManager().removeRequestBySender(player);
                MessageUtil.sendChatList(player, "outgoing_request_cancelled");
                MessageUtil.sendActionbar(player, "outgoing_request_cancelled_ab");
                SoundUtil.play(player, "cancel");
            } else {
                // Từ chối TPAHERE
                handleDenyTpahere(player);
            }
        }
    }

    private void handleSendTpa(Player sender) {
        TpaRequest pending = plugin.getRequestManager().getPendingSenderRequest(sender);
        if (pending == null) {
            SoundUtil.play(sender, "error");
            return;
        }

        Player target = pending.getReceiver();
        if (!target.isOnline()) {
            MessageUtil.sendChatList(sender, "target_offline");
            MessageUtil.sendActionbar(sender, "target_offline_ab");
            SoundUtil.play(sender, "error");
            plugin.getRequestManager().removePendingSenderRequest(sender);
            return;
        }

        plugin.getRequestManager().promotePendingToActive(sender);

        MessageUtil.sendChatList(sender, "request_sent_tpa",
                s -> s.replace("{player}", target.getName()));
        MessageUtil.sendActionbar(sender, "request_sent_tpa_ab",
                s -> s.replace("{player}", target.getName()));
        SoundUtil.play(sender, "request_sent");

        if (plugin.getRequestManager().isTpautoEnabled(target)) {
            plugin.getWarmupManager().startWarmup(sender, target, true);
            return;
        }

        MessageUtil.sendChatList(target, "request_received_tpa",
                s -> s.replace("{player}", sender.getName()));
        MessageUtil.sendActionbar(target, "request_received_tpa_ab",
                s -> s.replace("{player}", sender.getName()));
        SoundUtil.play(target, "request_sent");
    }

    private void handleAcceptTpahere(Player receiver) {
        TpaRequest request = plugin.getRequestManager().getRequest(receiver);
        if (request == null || request.getType() != TpaRequest.Type.TPAHERE) {
            SoundUtil.play(receiver, "error");
            return;
        }

        Player requester = request.getSender();
        if (!requester.isOnline()) {
            MessageUtil.sendChatList(receiver, "requester_offline");
            MessageUtil.sendActionbar(receiver, "requester_offline_ab");
            SoundUtil.play(receiver, "error");
            plugin.getRequestManager().removeRequest(receiver);
            return;
        }

        plugin.getRequestManager().removeRequest(receiver);

        SoundUtil.play(receiver, "request_accepted");
        SoundUtil.play(requester, "request_accepted");

        MessageUtil.sendActionbar(receiver, "request_accepted_tpahere_receiver_ab",
                s -> s.replace("{player}", requester.getName()));

        plugin.getWarmupManager().startWarmup(receiver, requester, true);
    }

    private void handleDenyTpahere(Player receiver) {
        TpaRequest request = plugin.getRequestManager().getRequest(receiver);
        if (request == null) return;

        Player sender = request.getSender();
        plugin.getRequestManager().removeRequest(receiver);

        MessageUtil.sendChatList(receiver, "request_denied_receiver");
        MessageUtil.sendActionbar(receiver, "request_denied_receiver_ab");
        SoundUtil.play(receiver, "cancel");

        if (sender.isOnline()) {
            MessageUtil.sendChatList(sender, "request_denied_sender",
                    s -> s.replace("{player}", receiver.getName()));
            MessageUtil.sendActionbar(sender, "request_denied_sender_ab",
                    s -> s.replace("{player}", receiver.getName()));
            SoundUtil.play(sender, "cancel");
        }
    }
}
