package acore.aurora.features.modules.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import acore.aurora.events.impl.PacketEvent;
import acore.aurora.features.modules.Module;

public class XCarry extends Module {
    public XCarry() {
        super("XCarry", Category.PLAYER);
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send e) {
        if (e.getPacket() instanceof CloseHandledScreenC2SPacket) e.cancel();
    }
}
