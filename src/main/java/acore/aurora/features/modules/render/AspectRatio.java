package acore.aurora.features.modules.render;

import acore.aurora.features.modules.Module;
import acore.aurora.setting.Setting;

public class AspectRatio extends Module {
    public AspectRatio() {
        super("AspectRatio", Category.RENDER);
    }

    public Setting<Float> ratio = new Setting<>("Ratio", 1.78f, 0.1f, 5f);
}
