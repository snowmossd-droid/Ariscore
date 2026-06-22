package acore.aurora.features.modules.player;

import acore.aurora.features.modules.Module;
import acore.aurora.setting.Setting;

public class NoInteract extends Module {
    public NoInteract() {
        super("NoInteract", Category.PLAYER);
    }

    public static Setting<Boolean> onlyAura = new Setting<>("OnlyAura", false);
}
