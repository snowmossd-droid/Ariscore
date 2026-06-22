package acore.aurora.features.modules.render;

import acore.aurora.features.modules.Module;
import acore.aurora.setting.Setting;

public final class Notifications extends Module {
    public Notifications() {
        super("Notifications", Category.RENDER);
    }

    public final Setting<Mode> mode = new Setting<>("Mode", Mode.CrossHair);

    public enum Mode {
        Default, CrossHair, Text
    }
}
