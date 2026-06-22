package acore.aurora.features.modules.combat;

import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;
import acore.aurora.core.Managers;
import acore.aurora.core.manager.player.CombatManager;
import acore.aurora.features.modules.base.TrapModule;
import acore.aurora.setting.Setting;

public final class AutoTrap extends TrapModule {
    private final Setting<CombatManager.TargetBy> targetBy = new Setting<>("Target By", CombatManager.TargetBy.Distance);
    private final Setting<Boolean> targetMovingPlayers = new Setting<>("MovingPlayers", false);

    public AutoTrap() {
        super("AutoTrap", Category.COMBAT);
    }

    @Override
    protected boolean needNewTarget() {
        return target == null
                || target.distanceTo(mc.player) > range.getValue()
                || target.getHealth() + target.getAbsorptionAmount() <= 0
                || target.isDead();
    }

    @Override
    protected @Nullable PlayerEntity getTarget() {
        return Managers.COMBAT.getTarget(range.getValue(), targetBy.getValue(), p -> p.getVelocity().lengthSquared() < 0.08 || targetMovingPlayers.getValue());
    }
}