package acore.aurora.features.modules.player;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import acore.aurora.AcoreAurora;
import acore.aurora.core.Managers;
import acore.aurora.core.manager.client.ModuleManager;
import acore.aurora.events.impl.EventTick;
import acore.aurora.features.modules.Module;

public class TpsSync extends Module {
    public TpsSync() {
        super("TpsSync", Module.Category.PLAYER);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTick(EventTick e) {
        if (ModuleManager.timer.isEnabled()) return;
        if (Managers.SERVER.getTPS() > 1)
            AcoreAurora.TICK_TIMER = Managers.SERVER.getTPS() / 20f;
        else AcoreAurora.TICK_TIMER = 1f;
    }

    @Override
    public void onDisable() {
        AcoreAurora.TICK_TIMER = 1f;
    }
}
