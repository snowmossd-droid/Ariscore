package me.vennlmao.ariscore.tpa.listeners;

import me.vennlmao.ariscore.tpa.TpaModule;
import me.vennlmao.ariscore.tpa.managers.TpaRequest;
import me.vennlmao.ariscore.tpa.utils.GuiUtil;
import me.vennlmao.ariscore.tpa.utils.MessageUtil;
import me.vennlmao.ariscore.tpa.utils.SoundUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class GuiListener implements Listener {

    private final TpaModule plugin;
    private static final MiniMessage MM = MiniMessage.miniMessage();

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
                TpaRequest outgoing = plugin.getRequestManager().getRequestBySender(player);
                if (outgoing != null) handleSend(player, outgoing);

            } else {
                TpaRequest request = plugin.getRequestManager().getRequest(player);
                if (request != null) handleAccept(player, request);
            }

        } else if (itemName.equalsIgnoreCase(cancelName)) {
            player.closeInventory();

            if (isSenderGui) {
                TpaRequest outgoing = plugin.getRequestManager().getRequestBySender(player);
                if (outgoing != null) {
                    plugin.getRequestManager().removeRequestBySender(player);
                    MessageUtil.sendChatList(player, "outgoing_request_cancelled");
                    MessageUtil.sendActionbar(player, "outgoing_request_cancelled_ab");
                    SoundUtil.play(player, "cancel");

                    Player receiver = outgoing.getReceiver();
                    if (receiver.isOnline()) {
                        MessageUtil.sendChatList(receiver, "incoming_request_cancelled",
                                s -> s.replace("{player}", player.getName()));
                        MessageUtil.sendActionbar(receiver, "incoming_request_cancelled_ab",
                                s -> s.replace("{player}", player.getName()));
                        SoundUtil.play(receiver, "cancel");
                    }
                }
            } else {
                TpaRequest request = plugin.getRequestManager().getRequest(player);
                if (request != null) handleDeny(player, request);
            }
        }
    }

    private void handleSend(Player sender, TpaRequest request) {
        Player target = request.getReceiver();
        if (!target.isOnline()) {
            MessageUtil.sendChatList(sender, "target_offline");
            MessageUtil.sendActionbar(sender, "target_offline_ab");
            SoundUtil.play(sender, "error");
            plugin.getRequestManager().removeRequestBySender(sender);
            return;
        }

        MessageUtil.sendChatList(sender, "request_sent_tpa",
                s -> s.replace("{player}", target.getName()));
        MessageUtil.sendActionbar(sender, "request_sent_tpa_ab",
                s -> s.replace("{player}", target.getName()));
        SoundUtil.play(sender, "request_sent");

        if (plugin.getRequestManager().isTpautoEnabled(target)) {
            plugin.getWarmupManager().startWarmup(sender, target, true);
            return;
        }

        sendClickableRequest(target, sender, "accept_tpa");
        MessageUtil.sendChatList(target, "request_received_tpa",
                s -> s.replace("{player}", sender.getName()));
        SoundUtil.play(target, "request_sent");
    }

    private void handleAccept(Player receiver, TpaRequest request) {
        Player sender = request.getSender();
        if (!sender.isOnline()) {
            MessageUtil.sendChatList(receiver, "requester_offline");
            MessageUtil.sendActionbar(receiver, "requester_offline_ab");
            SoundUtil.play(receiver, "error");
            plugin.getRequestManager().removeRequest(receiver);
            return;
        }

        plugin.getRequestManager().removeRequest(receiver);
        SoundUtil.play(receiver, "request_accepted");
        SoundUtil.play(sender, "request_accepted");

        if (request.getType() == TpaRequest.Type.TPA) {
            MessageUtil.sendActionbar(receiver, "request_accepted_tpa_receiver_ab",
                    s -> s.replace("{player}", sender.getName()));
            plugin.getWarmupManager().startWarmup(sender, receiver, true);
        } else {
            MessageUtil.sendActionbar(receiver, "request_accepted_tpahere_receiver_ab",
                    s -> s.replace("{player}", sender.getName()));
            plugin.getWarmupManager().startWarmup(receiver, sender, true);
        }
    }

    private void handleDeny(Player receiver, TpaRequest request) {
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

    private void sendClickableRequest(Player receiver, Player requester, String key) {
        List<String> lines = plugin.getConfig().getStringList("clickable_messages." + key + ".text");
        for (String line : lines) {
            String replaced = line.replace("{player}", requester.getName());
            String colored = replaced
                    .replaceAll("&#([0-9A-Fa-f]{6})", "<color:#$1>")
                    .replaceAll("#([0-9A-Fa-f]{6})", "<color:#$1>")
                    .replace("&7", "<gray>").replace("&a", "<green>").replace("&f", "<white>");
            Component component = MM.deserialize("<!italic>" + colored)
                    .clickEvent(ClickEvent.runCommand("/tpaccept " + requester.getName()));
            receiver.sendMessage(component);
        }
    }
}
