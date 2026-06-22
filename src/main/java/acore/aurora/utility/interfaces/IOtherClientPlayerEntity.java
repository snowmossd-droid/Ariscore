package acore.aurora.utility.interfaces;

import acore.aurora.features.modules.combat.Aura;

public interface IOtherClientPlayerEntity {
    void resolve(Aura.Resolver mode);

    void releaseResolver();
}
