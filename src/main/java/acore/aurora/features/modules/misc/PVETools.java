/*package acore.aurora.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import acore.aurora.events.impl.EventPostSync;
import acore.aurora.events.impl.EventSync;
import acore.aurora.features.modules.Module;
import acore.aurora.setting.Setting;

public class PVETools extends Module {
    public PVETools() {
        super("PVETools", Category.MISC);
    }

    // Crops
    private final Setting<Boolean> autoHoe = new Setting<>("AutoHoe", false);
    private final Setting<Boolean> cladHelper = new Setting<>("CladHelper", false);
    private final Setting<Boolean> autoLand = new Setting<>("AutoLand", false);
    private final Setting<Boolean> autoBoneMeal = new Setting<>("AutoBoneMeal", false);
    private final Setting<Boolean> Harvester = new Setting<>("Harvester", false);

    // Sheep
    private final Setting<Boolean> SheepPaint = new Setting<>("SheepPaint", false);
    private final Setting<Boolean> SheepShear = new Setting<>("SheepShear", false);


    @EventHandler
    public void rotateAction(EventSync e) {
    }

    @EventHandler
    public void postRotateAction(EventPostSync e) {
    }

    @EventHandler
    public void onSync(EventSync e) {
    }

    // ПИЗДЕЦ НЕ ТРОГАЙТЕ МОДУЛЬ!
}*/