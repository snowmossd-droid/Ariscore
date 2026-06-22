package acore.aurora.utility.interfaces;

import net.minecraft.util.math.BlockPos;
import acore.aurora.features.modules.render.Trails;

import java.util.List;

public interface IEntity {
    List<Trails.Trail> getTrails();

    BlockPos thunderHack_Recode$getVelocityBP();
}
