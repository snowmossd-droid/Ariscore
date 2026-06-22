package acore.aurora.features.modules.combat;

import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;
import acore.aurora.features.modules.base.TrapModule;

public final class SelfTrap extends TrapModule {
    public SelfTrap() {
        super("SelfTrap", Category.COMBAT);
    }

    @Override
    protected boolean needNewTarget() {
        return target == null;
    }

    @Override
    protected @Nullable PlayerEntity getTarget() {
        return mc.player;
    }
}
