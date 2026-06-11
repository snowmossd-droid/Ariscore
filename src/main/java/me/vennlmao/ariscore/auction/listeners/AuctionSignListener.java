package me.vennlmao.ariscore.auction.listeners;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateSign;
import me.vennlmao.ariscore.auction.AuctionModule;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public class AuctionSignListener extends PacketListenerAbstract {

    private final AuctionModule module;

    public AuctionSignListener(AuctionModule module) {
        this.module = module;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.UPDATE_SIGN) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        Consumer<String[]> callback = module.getPendingSign().remove(player.getUniqueId());
        if (callback == null) return;

        WrapperPlayClientUpdateSign packet = new WrapperPlayClientUpdateSign(event);
        String[] lines = packet.getTextLines();
        event.setCancelled(true);

        player.getScheduler().run(module.getPlugin(), t -> callback.accept(lines), null);
    }
}
